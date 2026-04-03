package com.finance.dashboard.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Base application exception that carries an HTTP status code and a
 * machine-readable error key alongside the human-readable message.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String     errorCode;

    public ApiException(HttpStatus status, String errorCode, String message) {
        super(message);
        this.status    = status;
        this.errorCode = errorCode;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    // ── convenience factories ──────────────────────────────────

    public static ApiException notFound(String entity, Object id) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                "NOT_FOUND",
                entity + " not found with id: " + id
        );
    }

    public static ApiException conflict(String message) {
        return new ApiException(HttpStatus.CONFLICT, "CONFLICT", message);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "BAD_REQUEST", message);
    }

    public static ApiException forbidden(String message) {
        return new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", message);
    }
}
