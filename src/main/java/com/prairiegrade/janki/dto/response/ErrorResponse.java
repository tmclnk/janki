package com.prairiegrade.janki.dto.response;

import java.time.LocalDateTime;

/**
 * Standard error response for REST API.
 */
public record ErrorResponse(
    int status,
    String message,
    String path,
    LocalDateTime timestamp
) {
    public ErrorResponse(int status, String message, String path) {
        this(status, message, path, LocalDateTime.now());
    }
}
