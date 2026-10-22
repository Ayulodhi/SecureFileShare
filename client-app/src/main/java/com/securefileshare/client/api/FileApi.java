package com.securefileshare.client.api;

import com.securefileshare.client.model.FileDto;
import com.securefileshare.client.model.SharedFileDto;

import java.util.List;

/**
 * Service API interface for encrypted file operations.
 * WHAT: Declares contract for querying owned encrypted files and files shared with the user.
 * WHY: Decouples UI table controllers from the underlying HTTP / Mock storage implementation.
 */
public interface FileApi {

    /**
     * Retrieves all encrypted files owned by the authenticated caller.
     *
     * @return List of FileDto items
     * @throws Exception on data retrieval failure
     */
    List<FileDto> getMyFiles() throws Exception;

    /**
     * Retrieves files shared with the authenticated caller by others.
     *
     * @return List of SharedFileDto items
     * @throws Exception on data retrieval failure
     */
    List<SharedFileDto> getSharedWithMe() throws Exception;
}
