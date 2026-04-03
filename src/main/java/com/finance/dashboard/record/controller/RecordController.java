package com.finance.dashboard.record.controller;

import com.finance.dashboard.common.model.ApiError;
import com.finance.dashboard.common.model.PageResponse;
import com.finance.dashboard.record.dto.*;
import com.finance.dashboard.record.service.RecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/records")
@RequiredArgsConstructor
@Validated
@Tag(name = "Records", description = "Financial record management")
@SecurityRequirement(name = "bearerAuth")
public class RecordController {

    private final RecordService recordService;

    // ── Create ─────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Create a financial record (ADMIN). Supports Idempotency-Key header.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Record created"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<RecordResponse> createRecord(
            @Valid @RequestBody CreateRecordRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false)
            @Parameter(description = "Optional UUID for idempotent record creation") String idempotencyKey) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recordService.createRecord(request, idempotencyKey));
    }

    // ── Bulk Create ────────────────────────────────────────────

    @PostMapping("/bulk")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bulk create financial records — atomic: all or nothing (ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "All records created"),
            @ApiResponse(responseCode = "400", description = "Validation error in one or more items",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<BulkCreateResponse> bulkCreate(
            @RequestBody @Size(max = 50, message = "Bulk creation must not exceed 50 items")
            List<@Valid CreateRecordRequest> items) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recordService.bulkCreate(items));
    }

    // ── List (filtered + paged) ────────────────────────────────

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(summary = "List financial records with optional filtering (ADMIN, ANALYST)")
    public ResponseEntity<PageResponse<RecordResponse>> listRecords(
            @ModelAttribute RecordFilterParams filter) {
        return ResponseEntity.ok(recordService.listRecords(filter));
    }

    // ── CSV Export ─────────────────────────────────────────────

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(summary = "Export records as CSV download — same filters as GET /records (ADMIN, ANALYST)")
    public void exportCsv(
            @ModelAttribute RecordFilterParams filter,
            HttpServletResponse response) throws IOException {
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"records-export.csv\"");
        recordService.exportToCsv(filter, response.getWriter());
    }

    // ── Get by ID ──────────────────────────────────────────────

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(summary = "Get a financial record by ID — returns ETag header (ADMIN, ANALYST)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Found"),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<RecordResponse> getRecord(@PathVariable UUID id) {
        RecordResponse record = recordService.getRecord(id);
        return ResponseEntity.ok()
                .eTag(String.valueOf(record.getVersion()))
                .body(record);
    }

    // ── Update (PUT) ───────────────────────────────────────────

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Fully update a financial record. Requires If-Match header or version in body (ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "404", description = "Not found or deleted",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Version mismatch (optimistic lock)",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<RecordResponse> updateRecord(
            @PathVariable UUID id,
            @RequestHeader(value = "If-Match", required = false)
            @Parameter(description = "ETag version — include to enable optimistic locking") String ifMatch,
            @Valid @RequestBody UpdateRecordRequest request) {
        Long version = parseVersion(ifMatch);
        if (version == null) version = request.getVersion();
        return ResponseEntity.ok(recordService.updateRecord(id, request, version));
    }

    // ── Delete (soft) ──────────────────────────────────────────

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Soft-delete a financial record. Requires If-Match header for safety (ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Deleted"),
            @ApiResponse(responseCode = "404", description = "Not found",
                    content = @Content(schema = @Schema(implementation = ApiError.class))),
            @ApiResponse(responseCode = "409", description = "Version mismatch",
                    content = @Content(schema = @Schema(implementation = ApiError.class)))
    })
    public ResponseEntity<Void> deleteRecord(
            @PathVariable UUID id,
            @RequestHeader(value = "If-Match", required = false)
            @Parameter(description = "ETag version — include to enable optimistic locking") String ifMatch) {
        recordService.deleteRecord(id, parseVersion(ifMatch));
        return ResponseEntity.noContent().build();
    }

    // ── Helpers ────────────────────────────────────────────────

    /** Parse If-Match header like {@code "3"} or {@code W/"3"} to a Long. Returns null if absent. */
    private Long parseVersion(String ifMatch) {
        if (ifMatch == null || ifMatch.isBlank()) return null;
        String clean = ifMatch.replace("\"", "").replace("W/", "").trim();
        try {
            return Long.parseLong(clean);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
