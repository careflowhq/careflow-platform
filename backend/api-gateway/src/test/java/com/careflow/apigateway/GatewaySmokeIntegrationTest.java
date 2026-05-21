package com.careflow.apigateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class GatewaySmokeIntegrationTest {

    private static final String JWT_SECRET = "careflow-secret-key-careflow-secret-key";
    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CLINIC_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    private static WireMockServer wireMock;

    @Autowired
    private WebTestClient webTestClient;

    @BeforeAll
    static void startWireMock() {
        wireMock = new WireMockServer(wireMockConfig().dynamicPort());
        wireMock.start();
        configureFor("localhost", wireMock.port());
    }

    @AfterAll
    static void stopWireMock() {
        if (wireMock != null) {
            wireMock.stop();
        }
    }

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("careflow.test.wiremock-uri", () -> "http://localhost:" + wireMock.port());
        registry.add("careflow.jwt.secret", () -> JWT_SECRET);
    }

    @Test
    void publicAuthRouteDoesNotRequireJwt() {
        stubFor(post(urlEqualTo("/auth/login"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"token\":\"downstream-token\"}")));

        webTestClient.post()
                .uri("/api/auth/login")
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .bodyValue("{\"email\":\"user@test.com\",\"password\":\"secret123\"}")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isEqualTo("downstream-token");
    }

    @Test
    void protectedRouteReturns401WithoutJwt() {
        webTestClient.get()
                .uri("/api/patients")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void inviteRouteRequiresJwt() {
        webTestClient.post()
                .uri("/api/auth/invite")
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .bodyValue("{\"fullName\":\"Dr. Ana\",\"email\":\"ana@test.com\",\"role\":\"DOCTOR\"}")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void protectedRoutesForwardWithTenantHeaders() {
        stubFor(get(urlEqualTo("/patients"))
                .willReturn(aResponse().withStatus(200).withBody("[]")));
        stubFor(get(urlEqualTo("/clinics"))
                .willReturn(aResponse().withStatus(200).withBody("[]")));
        stubFor(get(urlEqualTo("/followups/pending"))
                .willReturn(aResponse().withStatus(200).withBody("[]")));

        String token = buildToken();

        webTestClient.get()
                .uri("/api/patients")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk();

        webTestClient.get()
                .uri("/api/clinics")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk();

        webTestClient.get()
                .uri("/api/followups/pending")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isOk();

        verify(getRequestedFor(urlEqualTo("/patients"))
                .withHeader("X-User-Id", equalTo(USER_ID.toString()))
                .withHeader("X-Clinic-Id", equalTo(CLINIC_ID.toString()))
                .withHeader("X-Role", equalTo("CLINIC_ADMIN")));
        verify(getRequestedFor(urlEqualTo("/clinics"))
                .withHeader("X-Clinic-Id", equalTo(CLINIC_ID.toString())));
        verify(getRequestedFor(urlEqualTo("/followups/pending"))
                .withHeader("X-Clinic-Id", equalTo(CLINIC_ID.toString())));
    }

    private String buildToken() {
        SecretKey key = Keys.hmacShaKeyFor(JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(USER_ID.toString())
                .claim("clinicId", CLINIC_ID.toString())
                .claim("role", "CLINIC_ADMIN")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }
}
