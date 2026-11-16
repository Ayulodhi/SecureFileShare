package com.securefileshare.audit.controller;

import com.securefileshare.audit.dto.AuditEntryDto;
import com.securefileshare.audit.dto.AuditVerifyResultDto;
import com.securefileshare.audit.dto.LogEventRequest;
import com.securefileshare.audit.service.AuditService;
import com.securefileshare.common.dto.ApiResponse;
import jakarta.validation.Valid;
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

    /**
     * Ingests an audit event from internal microservices.
     */
    @PostMapping("/events")
    public ResponseEntity<ApiResponse<AuditEntryDto>> logEvent(@Valid @RequestBody LogEventRequest request) {
        AuditEntryDto entry = auditService.logEvent(request);
        return new ResponseEntity<>(ApiResponse.success("Audit event logged successfully", entry), HttpStatus.CREATED);
    }
}
