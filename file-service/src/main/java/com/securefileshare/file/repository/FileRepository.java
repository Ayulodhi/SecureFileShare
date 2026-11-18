package com.securefileshare.file.repository;

import com.securefileshare.file.entity.FileRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Spring Data JPA repository for FileRecord entities.
 * WHAT: Provides CRUD operations and query methods for files.
 * WHY: Enables persistence and retrieval of encrypted file metadata filtered by owner.
 */
@Repository
public interface FileRepository extends JpaRepository<FileRecord, UUID> {

    /**
     * Retrieves all files uploaded by a specific owner, ordered by creation date descending.
     * WHAT: Finds file metadata records for the specified owner UUID.
     * WHY: Powers the GET /files endpoint where callers view their own uploaded files.
     *
     * @param ownerId UUID of the file owner
     * @return List of FileRecord entities
     */
    List<FileRecord> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    /**
     * Finds a file record by its unique MinIO object key.
     * WHAT: Locates file metadata associated with an object storage path.
     * WHY: Supports integrity checks and object cleanup operations.
     *
     * @param objectKey S3 object key
     * @return Optional containing matching FileRecord if found
     */
    Optional<FileRecord> findByObjectKey(String objectKey);

    /**
     * Retrieves all files shared with a specific recipient via file_keys.
     * WHAT: Finds file records where a FileKey exists for the given recipient.
     * WHY: Powers the GET /files/shared-with-me endpoint.
     *
     * @param recipientId UUID of the recipient
     * @return List of FileRecord entities shared with the recipient
     */
    @Query("SELECT fk.fileRecord FROM FileKey fk WHERE fk.recipientId = :recipientId ORDER BY fk.createdAt DESC")
    List<FileRecord> findSharedFilesForRecipient(@Param("recipientId") UUID recipientId);
}
