package com.securefileshare.audit.controller;

import com.securefileshare.common.dto.HealthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Health probe controller for audit-service.
 * WHAT: Exposes the /health liveness probe.
 * WHY: Allows container orchestrators and monitoring agents to verify the service is running.
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    /**
     * Returns the service health status.
     * WHAT: Produces a 200 OK with service identifier and current timestamp.
     * WHY: Confirms audit-service HTTP responsiveness.
     *
     * @return HealthResponse wrapped in ResponseEntity
     */
    @GetMapping
    public ResponseEntity<HealthResponse> checkHealth() {
        return ResponseEntity.ok(HealthResponse.up("audit-service"));
    }
}
