package com.securefileshare.file.exception;

import com.securefileshare.common.dto.ApiError;
import com.securefileshare.common.exception.GlobalExceptionHandlerBase;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

/**
 * Controller advice specializing exception handling for file-service.
 * WHAT: Extends GlobalExceptionHandlerBase and handles file-specific upload and storage exceptions.
 * WHY: Normalizes multipart file upload errors (e.g., size exceeded) into uniform ApiError responses.
 */
@RestControllerAdvice
public class FileExceptionHandler extends GlobalExceptionHandlerBase {

    private static final Logger log = LoggerFactory.getLogger(FileExceptionHandler.class);

    /**
     * Handles multipart file uploads exceeding the configured maximum size.
     * WHAT: Intercepts MaxUploadSizeExceededException thrown by Spring Servlet container.
     * WHY: Returns client-friendly 400 Bad Request explaining that the file payload exceeded upload limits.
     *
     * @param ex      MaxUploadSizeExceededException instance
     * @param request HttpServletRequest instance
     * @return ResponseEntity containing ApiError
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        log.warn("Upload size exceeded at {}: {}", request.getRequestURI(), ex.getMessage());

        ApiError error = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Uploaded file exceeds the maximum allowable upload limit",
                request.getRequestURI()
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles illegal argument exceptions.
     * WHAT: Translates IllegalArgumentException into 400 Bad Request.
     * WHY: Provides uniform handling for input sanity failures.
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException ex, HttpServletRequest request) {
        log.warn("Illegal argument provided at {}: {}", request.getRequestURI(), ex.getMessage());

        ApiError error = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                ex.getMessage(),
                request.getRequestURI()
        );

        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }
}
