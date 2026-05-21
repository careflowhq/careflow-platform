package com.careflow.followupservice.controller;

import com.careflow.followupservice.dto.CompleteFollowUpRequest;
import com.careflow.followupservice.dto.CreateFollowUpRequest;
import com.careflow.followupservice.dto.FollowUpResponse;
import com.careflow.followupservice.dto.UpdateFollowUpRequest;
import com.careflow.followupservice.service.FollowUpService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/followups")
@RequiredArgsConstructor
public class FollowUpController {

    private final FollowUpService followUpService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FollowUpResponse create(@Valid @RequestBody CreateFollowUpRequest request) {
        return followUpService.create(request);
    }

    @GetMapping
    public List<FollowUpResponse> findAll() {
        return followUpService.findAllForCurrentClinic();
    }

    @GetMapping("/pending")
    public List<FollowUpResponse> findPending() {
        return followUpService.findPendingForCurrentClinic();
    }

    @GetMapping("/{id}")
    public FollowUpResponse findById(@PathVariable UUID id) {
        return followUpService.findByIdForCurrentClinic(id);
    }

    @PutMapping("/{id}")
    public FollowUpResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateFollowUpRequest request) {
        return followUpService.update(id, request);
    }

    @PatchMapping("/{id}/complete")
    public FollowUpResponse complete(
            @PathVariable UUID id,
            @RequestBody(required = false) CompleteFollowUpRequest request) {
        return followUpService.complete(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@PathVariable UUID id) {
        followUpService.cancel(id);
    }
}
