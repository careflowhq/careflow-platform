package com.careflow.authservice;

import com.careflow.authservice.client.ClinicServiceClient;
import com.careflow.authservice.entity.User;
import com.careflow.authservice.entity.UserRole;
import com.careflow.authservice.repository.InvitationRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class InviteIntegrationTest {

    private static final UUID CLINIC_ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");
    private static final UUID ADMIN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

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
    private InvitationRepository invitationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockBean
    private ClinicServiceClient clinicServiceClient;

    @BeforeEach
    void setUp() {
        invitationRepository.deleteAll();
        userRepository.deleteAll();

        userRepository.save(User.builder()
                .clinicId(CLINIC_ID)
                .fullName("Clinic Admin")
                .email("admin@test.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .role(UserRole.CLINIC_ADMIN)
                .build());
    }

    @Test
    void inviteRequiresTenantHeaders() throws Exception {
        mockMvc.perform(post("/auth/invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName":"Dr. Ana",
                                  "email":"ana@test.com",
                                  "role":"DOCTOR"
                                }
                                """))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void inviteReturns403ForNonAdminRole() throws Exception {
        mockMvc.perform(post("/auth/invite")
                        .header("X-User-Id", ADMIN_ID.toString())
                        .header("X-Clinic-Id", CLINIC_ID.toString())
                        .header("X-Role", "DOCTOR")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName":"Assistant",
                                  "email":"assist@test.com",
                                  "role":"ASSISTANT"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Only CLINIC_ADMIN can invite staff"));
    }

    @Test
    void inviteAndRegisterInviteCreatesStaffUser() throws Exception {
        mockMvc.perform(post("/auth/invite")
                        .header("X-User-Id", ADMIN_ID.toString())
                        .header("X-Clinic-Id", CLINIC_ID.toString())
                        .header("X-Role", "CLINIC_ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName":"Dr. Ana",
                                  "email":"ana@test.com",
                                  "role":"DOCTOR"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("ana@test.com"))
                .andExpect(jsonPath("$.role").value("DOCTOR"));

        String token = invitationRepository.findAll().getFirst().getToken();

        mockMvc.perform(post("/auth/register-invite")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "token":"%s",
                                  "password":"doctor123"
                                }
                                """.formatted(token)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ana@test.com","password":"doctor123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }
}
