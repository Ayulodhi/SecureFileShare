package com.securefileshare.share.exception;

import com.securefileshare.common.exception.GlobalExceptionHandlerBase;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Controller advice specializing exception handling for share-service.
 * WHAT: Extends GlobalExceptionHandlerBase.
 * WHY: Normalizes share-service exceptions into standard ApiError responses.
 */
@RestControllerAdvice
public class ShareExceptionHandler extends GlobalExceptionHandlerBase {
}
