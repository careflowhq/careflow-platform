package com.careflow.clinicservice.controller;

import com.careflow.clinicservice.dto.ClinicResponse;
import com.careflow.clinicservice.dto.CreateClinicRequest;
import com.careflow.clinicservice.dto.UpdateClinicRequest;
import com.careflow.clinicservice.service.ClinicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clinics")
@RequiredArgsConstructor
public class ClinicController {

    private final ClinicService clinicService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClinicResponse create(@Valid @RequestBody CreateClinicRequest request) {
        return clinicService.create(request);
    }

    @GetMapping
    public List<ClinicResponse> findAll() {
        return clinicService.findAllAccessible();
    }

    @GetMapping("/{id}")
    public ClinicResponse findById(@PathVariable UUID id) {
        return clinicService.findByIdAccessible(id);
    }

    @PutMapping("/{id}")
    public ClinicResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateClinicRequest request) {
        return clinicService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        clinicService.delete(id);
    }
}
