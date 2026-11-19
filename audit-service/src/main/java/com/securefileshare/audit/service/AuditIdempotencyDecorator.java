package com.securefileshare.audit.service;

import com.securefileshare.audit.dto.AuditEntryDto;
import com.securefileshare.audit.dto.LogEventRequest;
import com.securefileshare.audit.entity.AuditRecord;
import com.securefileshare.audit.repository.AuditRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Enhanced AuditService with idempotency checking to prevent duplicate event ingestion.
 */
@Service
public class AuditIdempotencyDecorator {

    private final AuditService auditService;
    // In-memory deduplication cache for demo/test outbox ingestion
    private final Set<String> processedEventIds = new HashSet<>();

    public AuditIdempotencyDecorator(AuditService auditService) {
        this.auditService = auditService;
    }

    @Transactional
    public synchronized AuditEntryDto logEventIdempotent(UUID eventId, LogEventRequest request) {
        if (eventId != null) {
            String key = eventId.toString();
            if (processedEventIds.contains(key)) {
                // Already processed (idempotent ignore)
                return new AuditEntryDto(0L, java.time.Instant.now(), request.getActor(), request.getAction(), request.getFileName(), "SUCCESS", "");
            }
            processedEventIds.add(key);
        }
        return auditService.logEvent(request);
    }
}
