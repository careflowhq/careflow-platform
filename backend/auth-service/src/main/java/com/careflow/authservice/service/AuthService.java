package com.careflow.authservice.service;

import com.careflow.authservice.dto.LoginRequest;
import com.careflow.authservice.dto.LoginResponse;
import com.careflow.authservice.dto.RegisterRequest;
import com.careflow.authservice.entity.User;
import com.careflow.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public void register(RegisterRequest request) {

        User user = User.builder()
                .clinicId(UUID.randomUUID())
                .fullName(request.fullName())
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.email())
                .orElseThrow();

        boolean validPassword = passwordEncoder.matches(
                request.password(),
                user.getPasswordHash()
        );

        if (!validPassword) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtService.generateToken(
                user.getId(),
                user.getClinicId(),
                user.getRole().name()
        );

        return new LoginResponse(token);
    }
}