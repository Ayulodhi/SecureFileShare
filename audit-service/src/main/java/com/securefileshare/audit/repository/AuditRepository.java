package com.securefileshare.audit.repository;

import com.securefileshare.audit.entity.AuditRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for AuditRecord entities.
 * WHAT: Provides queries for chronological audit traversal and pagination.
 * WHY: Powers audit log listing and hash chain verification.
 */
@Repository
public interface AuditRepository extends JpaRepository<AuditRecord, Long> {

    List<AuditRecord> findAllByOrderByIdAsc();

    Optional<AuditRecord> findTopByOrderByIdDesc();

    Page<AuditRecord> findByActorContainingIgnoreCaseOrActionContainingIgnoreCaseOrderByIdDesc(
            String actor, String action, Pageable pageable);

    Page<AuditRecord> findByActorContainingIgnoreCaseOrderByIdDesc(String actor, Pageable pageable);

    Page<AuditRecord> findByActionContainingIgnoreCaseOrderByIdDesc(String action, Pageable pageable);

    Page<AuditRecord> findAllByOrderByIdDesc(Pageable pageable);
}
