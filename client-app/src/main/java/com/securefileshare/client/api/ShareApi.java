package com.securefileshare.client.api;

import com.securefileshare.client.model.ReceivedPackage;
import com.securefileshare.client.model.ShareLinkResult;
import com.securefileshare.client.model.ShareRecordDto;

import java.util.List;

/**
 * Service API interface for managing encrypted share permissions.
 * WHAT: Declares contract for listing issued share grants, creating secure share links,
 *       revoking recipient access, and validating/downloading received packages.
 * WHY: Decouples the ShareDialog, SharesCreatedPanel, and ReceivePanel from backend share-service logic.
 */
public interface ShareApi {

    /**
     * Lists all file shares created by the authenticated user.
     *
     * @return List of active and historical ShareRecordDto entries
     * @throws Exception on service failure
     */
    List<ShareRecordDto> getSharesCreated() throws Exception;

    /**
     * Instantly revokes an active share grant, blocking further recipient key unwrap operations.
     *
     * @param shareId Unique identifier of the share grant
     * @throws Exception on service failure
     */
    void revokeShare(String shareId) throws Exception;

    /**
     * Creates a new encrypted share link grant for a recipient user with specified expiry window.
     *
     * @param fileId            Target encrypted file ID
     * @param fileName          Display file name
     * @param recipientUsername Target recipient username
     * @param expiryOption      Expiry duration (1 hour, 24 hours, 7 days, 30 days)
     * @return ShareLinkResult containing generated link URL and expiration timestamp
     * @throws Exception on invalid recipient or service failure
     */
    ShareLinkResult createShareLink(String fileId, String fileName, String recipientUsername, String expiryOption) throws Exception;

    /**
     * Validates a share link URL and downloads the encrypted package for recipient verification.
     *
     * @param shareLink       Full or tokenized share link URL
     * @param currentUsername Currently authenticated recipient username
     * @return ReceivedPackage containing encrypted payload, metadata, and validation parameters
     * @throws Exception on link lookup or download failure
     */
    ReceivedPackage validateAndDownloadPackage(String shareLink, String currentUsername) throws Exception;
}
