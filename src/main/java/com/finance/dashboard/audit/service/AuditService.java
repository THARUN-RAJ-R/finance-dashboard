package com.finance.dashboard.audit.service;

import com.finance.dashboard.audit.context.AuditContextHolder;
import com.finance.dashboard.audit.entity.AuditLogEntity;
import com.finance.dashboard.audit.model.AuditMetadata;
import com.finance.dashboard.audit.repository.AuditLogRepository;
import com.finance.dashboard.user.entity.UserEntity;
import com.finance.dashboard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Audit logging service.
 *
 * <p>Writes are performed in a NEW transaction so that even if the parent
 * transaction rolls back, the audit record persists (best-effort audit).
 * The operation is asynchronous to avoid adding latency to the calling request.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository     userRepository;

    /**
     * Entry point for audit logging from controllers/services.
     * Captures request metadata from ThreadLocal synchronously.
     */
    public void log(String action, String entityType, UUID entityId, Map<String, Object> details) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String actorEmail = (auth != null) ? auth.getName() : "system";
        AuditMetadata metadata = AuditContextHolder.get();

        logAsync(actorEmail, action, entityType, entityId, details, metadata);
    }

    /**
     * Legacy support or forced email audit.
     */
    public void log(String actorEmail, String action, String entityType,
                    UUID entityId, Map<String, Object> details) {
        AuditMetadata metadata = AuditContextHolder.get();
        logAsync(actorEmail, action, entityType, entityId, details, metadata);
    }

    /**
     * Log an audit event asynchronously.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAsync(String actorEmail, String action, String entityType,
                         UUID entityId, Map<String, Object> details, AuditMetadata requestMetadata) {
        try {
            Optional<UserEntity> actor = userRepository.findByEmail(actorEmail);
            if (actor.isEmpty() && !"system".equals(actorEmail)) {
                log.warn("Audit log: actor '{}' not found, skipping audit entry", actorEmail);
                return;
            }

            Map<String, Object> fullMetadata = new HashMap<>();
            if (details != null) {
                fullMetadata.putAll(details);
            }

            if (requestMetadata != null) {
                fullMetadata.put("_ip", requestMetadata.getRemoteIp());
                fullMetadata.put("_ua", requestMetadata.getUserAgent());
                fullMetadata.put("_rid", requestMetadata.getRequestId());
            }

            AuditLogEntity entry = AuditLogEntity.builder()
                    .actor(actor.orElse(null)) // System or deleted user
                    .action(action)
                    .entityType(entityType)
                    .entityId(entityId)
                    .metadata(fullMetadata)
                    .build();

            auditLogRepository.save(entry);
        } catch (Exception e) {
            log.error("Failed to write audit log entry [{} {} {}]: {}",
                    actorEmail, action, entityId, e.getMessage());
        }
    }
}
