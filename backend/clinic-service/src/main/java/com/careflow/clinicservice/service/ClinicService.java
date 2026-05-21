package com.careflow.clinicservice.service;

import com.careflow.clinicservice.dto.ClinicResponse;
import com.careflow.clinicservice.dto.CreateClinicRequest;
import com.careflow.clinicservice.dto.OnboardClinicRequest;
import com.careflow.clinicservice.dto.UpdateClinicRequest;
import com.careflow.clinicservice.entity.Clinic;
import com.careflow.clinicservice.entity.SubscriptionPlan;
import com.careflow.clinicservice.exception.ClinicNotFoundException;
import com.careflow.clinicservice.repository.ClinicRepository;
import com.careflow.clinicservice.security.ClinicAccessGuard;
import com.careflow.clinicservice.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClinicService {

    private final ClinicRepository clinicRepository;

    @Transactional
    public ClinicResponse create(CreateClinicRequest request) {
        ClinicAccessGuard.requirePlatformAdmin();

        Clinic clinic = Clinic.builder()
                .name(request.name())
                .country(request.country())
                .timezone(request.timezone())
                .subscriptionPlan(request.subscriptionPlan())
                .active(request.active() != null ? request.active() : true)
                .build();

        return ClinicResponse.from(clinicRepository.save(clinic));
    }

    /**
     * Creates a clinic during auth onboarding.
     * Called by auth-service over the internal API — no JWT tenant context required.
     */
    @Transactional
    public ClinicResponse onboard(OnboardClinicRequest request) {
        Clinic clinic = Clinic.builder()
                .name(request.name())
                .country(request.country())
                .timezone(request.timezone())
                .subscriptionPlan(SubscriptionPlan.FREE)
                .active(true)
                .build();

        return ClinicResponse.from(clinicRepository.save(clinic));
    }

    @Transactional(readOnly = true)
    public List<ClinicResponse> findAllAccessible() {
        if (TenantContext.isPlatformAdmin()) {
            return clinicRepository.findByActiveTrueOrderByNameAsc().stream()
                    .map(ClinicResponse::from)
                    .toList();
        }

        return clinicRepository.findByIdAndActiveTrue(TenantContext.clinicId())
                .map(clinic -> List.of(ClinicResponse.from(clinic)))
                .orElse(List.of());
    }

    @Transactional(readOnly = true)
    public ClinicResponse findByIdAccessible(UUID id) {
        ClinicAccessGuard.requireOwnClinicOrPlatformAdmin(id);

        return clinicRepository.findByIdAndActiveTrue(id)
                .map(ClinicResponse::from)
                .orElseThrow(() -> new ClinicNotFoundException(id));
    }

    @Transactional
    public ClinicResponse update(UUID id, UpdateClinicRequest request) {
        ClinicAccessGuard.requireClinicAdminOrPlatformAdmin(id);

        Clinic clinic = clinicRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ClinicNotFoundException(id));

        clinic.setName(request.name());
        clinic.setCountry(request.country());
        clinic.setTimezone(request.timezone());
        clinic.setSubscriptionPlan(request.subscriptionPlan());
        clinic.setActive(request.active());

        return ClinicResponse.from(clinicRepository.save(clinic));
    }

    @Transactional
    public void delete(UUID id) {
        ClinicAccessGuard.requirePlatformAdmin();

        Clinic clinic = clinicRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ClinicNotFoundException(id));

        clinic.setActive(false);
        clinicRepository.save(clinic);
    }
}
