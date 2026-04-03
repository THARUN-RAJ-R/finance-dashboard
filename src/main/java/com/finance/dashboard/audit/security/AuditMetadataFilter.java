package com.finance.dashboard.audit.security;

import com.finance.dashboard.audit.context.AuditContextHolder;
import com.finance.dashboard.audit.model.AuditMetadata;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter to capture request metadata for audit logging.
 */
@Component
public class AuditMetadataFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String remoteIp = request.getHeader("X-Forwarded-For");
            if (remoteIp == null || remoteIp.isEmpty()) {
                remoteIp = request.getRemoteAddr();
            }

            String userAgent = request.getHeader("User-Agent");
            String requestId = request.getHeader("X-Request-ID");
            if (requestId == null || requestId.isEmpty()) {
                requestId = UUID.randomUUID().toString();
            }

            AuditMetadata metadata = AuditMetadata.builder()
                    .remoteIp(remoteIp)
                    .userAgent(userAgent)
                    .requestId(requestId)
                    .build();

            AuditContextHolder.set(metadata);
            response.setHeader("X-Request-ID", requestId);

            filterChain.doFilter(request, response);
        } finally {
            AuditContextHolder.clear();
        }
    }
}
