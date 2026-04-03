package com.finance.dashboard.audit.model;

import lombok.Builder;
import lombok.Value;

/**
 * Request metadata for audit logging.
 */
@Value
@Builder
public class AuditMetadata {
    String remoteIp;
    String userAgent;
    String requestId;
}
