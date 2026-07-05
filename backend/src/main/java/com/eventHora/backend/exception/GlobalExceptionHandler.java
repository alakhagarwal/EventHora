package com.eventHora.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Single place that catches every exception thrown anywhere in the application
 * and maps it to a clean, consistent JSON error response.
 *
 * Order of handlers:
 *  1. Our own custom exceptions (specific)
 *  2. Spring validation errors
 *  3. Spring Security exceptions
 *  4. Generic fallback
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ─── 1. Custom application exceptions ──────────────────────────────────────

    /**
     * 404 – resource (user, event, ticket, etc.) not found.
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(ResourceNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(404, "Not Found", ex.getMessage()));
    }

    /**
     * 409 – duplicate resource (e.g. email already registered).
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(DuplicateResourceException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(409, "Conflict", ex.getMessage()));
    }

    /**
     * 401 – bad login credentials.
     */
    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(InvalidCredentialsException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(401, "Unauthorized", ex.getMessage()));
    }

    // ─── 2. Bean Validation (@Valid) failures ──────────────────────────────────

    /**
     * 400 – request body failed @NotBlank / @Email / @NotNull validation.
     * Collects all field errors into one readable message.
     *
     * Example: "email: Must be a valid email address; password: Password is required"
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(400, "Validation Failed", message));
    }

    // ─── 3. Spring Security exceptions ────────────────────────────────────────

    /**
     * 401 – JWT missing, expired, or invalid (thrown by Spring Security filter chain).
     */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(401, "Unauthorized", "Authentication required: " + ex.getMessage()));
    }

    /**
     * 403 – valid JWT but insufficient role (STAFF hitting ADMIN-only route).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(403, "Forbidden", "You do not have permission to perform this action"));
    }

    // ─── 4. Catch-all fallback ─────────────────────────────────────────────────

    /**
     * 500 – any unhandled exception.
     * Hides internal details from the client; logs the real cause on the server.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex) {
        // In production, log ex.getMessage() to your monitoring tool here
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(500, "Internal Server Error", "An unexpected error occurred"));
    }
}
