package com.securefileshare.common.exception;

import com.securefileshare.common.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Abstract global exception handler base class for Spring Boot microservices.
 * WHAT: Catches standard web, validation, custom business, and unexpected runtime exceptions.
 * WHY: Normalizes all error outcomes into uniform ApiError responses across all services,
 *      preventing leakage of stack traces, internal database schema names, and SQL injection clues.
 */
public abstract class GlobalExceptionHandlerBase {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandlerBase.class);

    /**
     * Handles custom business and cryptographic API exceptions.
     * WHAT: Translates ApiException into structured ApiError response.
     * WHY: Returns client-friendly error descriptions with explicit HTTP status codes set by domain logic.
     */
    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiError> handleApiException(ApiException ex, HttpServletRequest request) {
        // Log at WARN level rather than ERROR because business rejections are expected operational events
        log.warn("API Exception occurred at {}: {}", request.getRequestURI(), ex.getMessage());

        ApiError error = new ApiError(
                ex.getStatus().value(),
                ex.getStatus().getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );

        // Explicitly assert non-null HttpStatus to satisfy ResponseEntity @NonNull HttpStatusCode contract
        return new ResponseEntity<>(error, Objects.requireNonNull(ex.getStatus()));
    }

    /**
     * Handles Bean Validation constraint failures triggered by @Valid annotations.
     * WHAT: Extracts field-level validation constraint violations into ApiError.ValidationError objects.
     * WHY: Provides granular feedback to the client regarding invalid inputs without exposing server internals.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ApiError.ValidationError> validationErrors = new ArrayList<>();

        // Iterate over all field errors to build a clean list of constraint violations
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            validationErrors.add(new ApiError.ValidationError(
                    fieldError.getField(),
                    fieldError.getDefaultMessage()
            ));
        }

        // Why: 400 Bad Request clearly signals client-side input contract violation
        ApiError error = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Input validation failed. Please check the rejected fields.",
                request.getRequestURI(),
                validationErrors
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles malformed JSON and unparseable HTTP request bodies.
     * WHAT: Intercepts JSON deserialization failures before they trigger raw framework stack traces.
     * WHY: Prevents attackers from probing Jackson deserialization parser internals via malformed payloads.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleNotReadableException(HttpMessageNotReadableException ex, HttpServletRequest request) {
        // Why: Prevents detailed Jackson parser error details from leaking internal class structures
        log.warn("Malformed JSON request received at {}", request.getRequestURI());

        ApiError error = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Malformed JSON request body or invalid data type",
                request.getRequestURI()
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Fallback handler for all unexpected runtime and unchecked exceptions.
     * WHAT: Catches generic Throwable/Exception and returns a sanitized 500 Internal Server Error.
     * WHY: CRITICAL SECURITY BARRIER - Masks raw SQL exceptions, class names, file paths, and stack traces
     *      to defend against reconnaissance and information disclosure attacks.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneralException(Exception ex, HttpServletRequest request) {
        // Log the actual full stack trace internally for developer debugging, but NEVER return it to the caller
        log.error("Unhandled server exception at {}: ", request.getRequestURI(), ex);

        // Security decision: return a sanitized generic error message to the client
        // Why: Prevents information disclosure (CWE-209) that attackers use to map server technologies
        ApiError error = new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected internal error occurred. Please contact system support.",
                request.getRequestURI()
        );

        return new ResponseEntity<>(error, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
