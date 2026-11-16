package com.securefileshare.audit.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO for ingesting audit events from other microservices.
 * WHAT: Captures actor, action, and file name.
 * WHY: Enables decentralized event logging into the centralized audit hash chain.
 */
public class LogEventRequest {

    @NotBlank(message = "Actor is required")
    private String actor;

    @NotBlank(message = "Action is required")
    private String action;

    private String fileName;

    public LogEventRequest() {}

    public LogEventRequest(String actor, String action, String fileName) {
        this.actor = actor;
        this.action = action;
        this.fileName = fileName != null ? fileName : "—";
    }

    public String getActor() { return actor; }
    public String getAction() { return action; }
    public String getFileName() { return fileName != null ? fileName : "—"; }
}
