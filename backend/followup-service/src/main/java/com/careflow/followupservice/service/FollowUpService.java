package com.careflow.followupservice.service;

import com.careflow.followupservice.dto.CompleteFollowUpRequest;
import com.careflow.followupservice.dto.CreateFollowUpRequest;
import com.careflow.followupservice.dto.FollowUpResponse;
import com.careflow.followupservice.dto.UpdateFollowUpRequest;
import com.careflow.followupservice.entity.FollowUp;
import com.careflow.followupservice.entity.FollowUpStatus;
import com.careflow.followupservice.exception.FollowUpInvalidStateException;
import com.careflow.followupservice.exception.FollowUpNotFoundException;
import com.careflow.followupservice.client.PatientServiceClient;
import com.careflow.followupservice.client.PatientSummary;
import com.careflow.followupservice.messaging.FollowUpEventPublisher;
import com.careflow.followupservice.repository.FollowUpRepository;
import com.careflow.followupservice.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FollowUpService {

    private final FollowUpRepository followUpRepository;
    private final PatientServiceClient patientServiceClient;
    private final FollowUpEventPublisher eventPublisher;

    @Transactional
    public FollowUpResponse create(CreateFollowUpRequest request) {
        FollowUp followUp = FollowUp.builder()
                .clinicId(TenantContext.clinicId())
                .patientId(request.patientId())
                .doctorId(request.doctorId())
                .type(request.type())
                .scheduledDate(request.scheduledDate())
                .notes(request.notes())
                .createdBy(TenantContext.userId())
                .status(FollowUpStatus.PENDING)
                .build();

        FollowUp saved = followUpRepository.save(followUp);
        PatientSummary patient = patientServiceClient.getPatient(
                saved.getPatientId(),
                saved.getClinicId(),
                TenantContext.userId(),
                TenantContext.role()
        );
        eventPublisher.publishScheduled(saved, patient);
        return FollowUpResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<FollowUpResponse> findAllForCurrentClinic() {
        return followUpRepository.findByClinicIdOrderByScheduledDateDesc(TenantContext.clinicId()).stream()
                .map(FollowUpResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FollowUpResponse> findPendingForCurrentClinic() {
        return followUpRepository
                .findByClinicIdAndStatusOrderByScheduledDateAsc(TenantContext.clinicId(), FollowUpStatus.PENDING)
                .stream()
                .map(FollowUpResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public FollowUpResponse findByIdForCurrentClinic(UUID id) {
        return FollowUpResponse.from(getFollowUpForCurrentClinic(id));
    }

    @Transactional
    public FollowUpResponse update(UUID id, UpdateFollowUpRequest request) {
        FollowUp followUp = getFollowUpForCurrentClinic(id);
        assertPending(followUp, "Only PENDING follow-ups can be updated");

        followUp.setDoctorId(request.doctorId());
        followUp.setType(request.type());
        followUp.setScheduledDate(request.scheduledDate());
        followUp.setNotes(request.notes());

        return FollowUpResponse.from(followUpRepository.save(followUp));
    }

    @Transactional
    public FollowUpResponse complete(UUID id, CompleteFollowUpRequest request) {
        FollowUp followUp = getFollowUpForCurrentClinic(id);
        assertPending(followUp, "Only PENDING follow-ups can be completed");

        followUp.setStatus(FollowUpStatus.COMPLETED);
        if (request != null && request.notes() != null && !request.notes().isBlank()) {
            followUp.setNotes(request.notes());
        }

        return FollowUpResponse.from(followUpRepository.save(followUp));
    }

    @Transactional
    public void cancel(UUID id) {
        FollowUp followUp = getFollowUpForCurrentClinic(id);
        assertPending(followUp, "Only PENDING follow-ups can be cancelled");

        followUp.setStatus(FollowUpStatus.CANCELLED);
        followUpRepository.save(followUp);
    }

    private FollowUp getFollowUpForCurrentClinic(UUID id) {
        return followUpRepository.findByIdAndClinicId(id, TenantContext.clinicId())
                .orElseThrow(() -> new FollowUpNotFoundException(id));
    }

    private void assertPending(FollowUp followUp, String message) {
        if (followUp.getStatus() != FollowUpStatus.PENDING) {
            throw new FollowUpInvalidStateException(message);
        }
    }
}
