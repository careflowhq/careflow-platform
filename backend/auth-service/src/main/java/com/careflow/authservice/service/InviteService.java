package com.careflow.authservice.service;

import com.careflow.authservice.dto.InviteRequest;
import com.careflow.authservice.dto.InviteResponse;
import com.careflow.authservice.dto.RegisterInviteRequest;
import com.careflow.authservice.entity.Invitation;
import com.careflow.authservice.entity.InvitationStatus;
import com.careflow.authservice.entity.User;
import com.careflow.authservice.exception.DuplicateEmailException;
import com.careflow.authservice.exception.InviteAlreadyPendingException;
import com.careflow.authservice.exception.InviteExpiredException;
import com.careflow.authservice.exception.InviteNotFoundException;
import com.careflow.authservice.repository.InvitationRepository;
import com.careflow.authservice.repository.UserRepository;
import com.careflow.authservice.security.InviteAccessGuard;
import com.careflow.authservice.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InviteService {

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final InviteAccessGuard inviteAccessGuard;

    @Value("${careflow.invite.expiration-days:7}")
    private int expirationDays;

    @Transactional
    public InviteResponse createInvite(InviteRequest request) {
        inviteAccessGuard.assertCanInvite();
        inviteAccessGuard.assertInvitableRole(request.role());

        UUID clinicId = TenantContext.clinicId();

        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(request.email());
        }

        if (invitationRepository.existsByEmailAndClinicIdAndStatus(
                request.email(), clinicId, InvitationStatus.PENDING)) {
            throw new InviteAlreadyPendingException(request.email());
        }

        Invitation invitation = Invitation.builder()
                .token(UUID.randomUUID().toString())
                .clinicId(clinicId)
                .email(request.email())
                .fullName(request.fullName())
                .role(request.role())
                .invitedBy(TenantContext.userId())
                .status(InvitationStatus.PENDING)
                .expiresAt(Instant.now().plus(expirationDays, ChronoUnit.DAYS))
                .build();

        return InviteResponse.from(invitationRepository.save(invitation));
    }

    @Transactional
    public void acceptInvite(RegisterInviteRequest request) {
        Invitation invitation = invitationRepository
                .findByTokenAndStatus(request.token(), InvitationStatus.PENDING)
                .orElseThrow(InviteNotFoundException::new);

        if (invitation.getExpiresAt().isBefore(Instant.now())) {
            invitation.setStatus(InvitationStatus.EXPIRED);
            invitationRepository.save(invitation);
            throw new InviteExpiredException();
        }

        if (userRepository.existsByEmail(invitation.getEmail())) {
            throw new DuplicateEmailException(invitation.getEmail());
        }

        User user = User.builder()
                .clinicId(invitation.getClinicId())
                .fullName(invitation.getFullName())
                .email(invitation.getEmail())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(invitation.getRole())
                .build();

        userRepository.save(user);

        invitation.setStatus(InvitationStatus.ACCEPTED);
        invitationRepository.save(invitation);
    }
}
