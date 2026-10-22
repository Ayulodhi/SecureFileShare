package com.securefileshare.auth.service;

import com.securefileshare.auth.crypto.KeyValidator;
import com.securefileshare.auth.dto.KeyRegistrationRequest;
import com.securefileshare.auth.dto.KeyRegistryResponse;
import com.securefileshare.auth.entity.KeyRegistry;
import com.securefileshare.auth.entity.User;
import com.securefileshare.auth.repository.KeyRegistryRepository;
import com.securefileshare.auth.repository.UserRepository;
import com.securefileshare.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Objects;
import java.util.UUID;

/**
 * Service managing cryptographic public key registration, rotation, and lookups.
 * WHAT: Validates RSA-4096 and EC P-256 public keys, computes fingerprints, revokes old keys upon rotation, and persists keys.
 * WHY: CRITICAL ZERO-KNOWLEDGE FOUNDATION: Enables senders to retrieve trusted public keys for wrapping AES keys
 *      and verifying ECDSA signatures without the server ever seeing or handling private keys.
 */
@Service
public class KeyRegistryService {

    private final KeyRegistryRepository keyRegistryRepository;
    private final UserRepository userRepository;
    private final KeyValidator keyValidator;

    public KeyRegistryService(
            KeyRegistryRepository keyRegistryRepository,
            UserRepository userRepository,
            KeyValidator keyValidator) {
        this.keyRegistryRepository = keyRegistryRepository;
        this.userRepository = userRepository;
        this.keyValidator = keyValidator;
    }

    /**
     * Registers new RSA-4096 and EC P-256 public keys for the authenticated user, revoking any prior active keys.
     *
     * @param userId  Authenticated user UUID
     * @param request Validated key registration DTO
     * @return KeyRegistryResponse containing public keys and fingerprints
     */
    @Transactional
    public KeyRegistryResponse registerKeys(UUID userId, KeyRegistrationRequest request) {
        // Explicitly assert non-null UUID to satisfy Spring Data CrudRepository @NonNull parameter contract
        User user = userRepository.findById(Objects.requireNonNull(userId))
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));

        // Step 1: Validate RSA public key is well-formed and exactly 4096 bits
        // Why: Prevents weak key wrapping vulnerability
        RSAPublicKey rsaKey = keyValidator.validateRsaPublicKey(request.getRsaPublicKey());

        // Step 2: Validate EC public key is well-formed and uses NIST P-256 curve
        // Why: Ensures valid ECDSA signature verification parameters
        ECPublicKey ecKey = keyValidator.validateEcPublicKey(request.getEcPublicKey());

        // Step 3: Compute deterministic SHA-256 fingerprints of raw DER bytes
        // Why: Enables out-of-band verification by clients to detect rogue/compromised server key substitution
        String rsaFingerprint = keyValidator.computeFingerprint(rsaKey);
        String ecFingerprint = keyValidator.computeFingerprint(ecKey);

        // Step 4: Mark any existing active keys for this user as revoked (immutable audit trail)
        // Why: Security Rule: Never silently overwrite keys. Historical keys remain readable for verifying old signatures
        keyRegistryRepository.revokeActiveKeysByUserId(userId);

        // Step 5: Persist the new active public key record
        KeyRegistry newKey = new KeyRegistry(
                user,
                request.getRsaPublicKey().trim(),
                request.getEcPublicKey().trim(),
                rsaFingerprint,
                ecFingerprint
        );

        KeyRegistry saved = keyRegistryRepository.save(newKey);

        return new KeyRegistryResponse(
                saved.getId(),
                saved.getUser().getId(),
                saved.getRsaPublicKey(),
                saved.getEcPublicKey(),
                saved.getRsaFingerprint(),
                saved.getEcFingerprint(),
                saved.getCreatedAt(),
                saved.isRevoked()
        );
    }

    /**
     * Retrieves the currently active public keys and fingerprints for a target user ID.
     *
     * @param userId Target user UUID
     * @return KeyRegistryResponse containing public keys and fingerprints
     */
    @Transactional(readOnly = true)
    public KeyRegistryResponse getActiveKeys(UUID userId) {
        // Query active (revoked = false) key for the user
        // Why: Senders must always wrap files using the recipient's latest active key
        KeyRegistry keyRegistry = keyRegistryRepository.findActiveByUserId(userId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "No active public keys registered for user ID: " + userId));

        return new KeyRegistryResponse(
                keyRegistry.getId(),
                keyRegistry.getUser().getId(),
                keyRegistry.getRsaPublicKey(),
                keyRegistry.getEcPublicKey(),
                keyRegistry.getRsaFingerprint(),
                keyRegistry.getEcFingerprint(),
                keyRegistry.getCreatedAt(),
                keyRegistry.isRevoked()
        );
    }
}
