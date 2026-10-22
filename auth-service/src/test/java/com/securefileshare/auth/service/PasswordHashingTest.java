package com.securefileshare.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for password hashing and verification.
 * WHAT: Verifies BCrypt salted hashing behavior, cryptographic match correctness, and resistance to mismatch.
 * WHY: Validates authentication security guarantees preventing plaintext password storage.
 */
class PasswordHashingTest {

    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Instantiate BCrypt with work factor 12
        this.passwordEncoder = new BCryptPasswordEncoder(12);
    }

    @Test
    @DisplayName("Should successfully hash password and verify match")
    void testPasswordHashAndMatch() {
        String rawPassword = "CorrectHorseBatteryStaple@2026!";

        // Hash the raw password
        String hash = passwordEncoder.encode(rawPassword);

        // Verify hash is not equal to plaintext
        assertThat(hash).isNotEqualTo(rawPassword);
        assertThat(hash).startsWith("$2a$12$");

        // Verify matches method verifies correctly
        assertThat(passwordEncoder.matches(rawPassword, hash)).isTrue();
    }

    @Test
    @DisplayName("Should reject incorrect password against valid hash")
    void testWrongPasswordRejected() {
        String rawPassword = "CorrectPassword123!";
        String wrongPassword = "WrongPassword123!";

        String hash = passwordEncoder.encode(rawPassword);

        assertThat(passwordEncoder.matches(wrongPassword, hash)).isFalse();
    }

    @Test
    @DisplayName("Should produce distinct hashes for same password due to random salt")
    void testSaltingUniqueness() {
        String rawPassword = "RepeatedPassword456!";

        String hash1 = passwordEncoder.encode(rawPassword);
        String hash2 = passwordEncoder.encode(rawPassword);

        // Why: Random salting prevents attackers from using precomputed rainbow tables across accounts
        assertThat(hash1).isNotEqualTo(hash2);
        assertThat(passwordEncoder.matches(rawPassword, hash1)).isTrue();
        assertThat(passwordEncoder.matches(rawPassword, hash2)).isTrue();
    }
}
