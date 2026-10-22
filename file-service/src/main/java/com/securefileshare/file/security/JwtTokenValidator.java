package com.securefileshare.file.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/**
 * Service for validating cryptographic JSON Web Tokens (JWT) in file-service.
 * WHAT: Validates HMAC-SHA256 signatures, token expiration, and parses userId and role claims.
 * WHY: Provides stateless caller identification and authorization across file operations without session state.
 */
@Service
public class JwtTokenValidator {

    private final SecretKey signingKey;

    public JwtTokenValidator(@Value("${security.jwt.secret}") String secret) {
        // Enforce presence of JWT secret key from environment configuration
        // Why: Prevents running without an authenticated cryptographic secret
        if (secret == null || secret.isBlank()) {
            throw new IllegalArgumentException("Missing required JWT signing secret. Provide JWT_SECRET environment variable.");
        }

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret key must be at least 32 characters (256 bits) long. Current length: " + keyBytes.length);
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Validates the cryptographic signature and expiration of a JWT string.
     *
     * @param token Compact serialized JWT string
     * @return true if token is cryptographically authentic and not expired
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(signingKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Extracts the user UUID stored in the token's subject claim.
     *
     * @param token Validated JWT string
     * @return Caller's UUID
     */
    public UUID extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        return UUID.fromString(claims.getSubject());
    }

    /**
     * Extracts the user role name claim from the JWT.
     *
     * @param token Validated JWT string
     * @return User role string
     */
    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }

    /**
     * Parses and returns the entire signed claims payload.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
