package com.securefileshare.share;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securefileshare.share.dto.CreateShareRequest;
import com.securefileshare.share.entity.ShareRecord;
import com.securefileshare.share.repository.ShareRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
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
public class ShareServiceIntegrationTest extends BaseShareIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ShareRepository shareRepository;

    @BeforeEach
    void setUp() {
        shareRepository.deleteAll();
    }

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

        String token = shareLink.substring(shareLink.lastIndexOf('/') + 1);
        assertThat(token).isNotBlank();

        // 2. List shares created by owner
        mockMvc.perform(get("/shares")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(shareId))
                .andExpect(jsonPath("$.data[0].fileName").value("confidential_doc.pdf"));

        // 3. Public token resolution
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

        // 5. Resolving revoked token -> 403 Forbidden
        mockMvc.perform(get("/shares/token/" + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Revoked share test: attempting to resolve a revoked share token returns 403 Forbidden")
    void testRevokedShareReturnsForbidden() throws Exception {
        ShareRecord record = new ShareRecord(
                "sr_revoked1", UUID.randomUUID(), UUID.randomUUID(), "doc.pdf", "bob", "revoked_token_123", Instant.now().plusSeconds(3600)
        );
        record.setStatus("REVOKED");
        shareRepository.save(record);

        mockMvc.perform(get("/shares/token/revoked_token_123"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Expired share test: attempting to resolve an expired share token returns 403 Forbidden")
    void testExpiredShareReturnsForbidden() throws Exception {
        ShareRecord record = new ShareRecord(
                "sr_expired1", UUID.randomUUID(), UUID.randomUUID(), "doc.pdf", "bob", "expired_token_123", Instant.now().minusSeconds(3600)
        );
        record.setStatus("ACTIVE");
        shareRepository.save(record);

        mockMvc.perform(get("/shares/token/expired_token_123"))
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
