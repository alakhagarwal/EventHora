package com.eventHora.backend.exception;

import java.time.LocalDateTime;

/**
 * Standard error body returned for every exception.
 *
 * {
 *   "status":  409,
 *   "error":   "Conflict",
 *   "message": "A user with this email already exists",
 *   "timestamp": "2026-07-05T16:00:00"
 * }
 */
public record ErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp
) {
    /** Convenience factory */
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, LocalDateTime.now());
    }
}
