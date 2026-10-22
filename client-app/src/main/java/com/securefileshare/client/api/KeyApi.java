package com.securefileshare.client.api;

import com.securefileshare.client.model.KeyDetailsDto;

/**
 * Service API interface for local cryptographic key management.
 * WHAT: Declares contract for querying active public key fingerprints and keystore metadata.
 * WHY: Decouples the "My keys" panel from PKCS12 keystore file management.
 */
public interface KeyApi {

    /**
     * Retrieves the current user's cryptographic key details and SHA-256 fingerprints.
     *
     * @return KeyDetailsDto containing public key fingerprints and keystore status
     * @throws Exception on keystore read failure
     */
    KeyDetailsDto getMyKeys() throws Exception;
}
