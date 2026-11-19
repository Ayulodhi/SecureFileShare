package com.securefileshare.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securefileshare.audit.dto.LogEventRequest;
import com.securefileshare.audit.repository.AuditRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests for audit-service against containerized PostgreSQL.
 * WHAT: Verifies audit event logging, hash-chain verification, pagination, and admin role authorization.
 * WHY: Validates Phase 2 Audit Service requirements against live database container.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class AuditServiceIntegrationTest extends BaseAuditIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AuditRepository auditRepository;

    @BeforeEach
    void setUp() {
        auditRepository.deleteAll();
    }

    @Test
    @DisplayName("Verify health endpoint returns UP for audit-service")
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("audit-service"));
    }

    @Test
    @DisplayName("Audit lifecycle: Log events -> Admin verifies hash chain -> Admin queries entries -> Non-admin denied")
    void testCompleteAuditLifecycleAndAccessControl() throws Exception {
        String adminToken = generateTestJwt(UUID.randomUUID(), "admin_user", "ROLE_ADMIN");
        String regularUserToken = generateTestJwt(UUID.randomUUID(), "normal_user", "ROLE_SENDER_RECIPIENT");

        // 1. Log audit events (public/internal endpoint)
        LogEventRequest event1 = new LogEventRequest("alice", "LOGIN", "—");
        mockMvc.perform(post("/audit/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event1)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.action").value("LOGIN"))
                .andExpect(jsonPath("$.data.previousHash").value("0000000000000000000000000000000000000000000000000000000000000000"));

        LogEventRequest event2 = new LogEventRequest("alice", "UPLOAD", "secret.pdf");
        mockMvc.perform(post("/audit/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event2)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.action").value("UPLOAD"))
                .andExpect(jsonPath("$.data.previousHash").isNotEmpty());

        // 2. Non-admin user cannot access audit entries or verify chain (403 Forbidden)
        mockMvc.perform(get("/audit/entries")
                        .header("Authorization", "Bearer " + regularUserToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/audit/verify")
                        .header("Authorization", "Bearer " + regularUserToken))
                .andExpect(status().isForbidden());

        // 3. Admin user successfully verifies hash chain
        mockMvc.perform(get("/audit/verify")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.checkedCount").value(2));

        // 4. Admin user retrieves paginated entries
        mockMvc.perform(get("/audit/entries")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].action").isNotEmpty());
    }

    @Test
    @DisplayName("Audit ingestion authentication: No credential -> 401; User JWT -> 403; Valid service key -> 201")
    void testAuditIngestionAuthentication() throws Exception {
        LogEventRequest event = new LogEventRequest("bot", "SYSTEM_CHECK", "none");
        String userToken = generateTestJwt(UUID.randomUUID(), "user", "ROLE_SENDER_RECIPIENT");

        // 1. No credential -> 401 Unauthorized
        mockMvc.perform(post("/audit/events")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isUnauthorized());

        // 2. User JWT without service key -> 403 Forbidden
        mockMvc.perform(post("/audit/events")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isForbidden());

        // 3. Valid service key -> 201 Created
        mockMvc.perform(post("/audit/events")
                        .header("X-Service-Key", "sfs_service_audit_secret_2026")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(event)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));
    }

    private String generateTestJwt(UUID userId, String username, String role) {
        SecretKey key = Keys.hmacShaKeyFor(TEST_JWT_SECRET.getBytes(StandardCharsets.UTF_8));
        return Jwts.builder()
                .subject(userId.toString())
                .id(UUID.randomUUID().toString())
                .claim("username", username)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 3600000))
                .signWith(key)
                .compact();
    }
}
