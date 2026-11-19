package com.securefileshare.audit.repository;

import com.securefileshare.audit.entity.AuditRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Spring Data JPA repository for AuditRecord entities.
 * WHAT: Provides queries for chronological audit traversal and pagination with pessimistic locking.
 * WHY: Powers audit log listing and prevents concurrent race conditions on hash chains.
 */
@Repository
public interface AuditRepository extends JpaRepository<AuditRecord, Long> {

    List<AuditRecord> findAllByOrderByIdAsc();

    Optional<AuditRecord> findTopByOrderByIdDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AuditRecord> findFirstByOrderByIdDesc();

    Page<AuditRecord> findByActorContainingIgnoreCaseOrActionContainingIgnoreCaseOrderByIdDesc(
            String actor, String action, Pageable pageable);

    Page<AuditRecord> findByActorContainingIgnoreCaseOrderByIdDesc(String actor, Pageable pageable);

    Page<AuditRecord> findByActionContainingIgnoreCaseOrderByIdDesc(String action, Pageable pageable);

    Page<AuditRecord> findAllByOrderByIdDesc(Pageable pageable);
}
