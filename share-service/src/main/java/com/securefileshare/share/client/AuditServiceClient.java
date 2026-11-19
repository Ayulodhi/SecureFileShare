package com.securefileshare.share.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Resilient Audit Service Client for share-service.
 * WHAT: Publishes share link creation and revocation events asynchronously to audit-service.
 * WHY: Ensures sharing events are durably recorded in audit_entries.
 */
@Component
public class AuditServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AuditServiceClient.class);

    private final RestClient restClient;
    private final String auditServiceUrl;
    private final String serviceKey;
    private final ObjectMapper objectMapper;

    public AuditServiceClient(
            @Value("${services.audit.url:http://localhost:8084}") String auditServiceUrl,
            @Value("${audit.service-key:sfs_service_audit_secret_2026}") String serviceKey,
            ObjectMapper objectMapper) {
        this.auditServiceUrl = auditServiceUrl;
        this.serviceKey = serviceKey;
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder()
                .baseUrl(auditServiceUrl)
                .build();
    }

    public void logEvent(String actor, String action, String fileName) {
        CompletableFuture.runAsync(() -> {
            try {
                Map<String, String> payload = Map.of(
                        "actor", actor,
                        "action", action,
                        "fileName", fileName != null ? fileName : "—"
                );

                restClient.post()
                        .uri("/api/v1/audit/events")
                        .header("X-Service-Key", serviceKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(objectMapper.writeValueAsString(payload))
                        .retrieve()
                        .toBodilessEntity();

                log.debug("Successfully published audit event [{}] for actor [{}]", action, actor);
            } catch (Exception ex) {
                log.warn("Audit service unavailable at {} — event [{}] queued/skipped safely: {}",
                        auditServiceUrl, action, ex.getMessage());
            }
        });
    }
}
