package com.securefileshare.auth.controller;

import com.securefileshare.common.dto.ApiResponse;
import com.securefileshare.auth.service.FriendsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for friend requests and friendships.
 */
@RestController
@RequestMapping("/friends")
public class FriendsController {

    private final FriendsService friendsService;

    public FriendsController(FriendsService friendsService) {
        this.friendsService = friendsService;
    }

    @PostMapping("/requests")
    public ResponseEntity<ApiResponse<Void>> sendFriendRequest(
            @AuthenticationPrincipal UUID userId,
            @RequestParam String email) {
        friendsService.sendFriendRequest(userId, email);
        return ResponseEntity.ok(ApiResponse.success("Friend request processed successfully"));
    }

    @PostMapping("/requests/{requestId}/accept")
    public ResponseEntity<ApiResponse<Void>> acceptFriendRequest(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID requestId) {
        friendsService.acceptFriendRequest(userId, requestId);
        return ResponseEntity.ok(ApiResponse.success("Friend request accepted successfully"));
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<ApiResponse<Void>> unfriend(
            @AuthenticationPrincipal UUID userId,
            @PathVariable UUID friendId) {
        friendsService.unfriend(userId, friendId);
        return ResponseEntity.ok(ApiResponse.success("Successfully unfriended user"));
    }
}
