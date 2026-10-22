package com.securefileshare.file.repository;

import com.securefileshare.file.entity.FileKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for FileKey entities.
 * WHAT: Manages persistence and lookup of wrapped AES keys for recipients.
 * WHY: Enables zero-knowledge authorization: verifies if a user has a wrapped key for a file,
 *      and returns their specific wrapped key during package retrieval.
 */
@Repository
public interface FileKeyRepository extends JpaRepository<FileKey, UUID> {

    /**
     * Finds the wrapped key record for a given file and recipient.
     * WHAT: Looks up the FileKey by file ID and recipient user ID.
     * WHY: Retrieves the caller's specific wrapped AES key during GET /files/{id}/package.
     *
     * @param fileRecordId UUID of the file
     * @param recipientId  UUID of the recipient user
     * @return Optional containing FileKey if found
     */
    Optional<FileKey> findByFileRecord_IdAndRecipientId(UUID fileRecordId, UUID recipientId);

    /**
     * Checks if a wrapped key exists for a given file and recipient.
     * WHAT: Checks existence without loading the entire entity into memory.
     * WHY: Supports quick permission checks and prevents duplicate recipient key creation.
     *
     * @param fileRecordId UUID of the file
     * @param recipientId  UUID of the recipient user
     * @return true if key exists for this file and recipient
     */
    boolean existsByFileRecord_IdAndRecipientId(UUID fileRecordId, UUID recipientId);

    /**
     * Retrieves all wrapped keys associated with a file.
     * WHAT: Returns all recipient keys for the specified file.
     * WHY: Allows file owners to inspect who can decrypt the file.
     *
     * @param fileRecordId UUID of the file
     * @return List of FileKey entities
     */
    List<FileKey> findByFileRecord_Id(UUID fileRecordId);
}
