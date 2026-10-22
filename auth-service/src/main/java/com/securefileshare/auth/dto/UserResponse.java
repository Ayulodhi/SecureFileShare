package com.securefileshare.auth.dto;

import com.securefileshare.auth.entity.Role;

import java.time.Instant;
import java.util.UUID;

/**
 * Data Transfer Object representing public user profile information.
 * WHAT: Exposes user ID, username, email, role, and registration date.
 * WHY: Never exposes password hash or internal database internals.
 */
public class UserResponse {

    private UUID id;
    private String username;
    private String email;
    private Role role;
    private Instant createdAt;

    public UserResponse() {}

    public UserResponse(UUID id, String username, String email, Role role, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
