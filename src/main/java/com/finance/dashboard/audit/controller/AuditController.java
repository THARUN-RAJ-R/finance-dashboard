package com.finance.dashboard.audit.controller;

import com.finance.dashboard.audit.dto.AuditLogResponse;
import com.finance.dashboard.audit.repository.AuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Tag(name = "Audit", description = "Audit log — tracks every create, update, and delete action (ADMIN only)")
@SecurityRequirement(name = "bearerAuth")
public class AuditController {

    private final AuditLogRepository auditLogRepository;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List all audit log entries, newest first (ADMIN only)")
    public ResponseEntity<List<AuditLogResponse>> listAuditLogs(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        List<AuditLogResponse> logs = auditLogRepository.findAll(pageable)
                .stream()
                .map(log -> new AuditLogResponse(
                        log.getId(),
                        log.getActor() != null ? log.getActor().getEmail() : "system",
                        log.getAction(),
                        log.getEntityType(),
                        log.getEntityId(),
                        log.getMetadata(),
                        log.getCreatedAt()
                ))
                .toList();

        return ResponseEntity.ok(logs);
    }

    @GetMapping("/entity/{entityType}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get audit logs filtered by entity type e.g. FinancialRecord (ADMIN only)")
    public ResponseEntity<List<AuditLogResponse>> getByEntityType(
            @PathVariable String entityType,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        List<AuditLogResponse> logs = auditLogRepository.findAll(pageable)
                .stream()
                .filter(log -> entityType.equalsIgnoreCase(log.getEntityType()))
                .map(log -> new AuditLogResponse(
                        log.getId(),
                        log.getActor() != null ? log.getActor().getEmail() : "system",
                        log.getAction(),
                        log.getEntityType(),
                        log.getEntityId(),
                        log.getMetadata(),
                        log.getCreatedAt()
                ))
                .toList();

        return ResponseEntity.ok(logs);
    }

    @GetMapping("/record/{recordId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get the full audit trail for a specific record ID (ADMIN only)")
    public ResponseEntity<List<AuditLogResponse>> getByRecordId(
            @PathVariable UUID recordId) {

        List<AuditLogResponse> logs = auditLogRepository.findAll()
                .stream()
                .filter(log -> recordId.equals(log.getEntityId()))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(log -> new AuditLogResponse(
                        log.getId(),
                        log.getActor() != null ? log.getActor().getEmail() : "system",
                        log.getAction(),
                        log.getEntityType(),
                        log.getEntityId(),
                        log.getMetadata(),
                        log.getCreatedAt()
                ))
                .toList();

        return ResponseEntity.ok(logs);
    }
}
