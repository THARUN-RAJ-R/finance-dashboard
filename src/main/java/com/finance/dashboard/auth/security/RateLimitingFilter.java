package com.finance.dashboard.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.dashboard.common.model.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

/**
 * Redis-based Rate-limiting filter for {@code POST /api/v1/auth/login}.
 *
 * <p>Uses Redis INCR with EXPIRE (fixed window) keyed by client IP.
 * Default: 10 requests per 60 seconds per IP.
 */
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final String LOGIN_PATH = "/api/v1/auth/login";
    private static final String REDIS_KEY_PREFIX = "rate-limit:login:";

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.rate-limit.login-capacity:10}")
    private long loginCapacity;

    @Value("${app.rate-limit.login-refill-seconds:60}")
    private long loginRefillSeconds;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest  request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain         chain
    ) throws ServletException, IOException {

        if (!request.getRequestURI().contains(LOGIN_PATH)
                || !"POST".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        String clientIp = resolveClientIp(request);
        String redisKey = REDIS_KEY_PREFIX + clientIp;

        // Atomic increment
        Long count = redisTemplate.opsForValue().increment(redisKey);

        if (count != null && count == 1) {
            // First request in this window, set expiration
            redisTemplate.expire(redisKey, loginRefillSeconds, TimeUnit.SECONDS);
        }

        if (count != null && count > loginCapacity) {
            writeTooManyRequests(response, request.getRequestURI());
            return;
        }

        chain.doFilter(request, response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void writeTooManyRequests(HttpServletResponse response, String path) throws IOException {
        ApiError error = ApiError.builder()
                .timestamp(Instant.now())
                .status(429)
                .error("TOO_MANY_REQUESTS")
                .message("Too many login attempts. Please wait before trying again.")
                .path(path)
                .build();

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), error);
    }
}
