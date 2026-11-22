package com.securefileshare.share.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Client for communicating with auth-service from share-service.
 * WHAT: Asynchronously notifies auth-service to dispatch share/invite emails to recipients.
 * WHY: Decouples share creation from email dispatch and supports non-registered recipient invites.
 */
@Component
public class AuthServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceClient.class);

    private final RestClient restClient;
    private final String authServiceUrl;

    public AuthServiceClient(
            @Value("${services.auth.url:http://localhost:8081}") String authServiceUrl) {
        this.authServiceUrl = authServiceUrl;
        this.restClient = RestClient.builder()
                .baseUrl(authServiceUrl)
                .build();
    }

    public void notifyShare(String recipientIdentifier, String senderUsername, String fileName) {
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, String> payload = Map.of(
                        "recipientIdentifier", recipientIdentifier,
                        "senderUsername", senderUsername != null ? senderUsername : "Unknown",
                        "fileName", fileName != null ? fileName : "—"
                );

                restClient.post()
                        .uri("/api/v1/auth/notify-share")
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(payload)
                        .retrieve()
                        .toBodilessEntity();

                log.info("Share notification sent to auth-service for recipient {}", recipientIdentifier);
            } catch (Exception e) {
                log.error("Failed to notify auth-service of share for {}: {}", recipientIdentifier, e.getMessage());
            }
        });
    }
}
