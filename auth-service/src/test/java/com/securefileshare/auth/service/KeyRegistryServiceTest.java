package com.securefileshare.auth.service;

import com.securefileshare.auth.crypto.KeyValidator;
import com.securefileshare.auth.entity.KeyRegistry;
import com.securefileshare.auth.entity.Role;
import com.securefileshare.auth.entity.User;
import com.securefileshare.auth.repository.KeyRegistryRepository;
import com.securefileshare.auth.repository.UserRepository;
import com.securefileshare.common.exception.ApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * QA Unit and Integration test for KeyRegistryService identifier resolution.
 * WHAT: Tests robust key lookup by UUID, username, and email address.
 * WHY: Verifies that recipient encryption works seamlessly whether clients pass UUID, username, or email.
 */
class KeyRegistryServiceTest {

    private KeyRegistryRepository keyRegistryRepository;
    private UserRepository userRepository;
    private KeyValidator keyValidator;
    private KeyRegistryService keyRegistryService;

    @BeforeEach
    void setUp() {
        keyRegistryRepository = Mockito.mock(KeyRegistryRepository.class);
        userRepository = Mockito.mock(UserRepository.class);
        keyValidator = Mockito.mock(KeyValidator.class);

        keyRegistryService = new KeyRegistryService(
                keyRegistryRepository,
                userRepository,
                keyValidator
        );
    }

    @Test
    @DisplayName("QA Test: Should resolve active keys by valid email identifier")
    void testGetActiveKeysByEmail() {
        UUID userId = UUID.randomUUID();
        String email = "bob@example.com";
        User bob = new User("bob", email, "hash", Role.SENDER_RECIPIENT);
        bob.setId(userId);

        KeyRegistry keyRegistry = new KeyRegistry(bob, "rsa_pub", "ec_pub", "rsa_fp", "ec_fp");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(bob));
        when(keyRegistryRepository.findActiveByUserId(userId)).thenReturn(Optional.of(keyRegistry));

        var response = keyRegistryService.getActiveKeysByIdentifier(email);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getRsaPublicKey()).isEqualTo("rsa_pub");
    }

    @Test
    @DisplayName("QA Test: Should resolve active keys by username identifier")
    void testGetActiveKeysByUsername() {
        UUID userId = UUID.randomUUID();
        String username = "alice";
        User alice = new User(username, "alice@example.com", "hash", Role.SENDER_RECIPIENT);
        alice.setId(userId);

        KeyRegistry keyRegistry = new KeyRegistry(alice, "rsa_pub_alice", "ec_pub", "rsa_fp", "ec_fp");

        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        when(userRepository.findByEmail(username)).thenReturn(Optional.empty());
        when(userRepository.findByUsername(username)).thenReturn(Optional.of(alice));
        when(keyRegistryRepository.findActiveByUserId(userId)).thenReturn(Optional.of(keyRegistry));

        var response = keyRegistryService.getActiveKeysByIdentifier(username);

        assertThat(response).isNotNull();
        assertThat(response.getUserId()).isEqualTo(userId);
        assertThat(response.getRsaPublicKey()).isEqualTo("rsa_pub_alice");
    }

    @Test
    @DisplayName("QA Test: Non-existent identifier throws 404 ApiException")
    void testGetActiveKeysNotFound() {
        String unknown = "stranger@example.com";

        when(userRepository.findById(anyUuid())).thenReturn(Optional.empty());
        when(userRepository.findByEmail(unknown)).thenReturn(Optional.empty());
        when(userRepository.findByUsername(unknown)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> keyRegistryService.getActiveKeysByIdentifier(unknown))
                .isInstanceOf(ApiException.class)
                .hasFieldOrPropertyWithValue("status", org.springframework.http.HttpStatus.NOT_FOUND);
    }

    private UUID anyUuid() {
        return org.mockito.ArgumentMatchers.any(UUID.class);
    }
}
