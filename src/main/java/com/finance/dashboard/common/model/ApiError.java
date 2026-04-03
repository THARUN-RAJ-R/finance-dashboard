package com.finance.dashboard.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

/**
 * Standardised error envelope returned by the GlobalExceptionHandler.
 *
 * <pre>
 * {
 *   "timestamp": "2024-01-01T00:00:00Z",
 *   "status": 400,
 *   "error": "BAD_REQUEST",
 *   "message": "Validation failed",
 *   "path": "/api/v1/records",
 *   "details": [ { "field": "amount", "message": "must be greater than 0" } ]
 * }
 * </pre>
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private final Instant      timestamp;
    private final int          status;
    private final String       error;
    private final String       message;
    private final String       path;
    private final List<String> details;

}
