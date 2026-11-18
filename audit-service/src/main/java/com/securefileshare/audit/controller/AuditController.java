package com.securefileshare.audit.controller;

import com.securefileshare.audit.dto.AuditEntryDto;
import com.securefileshare.audit.dto.AuditVerifyResultDto;
import com.securefileshare.audit.dto.LogEventRequest;
import com.securefileshare.audit.service.AuditService;
import com.securefileshare.common.exception.ApiException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller exposing audit log querying, chain verification, and event logging endpoints.
 * WHAT: Handles GET /audit/entries, GET /audit/verify, and POST /audit/events.
 * WHY: Powers admin audit oversight and microservice compliance logging.
 */
@RestController
@RequestMapping("/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    /**
     * Retrieves paginated audit log entries with optional filters.
     */
    @GetMapping("/entries")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AuditEntryDto>>> getAuditEntries(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) String action) {
        List<AuditEntryDto> entries = auditService.getAuditEntries(page, size, actor, action);
        return ResponseEntity.ok(ApiResponse.success("Audit entries retrieved successfully", entries));
    }

    /**
     * Verifies the integrity of the audit log hash chain.
     */
    @GetMapping("/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AuditVerifyResultDto>> verifyChain() {
        AuditVerifyResultDto result = auditService.verifyChain();
        return ResponseEntity.ok(ApiResponse.success("Audit chain verification completed", result));
    }

    @Value("${audit.service-key:sfs_service_audit_secret_2026}")
    private String expectedServiceKey;

    /**
     * Ingests an audit event from internal microservices.
     */
    @PostMapping("/events")
    public ResponseEntity<ApiResponse<AuditEntryDto>> logEvent(
            @RequestHeader(value = "X-Service-Key", required = false) String serviceKey,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @Valid @RequestBody LogEventRequest request) {

        if (serviceKey == null || serviceKey.isBlank()) {
            if (authorization != null && authorization.startsWith("Bearer ")) {
                throw new ApiException(HttpStatus.FORBIDDEN, "Access denied: service credential required for audit ingestion");
            }
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Missing service credential");
        }

        if (!expectedServiceKey.equals(serviceKey)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid service credential");
        }

        AuditEntryDto entry = auditService.logEvent(request);
        return new ResponseEntity<>(ApiResponse.success("Audit event logged successfully", entry), HttpStatus.CREATED);
    }
}
