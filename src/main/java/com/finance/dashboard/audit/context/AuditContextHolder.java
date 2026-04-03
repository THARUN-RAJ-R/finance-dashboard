package com.finance.dashboard.audit.context;

import com.finance.dashboard.audit.model.AuditMetadata;

/**
 * Context holder for request-scoped audit metadata.
 */
public final class AuditContextHolder {

    private static final ThreadLocal<AuditMetadata> CONTEXT = new ThreadLocal<>();

    private AuditContextHolder() {}

    public static void set(AuditMetadata metadata) {
        CONTEXT.set(metadata);
    }

    public static AuditMetadata get() {
        return CONTEXT.get();
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
