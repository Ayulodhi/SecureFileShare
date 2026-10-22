package com.securefileshare.auth.exception;

import com.securefileshare.common.exception.GlobalExceptionHandlerBase;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Controller advice for auth-service extending GlobalExceptionHandlerBase.
 * WHAT: Catches and formats all exceptions occurring in auth-service controllers.
 * WHY: Inherits standardized error envelopes and sanitization logic from the common module.
 */
@RestControllerAdvice
public class AuthExceptionHandler extends GlobalExceptionHandlerBase {
    // Inherits standard ApiException, MethodArgumentNotValidException, and fallback handling
}
