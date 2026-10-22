package com.securefileshare.common.dto;

import java.time.Instant;

/**
 * Standardized health probe response payload.
 * WHAT: Conveys service liveness, name, and current UTC time.
 * WHY: Provides container orchestrators, load balancers, and developers a uniform /health check.
 */
public class HealthResponse {

    private String status;
    private String service;
    private Instant timestamp;

    public HealthResponse() {
        this.timestamp = Instant.now();
    }

    public HealthResponse(String status, String service) {
        this.status = status;
        this.service = service;
        this.timestamp = Instant.now();
    }

    public static HealthResponse up(String service) {
        return new HealthResponse("UP", service);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}
