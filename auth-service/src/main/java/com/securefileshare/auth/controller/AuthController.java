package com.securefileshare.auth.controller;

import com.securefileshare.auth.dto.*;
import com.securefileshare.auth.service.AuthService;
import com.securefileshare.auth.service.KeyRegistryService;
import com.securefileshare.common.constant.SecurityConstants;
import com.securefileshare.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller exposing authentication, user registration, and public key registry endpoints.
 * WHAT: Handles POST /auth/register, POST /auth/login, POST /auth/keys, GET /auth/keys/{userId}, and POST /auth/logout.
 * WHY: Serves as the primary HTTP API gateway for identity and cryptographic public-key distribution.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final KeyRegistryService keyRegistryService;

    public AuthController(AuthService authService, KeyRegistryService keyRegistryService) {
        this.authService = authService;
        this.keyRegistryService = keyRegistryService;
    }

    /**
     * Registers a new platform user account.
     * WHAT: Accepts username, email, raw password, and role.
     * WHY: Admin role cannot be self-registered; passwords are salted and hashed with BCrypt.
     *
     * @param request Validated RegisterRequest DTO
     * @return ApiResponse containing the created UserResponse
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse response = authService.register(request);
        return new ResponseEntity<>(ApiResponse.success("User registered successfully", response), HttpStatus.CREATED);
    }

    /**
     * Authenticates a user and issues a signed JWT access token.
     * WHAT: Validates credentials subject to Redis-backed rate limiting.
     * WHY: Defends against brute-force attacks and returns a stateless Bearer token.
     *
     * @param request  Validated LoginRequest DTO
     * @param servletRequest HttpServletRequest used to extract client IP for rate limiting
     * @return ApiResponse containing AuthResponse with JWT
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest) {

        String clientIp = extractClientIp(servletRequest);
        AuthResponse response = authService.login(request, clientIp);
        return ResponseEntity.ok(ApiResponse.success("Authentication successful", response));
    }

    /**
     * Registers or rotates the authenticated user's RSA-4096 and EC P-256 public keys.
     * WHAT: Validates key lengths and curves, computes SHA-256 fingerprints, and revokes prior active keys.
     * WHY: Crucial zero-knowledge step: guarantees server holds valid cryptographic public keys without ever seeing private keys.
     *
     * @param userId  UUID of the authenticated user extracted from the JWT principal
     * @param request Validated KeyRegistrationRequest DTO
     * @return ApiResponse containing KeyRegistryResponse
     */
    @PostMapping("/keys")
    public ResponseEntity<ApiResponse<KeyRegistryResponse>> registerKeys(
            @AuthenticationPrincipal UUID userId,
            @Valid @RequestBody KeyRegistrationRequest request) {

        KeyRegistryResponse response = keyRegistryService.registerKeys(userId, request);
        return new ResponseEntity<>(ApiResponse.success("Cryptographic public keys registered successfully", response), HttpStatus.CREATED);
    }

    /**
     * Retrieves the active public keys and fingerprints for a target user ID.
     * WHAT: Returns the recipient's RSA public key (for wrapping) and EC public key (for signature checks) plus SHA-256 fingerprints.
     * WHY: Defends against server key-swapping attacks: callers can verify fingerprints out-of-band before encrypting files.
     *
     * @param userId Target user UUID
     * @return ApiResponse containing KeyRegistryResponse
     */
    @GetMapping("/keys/{userId}")
    public ResponseEntity<ApiResponse<KeyRegistryResponse>> getKeys(@PathVariable UUID userId) {
        KeyRegistryResponse response = keyRegistryService.getActiveKeys(userId);
        return ResponseEntity.ok(ApiResponse.success("Public keys retrieved successfully", response));
    }

    /**
     * Logs out the user by blacklisting their active token's JTI in Redis.
     * WHAT: Revokes the token until its natural expiration.
     * WHY: Prevents token reuse if the token string was intercepted or retained.
     *
     * @param servletRequest HttpServletRequest containing the Authorization Bearer header
     * @return ApiResponse with logout confirmation message
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(HttpServletRequest servletRequest) {
        String authHeader = servletRequest.getHeader(SecurityConstants.AUTH_HEADER);
        if (authHeader != null && authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
            String token = authHeader.substring(SecurityConstants.BEARER_PREFIX.length()).trim();
            authService.logout(token);
        }
        return ResponseEntity.ok(ApiResponse.success("Logged out successfully"));
    }

    /**
     * Safely extracts the client IP address considering proxy headers (X-Forwarded-For).
     *
     * @param request HttpServletRequest instance
     * @return Normalized client IP address string
     */
    private String extractClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // First IP in the comma-separated list is the original client IP
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
