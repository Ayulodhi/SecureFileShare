package com.securefileshare.auth.repository;

import com.securefileshare.auth.entity.KeyRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for the KeyRegistry entity.
 * WHAT: Provides queries to fetch active public keys and perform key revocation updates.
 * WHY: Supports retrieval of recipient public keys for key wrapping and ECDSA signature checks.
 */
@Repository
public interface KeyRegistryRepository extends JpaRepository<KeyRegistry, UUID> {

    /**
     * Finds the currently active (non-revoked) key record for a given user ID.
     *
     * @param userId Target user UUID
     * @return Optional containing the active KeyRegistry entity if present
     */
    @Query("SELECT k FROM KeyRegistry k WHERE k.user.id = :userId AND k.revoked = false")
    Optional<KeyRegistry> findActiveByUserId(@Param("userId") UUID userId);

    /**
     * Marks all currently active key records for a user as revoked during key rotation.
     * Why: Prevents silent overwriting while maintaining historical keys for verification.
     *
     * @param userId Target user UUID
     */
    @Modifying
    @Query("UPDATE KeyRegistry k SET k.revoked = true WHERE k.user.id = :userId AND k.revoked = false")
    void revokeActiveKeysByUserId(@Param("userId") UUID userId);
}
