package com.securefileshare.file.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securefileshare.common.constant.SecurityConstants;
import com.securefileshare.common.dto.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;

/**
 * Spring Security filter executed once per incoming HTTP request.
 * WHAT: Intercepts HTTP requests, validates the Bearer JWT, and injects caller UUID into SecurityContextHolder.
 * WHY: Provides authenticated caller identification for file operations (upload, list, retrieve package).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenValidator jwtTokenValidator;
    private final ObjectMapper objectMapper;

    public JwtAuthenticationFilter(JwtTokenValidator jwtTokenValidator, ObjectMapper objectMapper) {
        this.jwtTokenValidator = jwtTokenValidator;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader(SecurityConstants.AUTH_HEADER);

        // If no Authorization header or not Bearer prefix, proceed down the chain (anonymous request)
        if (authHeader == null || !authHeader.startsWith(SecurityConstants.BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Extract raw token string
        String token = authHeader.substring(SecurityConstants.BEARER_PREFIX.length()).trim();

        // Validate cryptographic signature and expiration timestamp
        if (!jwtTokenValidator.validateToken(token)) {
            sendUnauthorizedError(response, request.getRequestURI(), "Invalid or expired JWT token");
            return;
        }

        // Extract user identity and role from claims
        UUID userId = jwtTokenValidator.extractUserId(token);
        String role = jwtTokenValidator.extractRole(token);

        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(
                (role != null && role.startsWith("ROLE_")) ? role : "ROLE_" + role
        );

        // Populate SecurityContext with caller UUID as principal
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                userId,
                token,
                Collections.singletonList(authority)
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        filterChain.doFilter(request, response);
    }

    /**
     * Writes a standardized JSON ApiError response directly to the HTTP response stream.
     */
    private void sendUnauthorizedError(HttpServletResponse response, String uri, String message) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ApiError error = new ApiError(
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                message,
                uri
        );

        response.getWriter().write(objectMapper.writeValueAsString(error));
    }
}
