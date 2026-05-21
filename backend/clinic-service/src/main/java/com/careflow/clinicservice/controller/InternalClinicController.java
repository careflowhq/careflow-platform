package com.careflow.clinicservice.controller;

import com.careflow.clinicservice.dto.ClinicResponse;
import com.careflow.clinicservice.dto.OnboardClinicRequest;
import com.careflow.clinicservice.service.ClinicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * Internal endpoints for trusted microservices (auth-service onboarding).
 * Not exposed through the public gateway flow for end users.
 */
@RestController
@RequestMapping("/internal/clinics")
@RequiredArgsConstructor
public class InternalClinicController {

    private final ClinicService clinicService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClinicResponse onboard(@Valid @RequestBody OnboardClinicRequest request) {
        return clinicService.onboard(request);
    }
}
