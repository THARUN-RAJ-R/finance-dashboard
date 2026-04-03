package com.finance.dashboard.auth.service;

import com.finance.dashboard.common.exception.ApiException;
import com.finance.dashboard.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Manages refresh tokens stored in Redis.
 *
 * <p>Key format: {@code refresh:{token_uuid}} → user email
 * <p>TTL is configurable via {@code app.jwt.refresh-expiration-seconds} (default 7 days).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final String KEY_PREFIX = "refresh:";

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${app.jwt.refresh-expiration-seconds:604800}")
    private long refreshExpirationSeconds;

    /** Generate a new refresh token UUID, store it in Redis, and return the token string. */
    public String createRefreshToken(UserEntity user) {
        String token = UUID.randomUUID().toString();
        String key   = KEY_PREFIX + token;
        redisTemplate.opsForValue().set(key, user.getEmail(), refreshExpirationSeconds, TimeUnit.SECONDS);
        log.debug("Refresh token created for user: {}", user.getEmail());
        return token;
    }

    /**
     * Validate refresh token and return the associated user email.
     * Rotates the token (delete old, issue new not done here — caller re-creates).
     *
     * @throws ApiException 401 if token is missing or expired
     */
    public String validateAndGetEmail(String token) {
        String key   = KEY_PREFIX + token;
        String email = redisTemplate.opsForValue().get(key);
        if (email == null) {
            throw ApiException.unauthorized("Refresh token is invalid or has expired");
        }
        // Rotate: delete used token to prevent reuse
        redisTemplate.delete(key);
        return email;
    }

    /** Revoke a specific refresh token (e.g., on logout). */
    public void revoke(String token) {
        redisTemplate.delete(KEY_PREFIX + token);
    }
}
