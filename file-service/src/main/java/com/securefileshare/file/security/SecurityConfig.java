package com.securefileshare.file.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securefileshare.common.dto.ApiError;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Spring Security configuration for file-service.
 * WHAT: Configures stateless JWT authentication, public health routes, and installs JwtAuthenticationFilter.
 * WHY: Enforces strict access control on file upload, package retrieval, and recipient key management.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final ObjectMapper objectMapper;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter, ObjectMapper objectMapper) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.objectMapper = objectMapper;
    }

    /**
     * Configures the HTTP security filter chain and authorization rules.
     *
     * @param http HttpSecurity configuration builder
     * @return Built SecurityFilterChain
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Disable CSRF because authentication is stateless and uses Bearer tokens stored in client headers (no cookies)
                .csrf(csrf -> csrf.disable())

                // Enforce stateless session policy: no HTTP sessions created or persisted
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Configure endpoint authorization rules
                .authorizeHttpRequests(auth -> auth
                        // Permit asynchronous dispatches so StreamingResponseBody flushes without re-authorization
                        .dispatcherTypeMatchers(DispatcherType.ASYNC).permitAll()
                        // Health checks and actuator metrics are public
                        .requestMatchers("/health", "/api/v1/health", "/actuator/**").permitAll()
                        // All file operations require an authenticated JWT Bearer token
                        .anyRequest().authenticated()
                )

                // Custom JSON error response for unauthenticated access attempts
                .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                    ApiError error = new ApiError(
                            HttpStatus.UNAUTHORIZED.value(),
                            HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                            "Full authentication is required to access this resource",
                            request.getRequestURI()
                    );

                    response.getWriter().write(objectMapper.writeValueAsString(error));
                }))

                // Install JWT authentication filter before the standard filter chain
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
