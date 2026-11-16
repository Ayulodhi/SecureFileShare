package com.securefileshare.auth;

import com.securefileshare.auth.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Phase 7 security tests: token-level attack scenarios against protected endpoints.
 * WHAT: Verifies that wrong tokens, expired tokens, and revoked (blacklisted) tokens
 *       are all rejected with 401 Unauthorized before reaching any business logic.
 * WHY: Guarantees the JWT authentication layer is impenetrable to common token abuse vectors.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class Phase7SecurityTest {

    private static final String TEST_JWT_SECRET = "super_secret_test_jwt_key_auth_service_2026_at_least_32_chars!";

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        String host = System.getenv().getOrDefault("POSTGRES_HOST", "localhost");
        String port = System.getenv().getOrDefault("POSTGRES_PORT", "5432");
        String user = System.getenv().getOrDefault("POSTGRES_USER", "sfs_admin");
        String pass = System.getenv().getOrDefault("POSTGRES_PASSWORD", "sfs_secure_pass_2026");

        registry.add("spring.datasource.url", () -> "jdbc:postgresql://" + host + ":" + port + "/sfs_auth");
        registry.add("spring.datasource.username", () -> user);
        registry.add("spring.datasource.password", () -> pass);

        registry.add("security.jwt.secret", () -> TEST_JWT_SECRET);
        registry.add("security.jwt.expiration-ms", () -> 3600000L);
        registry.add("security.rate-limit.max-attempts", () -> 5);
        registry.add("security.rate-limit.window-minutes", () -> 15);
        registry.add("security.admin.initial-username", () -> "test_admin");
        registry.add("security.admin.initial-email", () -> "test_admin@securefileshare.com");
        registry.add("security.admin.initial-password", () -> "Admin_Secure_Pass_2026!");
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @BeforeEach
    void setupRedisMock() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        // Default: no JTI is blacklisted — valid tokens pass the revocation check
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
    }

    @Test
    @DisplayName("Phase 7 — Wrong token: completely invalid JWT is rejected with 401 Unauthorized")
    void testWrongTokenRejected() throws Exception {
        // Send a garbage Bearer token to a protected endpoint
        // Why: JwtAuthenticationFilter calls jwtService.validateToken() → false → immediate 401
        mockMvc.perform(post("/auth/keys")
                        .header("Authorization", "Bearer this.is.a.completely.invalid.garbage.token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid or expired JWT token"));
    }

    @Test
    @DisplayName("Phase 7 — Expired token: token with 1 ms lifetime is rejected with 401 after expiry")
    void testExpiredTokenRejected() throws Exception {
        // Build a JwtService with a 1 ms lifetime — token expires almost instantly
        // Why: Verifies that the exp claim is enforced; the JJWT parser rejects past-expiry tokens
        JwtService shortLivedJwtService = new JwtService(TEST_JWT_SECRET, 1L);
        String expiredToken = shortLivedJwtService.generateToken(
                UUID.randomUUID(), "expiry_test_user", "ROLE_SENDER_RECIPIENT");

        // Sleep long enough to guarantee the token has expired
        Thread.sleep(50);

        mockMvc.perform(post("/auth/keys")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid or expired JWT token"));
    }

    @Test
    @DisplayName("Phase 7 — Revoked token: blacklisted JTI is rejected with 401 after logout")
    void testRevokedTokenBlacklistedJtiRejected() throws Exception {
        // Generate a valid, non-expired token
        JwtService jwtService = new JwtService(TEST_JWT_SECRET, 3600000L);
        String validToken = jwtService.generateToken(
                UUID.randomUUID(), "revoked_user", "ROLE_SENDER_RECIPIENT");

        // Simulate the Redis blacklist containing this token's JTI
        // Why: AuthService.logout() stores the JTI in Redis; JwtAuthenticationFilter checks it
        String jti = jwtService.extractJti(validToken);
        String blacklistKey = "jwt:blacklist:" + jti;
        when(redisTemplate.hasKey(blacklistKey)).thenReturn(true);

        mockMvc.perform(post("/auth/keys")
                        .header("Authorization", "Bearer " + validToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Token has been revoked. Please log in again."));
    }
}
