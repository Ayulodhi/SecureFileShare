package com.securefileshare.common.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * Standard API error payload returned by all microservices.
 * WHAT: Encapsulates standardized HTTP error details, timestamp, message, request URI, and field-level validation errors.
 * WHY: Provides clients with consistent, structured error envelopes and prevents the exposure of raw stack traces or internal SQL exceptions.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    /**
     * Exact UTC timestamp when the error occurred.
     * Formatted in ISO-8601 for interoperability across client platforms.
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private Instant timestamp;

    /**
     * HTTP status code (e.g., 400, 401, 403, 404, 500).
     */
    private int status;

    /**
     * Canonical HTTP status reason phrase (e.g., "Bad Request", "Unauthorized").
     */
    private String error;

    /**
     * Human-readable, sanitized message explaining the failure reason.
     */
    private String message;

    /**
     * The requested URL path that triggered this error response.
     */
    private String path;

    /**
     * Optional collection of field-level validation errors for form/DTO validation failures.
     */
    private List<ValidationError> validationErrors;

    /**
     * Default no-argument constructor required for Jackson deserialization.
     */
    public ApiError() {
        this.timestamp = Instant.now();
    }

    /**
     * Parameterized constructor to initialize core error details.
     *
     * @param status  HTTP status code
     * @param error   HTTP error description phrase
     * @param message Sanitized error message
     * @param path    Requested URI path
     */
    public ApiError(int status, String error, String message, String path) {
        this.timestamp = Instant.now();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }

    /**
     * Parameterized constructor including field-level validation errors.
     *
     * @param status           HTTP status code
     * @param error            HTTP error description phrase
     * @param message          Sanitized error message
     * @param path             Requested URI path
     * @param validationErrors List of specific field validation errors
     */
    public ApiError(int status, String error, String message, String path, List<ValidationError> validationErrors) {
        this(status, error, message, path);
        this.validationErrors = validationErrors;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }

    public void setValidationErrors(List<ValidationError> validationErrors) {
        this.validationErrors = validationErrors;
    }

    /**
     * Represents a single field-level validation constraint violation.
     * WHAT: Captures the rejected field name and the constraint violation message.
     * WHY: Informs clients precisely which input parameter failed validation without exposing backend internals.
     */
    public static class ValidationError {
        private String field;
        private String message;

        public ValidationError() {}

        public ValidationError(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
