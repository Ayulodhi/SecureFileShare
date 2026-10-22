package com.securefileshare.file.client;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.securefileshare.common.dto.ApiResponse;
import com.securefileshare.common.exception.ApiException;
import com.securefileshare.file.dto.KeyRegistryResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.UUID;


@Component
public class AuthServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceClient.class);

    private final RestClient restClient;
    private final String authServiceUrl;
    private final ObjectMapper objectMapper;

    public AuthServiceClient(
            @Value("${services.auth.url:http://localhost:8081}") String authServiceUrl,
            ObjectMapper objectMapper) {
        this.authServiceUrl = authServiceUrl;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(authServiceUrl)
                .build();
    }

    /**
     * Retrieves the active public keys for a specific user from auth-service.
     * WHAT: Executes an HTTP GET request to /api/v1/auth/keys/{userId}.
     * WHY: Retrieves the owner's NIST P-256 EC public key required for ECDSA signature validation.
     *
     * @param userId UUID of the user
     * @return KeyRegistryResponseDto containing the user's registered public keys
     */
    public KeyRegistryResponseDto getActiveKeys(UUID userId) {
        log.debug("Fetching registered public keys for user {} from auth-service ({})", userId, authServiceUrl);

        try {
            String rawJson = restClient.get()
                    .uri("/api/v1/auth/keys/{userId}", userId)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(String.class);

            if (rawJson == null || rawJson.isBlank()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "Empty response from auth-service for user keys");
            }

            // Parse ApiResponse<KeyRegistryResponseDto> structure
            ApiResponse<KeyRegistryResponseDto> apiResponse = objectMapper.readValue(
                    rawJson,
                    new TypeReference<>() {}
            );

            KeyRegistryResponseDto data = apiResponse.getData();
            if (data == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "No cryptographic keys found for user " + userId);
            }

            // Security Check: Verify that the key is not revoked
            // Why: Prevents accepting files signed by compromised or revoked private keys
            if (data.isRevoked()) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "The owner's cryptographic keys have been revoked");
            }

            return data;
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Failed to retrieve public keys for user {}: {}", userId, ex.getMessage());
            throw new ApiException(HttpStatus.BAD_REQUEST,
                    "Failed to fetch owner public key from auth-service: " + ex.getMessage());
        }
    }
}
