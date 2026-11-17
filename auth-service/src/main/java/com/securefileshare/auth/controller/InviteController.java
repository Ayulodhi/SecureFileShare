package com.securefileshare.auth.controller;

import com.securefileshare.common.dto.ApiResponse;
import com.securefileshare.auth.entity.Invite;
import com.securefileshare.auth.service.InviteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for invite links and codes (Task 4).
 */
@RestController
@RequestMapping("/invites")
public class InviteController {

    private final InviteService inviteService;

    public InviteController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Invite>> createInvite(
            @AuthenticationPrincipal UUID userId,
            @RequestParam(defaultValue = "1") int maxUses,
            @RequestParam(defaultValue = "7") int validityDays) {
        Invite invite = inviteService.createInvite(userId, maxUses, validityDays);
        return new ResponseEntity<>(ApiResponse.success("Invite created successfully", invite), HttpStatus.CREATED);
    }

    @GetMapping("/validate/{code}")
    public ResponseEntity<ApiResponse<Void>> validateInvite(@PathVariable String code) {
        inviteService.validateAndGetInvite(code);
        return ResponseEntity.ok(ApiResponse.success("Invite code is valid"));
    }

    @DeleteMapping("/{inviteId}")
    public ResponseEntity<ApiResponse<Void>> revokeInvite(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID inviteId) {
        inviteService.revokeInvite(userId, inviteId);
        return ResponseEntity.ok(ApiResponse.success("Invite revoked successfully"));
    }
}
