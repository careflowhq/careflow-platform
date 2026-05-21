package com.careflow.authservice;

import com.careflow.authservice.client.ClinicServiceClient;
import com.careflow.authservice.entity.User;
import com.careflow.authservice.entity.UserRole;
import com.careflow.authservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class AuthControllerIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("auth_db")
            .withUsername("careflow")
            .withPassword("careflow");

    @DynamicPropertySource
    static void configureDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("careflow.jwt.secret", () -> "careflow-secret-key-careflow-secret-key");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private ClinicServiceClient clinicServiceClient;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        when(clinicServiceClient.onboardClinic(anyString(), anyString(), anyString()))
                .thenReturn(UUID.randomUUID());
    }

    @Test
    void loginReturns401ForUnknownEmail() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"unknown@test.com","password":"secret123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void loginReturns401ForWrongPassword() throws Exception {
        userRepository.save(User.builder()
                .clinicId(UUID.randomUUID())
                .fullName("Test User")
                .email("user@test.com")
                .passwordHash(passwordEncoder.encode("correct-password"))
                .role(UserRole.CLINIC_ADMIN)
                .build());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@test.com","password":"wrong-password"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid email or password"));
    }

    @Test
    void loginReturns200ForValidCredentials() throws Exception {
        userRepository.save(User.builder()
                .clinicId(UUID.randomUUID())
                .fullName("Test User")
                .email("user@test.com")
                .passwordHash(passwordEncoder.encode("secret123"))
                .role(UserRole.CLINIC_ADMIN)
                .build());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@test.com","password":"secret123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void registerReturns409ForDuplicateEmail() throws Exception {
        userRepository.save(User.builder()
                .clinicId(UUID.randomUUID())
                .fullName("Existing User")
                .email("dup@test.com")
                .passwordHash(passwordEncoder.encode("secret123"))
                .role(UserRole.CLINIC_ADMIN)
                .build());

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName":"New User",
                                  "email":"dup@test.com",
                                  "password":"secret123",
                                  "role":"CLINIC_ADMIN",
                                  "clinicName":"Clinic",
                                  "country":"PE",
                                  "timezone":"America/Lima"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Email already registered: dup@test.com"));
    }
}
