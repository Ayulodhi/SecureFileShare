package com.securefileshare.share.controller;

import com.securefileshare.common.dto.ApiResponse;
import com.securefileshare.share.dto.CreateShareRequest;
import com.securefileshare.share.dto.ShareResponse;
import com.securefileshare.share.service.ShareService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller exposing share link management and token resolution endpoints.
 * WHAT: Handles GET /shares, POST /shares, DELETE /shares/{id}, and GET /shares/token/{token}.
 * WHY: Powers share creation, revocation, and public token access for recipients.
 */
@RestController
@RequestMapping("/shares")
public class ShareController {

    private final ShareService shareService;

    public ShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    /**
     * Lists all shares created by the authenticated user.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ShareResponse>>> getSharesCreated(
            @AuthenticationPrincipal UUID userId,
            HttpServletRequest request) {
        String baseUrl = extractBaseUrl(request);
        List<ShareResponse> shares = shareService.getSharesCreated(userId, baseUrl);
        return ResponseEntity.ok(ApiResponse.success("Shares retrieved successfully", shares));
    }

    /**
     * Creates a new cryptographically bound share link.
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ShareResponse>> createShare(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody CreateShareRequest requestBody,
            HttpServletRequest request) {
        String baseUrl = extractBaseUrl(request);
        ShareResponse response = shareService.createShare(userId, requestBody, baseUrl);
        return new ResponseEntity<>(ApiResponse.success("Share link created successfully", response), HttpStatus.CREATED);
    }

    /**
     * Revokes an active share link by ID.
     */
    @DeleteMapping("/{shareId}")
    public ResponseEntity<ApiResponse<Void>> revokeShare(
            @AuthenticationPrincipal UUID userId,
            @PathVariable String shareId) {
        shareService.revokeShare(userId, shareId);
        return ResponseEntity.ok(ApiResponse.success("Share link revoked successfully"));
    }

    /**
     * Public endpoint to resolve a share token for recipients.
     */
    @GetMapping("/token/{token}")
    public ResponseEntity<ApiResponse<ShareResponse>> getShareByToken(
            @PathVariable String token,
            HttpServletRequest request) {
        String baseUrl = extractBaseUrl(request);
        ShareResponse response = shareService.getShareByToken(token, baseUrl);
        return ResponseEntity.ok(ApiResponse.success("Share resolved successfully", response));
    }

    private String extractBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        if ((scheme.equals("http") && serverPort == 80) || (scheme.equals("https") && serverPort == 443)) {
            return scheme + "://" + serverName;
        }
        return scheme + "://" + serverName + ":" + serverPort;
    }
}
