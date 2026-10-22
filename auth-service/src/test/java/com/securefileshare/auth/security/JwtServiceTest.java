package com.securefileshare.auth.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for JWT generation, claim parsing, and cryptographic signature validation.
 * WHAT: Verifies token issuance, subject/role claims, JTI uniqueness, and tamper detection.
 * WHY: Ensures issued tokens cannot be modified or forged by malicious actors.
 */
class JwtServiceTest {

    private JwtService jwtService;
    private final String secret = "test_super_secret_jwt_signing_key_at_least_32_characters_long!";
    private final long expirationMs = 900000; // 15 minutes

    @BeforeEach
    void setUp() {
        this.jwtService = new JwtService(secret, expirationMs);
    }

    @Test
    @DisplayName("Should generate valid token with correct claims and JTI")
    void testTokenGenerationAndClaimsExtraction() {
        UUID userId = UUID.randomUUID();
        String username = "alice";
        String role = "ROLE_SENDER_RECIPIENT";

        String token = jwtService.generateToken(userId, username, role);

        assertThat(token).isNotBlank();
        assertThat(jwtService.validateToken(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isEqualTo(userId);
        assertThat(jwtService.extractUsername(token)).isEqualTo(username);
        assertThat(jwtService.extractRole(token)).isEqualTo(role);
        assertThat(jwtService.extractJti(token)).isNotBlank();
    }

    @Test
    @DisplayName("Should detect and reject tampered token")
    void testTamperedTokenRejected() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "bob", "ROLE_SENDER_RECIPIENT");

        // Split token into header.payload.signature
        String[] parts = token.split("\\.");
        assertThat(parts).hasSize(3);

        // Tamper with the payload (middle segment)
        String tamperedToken = parts[0] + "." + parts[1] + "tampered" + "." + parts[2];

        // Why: Cryptographic signature mismatch must cause validation to return false
        assertThat(jwtService.validateToken(tamperedToken)).isFalse();
    }

    @Test
    @DisplayName("Should generate distinct JTI for consecutive tokens for same user")
    void testJtiUniqueness() {
        UUID userId = UUID.randomUUID();

        String token1 = jwtService.generateToken(userId, "charlie", "ROLE_SENDER_RECIPIENT");
        String token2 = jwtService.generateToken(userId, "charlie", "ROLE_SENDER_RECIPIENT");

        String jti1 = jwtService.extractJti(token1);
        String jti2 = jwtService.extractJti(token2);

        // Why: Unique JTI per token allows single-session logout without revoking other sessions
        assertThat(jti1).isNotEqualTo(jti2);
    }
}
