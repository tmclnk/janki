package com.prairiegrade.janki.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Validation error response with field-level error messages.
 */
public record ValidationErrorResponse(
    int status,
    String message,
    Map<String, String> errors,
    LocalDateTime timestamp
) {
    public ValidationErrorResponse(int status, String message, Map<String, String> errors) {
        this(status, message, errors, LocalDateTime.now());
    }
}
