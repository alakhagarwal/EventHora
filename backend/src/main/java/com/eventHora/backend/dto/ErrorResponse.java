package com.eventHora.backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Standardised error response returned for ALL exceptions.
 * Every error from the API will look like this:
 *
 * {
 *   "status": 404,
 *   "error": "Not Found",
 *   "message": "User not found: someone@example.com",
 *   "path": "/api/auth/users/someone@example.com/deactivate",
 *   "timestamp": "2026-07-05T15:30:00",
 *   "fieldErrors": null           // only present on validation failures
 * }
 */
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)   // hides null fields from response
public class ErrorResponse {

    private int status;
    private String error;
    private String message;
    private String path;
    private LocalDateTime timestamp;

    /** Only present when request body validation fails — lists each invalid field */
    private List<FieldError> fieldErrors;

    @Data
    @Builder
    public static class FieldError {
        private String field;
        private String message;
    }
}
