package com.careflow.authservice.service;

import com.careflow.authservice.client.ClinicServiceClient;
import com.careflow.authservice.dto.LoginRequest;
import com.careflow.authservice.dto.LoginResponse;
import com.careflow.authservice.dto.RegisterRequest;
import com.careflow.authservice.entity.User;
import com.careflow.authservice.entity.UserRole;
import com.careflow.authservice.exception.DuplicateEmailException;
import com.careflow.authservice.exception.InvalidCredentialsException;
import com.careflow.authservice.exception.InvalidRegistrationException;
import com.careflow.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ClinicServiceClient clinicServiceClient;

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        UUID clinicId = resolveClinicId(request);

        User user = User.builder()
                .clinicId(clinicId)
                .fullName(request.fullName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        userRepository.save(user);
    }

    private UUID resolveClinicId(RegisterRequest request) {
        if (request.role() != UserRole.CLINIC_ADMIN) {
            throw new InvalidRegistrationException(
                    "Only CLINIC_ADMIN self-registration is supported. Other roles must be invited.");
        }

        validateClinicFields(request);

        return clinicServiceClient.onboardClinic(
                request.clinicName(),
                request.country(),
                request.timezone()
        );
    }

    private void validateClinicFields(RegisterRequest request) {
        if (isBlank(request.clinicName()) || isBlank(request.country()) || isBlank(request.timezone())) {
            throw new InvalidRegistrationException(
                    "clinicName, country and timezone are required for CLINIC_ADMIN registration");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(
                user.getId(),
                user.getClinicId(),
                user.getRole().name()
        );

        return new LoginResponse(token);
    }
}
