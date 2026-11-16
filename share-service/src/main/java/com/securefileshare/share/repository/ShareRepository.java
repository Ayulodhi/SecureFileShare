package com.securefileshare.share.repository;

import com.securefileshare.share.entity.ShareRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for ShareRecord entities.
 * WHAT: Provides CRUD and query methods for share records.
 * WHY: Powers database access for share creation, revocation, and token resolution.
 */
@Repository
public interface ShareRepository extends JpaRepository<ShareRecord, String> {

    List<ShareRecord> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    Optional<ShareRecord> findByShareToken(String shareToken);
}
