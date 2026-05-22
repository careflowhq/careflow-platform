package com.careflow.followupservice.client;

import com.careflow.followupservice.tenant.TenantHeaders;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Slf4j
@Component
public class PatientServiceClient {

    private final RestClient restClient;

    public PatientServiceClient(@Value("${careflow.patient-service.base-url}") String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public PatientSummary getPatient(UUID patientId, UUID clinicId, UUID userId, String role) {
        try {
            return restClient.get()
                    .uri("/patients/{id}", patientId)
                    .header(TenantHeaders.CLINIC_ID, clinicId.toString())
                    .header(TenantHeaders.USER_ID, userId.toString())
                    .header(TenantHeaders.ROLE, role)
                    .retrieve()
                    .body(PatientSummary.class);
        } catch (Exception ex) {
            log.warn("Failed to fetch patient {} for clinic {}: {}", patientId, clinicId, ex.getMessage());
            return null;
        }
    }
}
