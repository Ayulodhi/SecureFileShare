package com.securefileshare.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end integration tests for audit-service against containerized PostgreSQL.
 * WHAT: Verifies Spring Boot context boot, Flyway database initialization for sfs_audit, and health endpoint.
 * WHY: Validates service availability and container readiness for zero-downtime deployment.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class AuditServiceIntegrationTest {

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String host = System.getenv().getOrDefault("POSTGRES_HOST", "localhost");
        String port = System.getenv().getOrDefault("POSTGRES_PORT", "5432");
        String user = System.getenv().getOrDefault("POSTGRES_USER", "sfs_admin");
        String pass = System.getenv().getOrDefault("POSTGRES_PASSWORD", "sfs_secure_pass_2026");

        registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + host + ":" + port + "/sfs_audit");
        registry.add("spring.datasource.username", () -> user);
        registry.add("spring.datasource.password", () -> pass);
    }

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Verify health endpoint returns UP for audit-service")
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("audit-service"));
    }
}
