package com.securefileshare.share;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securefileshare.share.dto.CreateShareRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * End-to-end integration tests for share-service against containerized PostgreSQL.
 * WHAT: Verifies share link creation, listing, public token resolution, revocation, and JWT authorization.
 * WHY: Validates Phase 1 Share Service requirements against live database container.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class ShareServiceIntegrationTest {

    private static final String TEST_JWT_SECRET = "super_secret_test_jwt_key_share_service_2026_at_least_32_chars!";

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String host = System.getenv().getOrDefault("POSTGRES_HOST", "localhost");
        String port = System.getenv().getOrDefault("POSTGRES_PORT", "5432");
        String user = System.getenv().getOrDefault("POSTGRES_USER", "sfs_admin");
        String pass = System.getenv().getOrDefault("POSTGRES_PASSWORD", "sfs_secure_pass_2026");

        registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + host + ":" + port + "/sfs_share");
        registry.add("spring.datasource.username", () -> user);
        registry.add("spring.datasource.password", () -> pass);
        registry.add("security.jwt.secret", () -> TEST_JWT_SECRET);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Verify health endpoint returns UP for share-service")
    void testHealthEndpoint() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.service").value("share-service"));
    }

    @Test
    @DisplayName("Share lifecycle: Create share -> List shares -> Resolve share by token -> Revoke share")
    void testCompleteShareLifecycle() throws Exception {
        UUID ownerId = UUID.randomUUID();
        String ownerToken = generateTestJwt(ownerId, "alice", "ROLE_SENDER_RECIPIENT");
        UUID fileId = UUID.randomUUID();

        // 1. Create share link
        CreateShareRequest createRequest = new CreateShareRequest(
                fileId,
                "confidential_doc.pdf",
                "bob",
                "24 hours"
        );

        MvcResult createResult = mockMvc.perform(post("/shares")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.fileName").value("confidential_doc.pdf"))
                .andExpect(jsonPath("$.data.recipientUsername").value("bob"))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.shareLink").isNotEmpty())
                .andReturn();

        JsonNode json = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String shareId = json.path("data").path("id").asText();
        String shareLink = json.path("data").path("shareLink").asText();

        // Extract token from shareLink (format: http://.../share/{token})
        String token = shareLink.substring(shareLink.lastIndexOf('/') + 1);
        assertThat(token).isNotBlank();

        // 2. List shares created by owner
        mockMvc.perform(get("/shares")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(shareId))
                .andExpect(jsonPath("$.data[0].fileName").value("confidential_doc.pdf"));

        // 3. Public token resolution (unauthenticated recipient resolving share link)
        mockMvc.perform(get("/shares/token/" + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(shareId))
                .andExpect(jsonPath("$.data.recipientUsername").value("bob"));

        // 4. Revoke share link
        mockMvc.perform(delete("/shares/" + shareId)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 5. Resolving revoked token should fail (403 Forbidden)
        mockMvc.perform(get("/shares/token/" + token))
                .andExpect(status().isForbidden());
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
