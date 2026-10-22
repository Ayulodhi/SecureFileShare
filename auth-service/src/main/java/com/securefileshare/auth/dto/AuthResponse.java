package com.securefileshare.auth.dto;

import java.util.UUID;

/**
 * Data Transfer Object returned upon successful authentication.
 * WHAT: Conveys the signed JWT access token, token type, expiry in seconds, and user identity summary.
 * WHY: Enables clients to attach Bearer tokens to subsequent authenticated calls.
 */
public class AuthResponse {

    private String accessToken;
    private String tokenType;
    private long expiresInSeconds;
    private UUID userId;
    private String username;
    private String role;

    public AuthResponse() {
        this.tokenType = "Bearer";
    }

    public AuthResponse(String accessToken, long expiresInSeconds, UUID userId, String username, String role) {
        this.accessToken = accessToken;
        this.tokenType = "Bearer";
        this.expiresInSeconds = expiresInSeconds;
        this.userId = userId;
        this.username = username;
        this.role = role;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(long expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
