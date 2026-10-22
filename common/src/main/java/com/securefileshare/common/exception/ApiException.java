package com.securefileshare.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base custom business and security runtime exception for the platform.
 * WHAT: Captures HTTP status code alongside the error message.
 * WHY: Enables service layers to throw explicit, typed HTTP exceptions that bubble up to the global handler.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    /**
     * Constructs an ApiException with a custom status and message.
     *
     * @param status  Target HTTP status
     * @param message Sanitized error description
     */
    public ApiException(HttpStatus status, String message) {
        super(message);
        // Explicitly record status so the controller advice translates it directly into the HTTP response code
        this.status = status;
    }

    /**
     * Constructs an ApiException with a custom status, message, and root cause.
     *
     * @param status  Target HTTP status
     * @param message Sanitized error description
     * @param cause   Root throwable cause
     */
    public ApiException(HttpStatus status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
