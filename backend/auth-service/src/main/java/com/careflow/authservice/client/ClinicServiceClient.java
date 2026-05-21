package com.careflow.authservice.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.UUID;

@Component
public class ClinicServiceClient {

    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private final RestClient restClient;
    private final String internalApiKey;

    public ClinicServiceClient(
            RestClient.Builder restClientBuilder,
            @Value("${careflow.clinic-service.url}") String clinicServiceUrl,
            @Value("${careflow.internal.api-key}") String internalApiKey) {
        this.restClient = restClientBuilder.baseUrl(clinicServiceUrl).build();
        this.internalApiKey = internalApiKey;
    }

    public UUID onboardClinic(String name, String country, String timezone) {
        try {
            ClinicOnboardResponse response = restClient.post()
                    .uri("/internal/clinics")
                    .header(INTERNAL_API_KEY_HEADER, internalApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new ClinicOnboardRequest(name, country, timezone))
                    .retrieve()
                    .body(ClinicOnboardResponse.class);

            if (response == null || response.id() == null) {
                throw new ClinicServiceException("Clinic service returned an empty response");
            }

            return response.id();
        } catch (RestClientException ex) {
            throw new ClinicServiceException("Failed to create clinic during registration", ex);
        }
    }
}
