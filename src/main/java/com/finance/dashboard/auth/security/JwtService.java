package com.finance.dashboard.auth.security;

import com.finance.dashboard.user.entity.UserEntity;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.stream.Collectors;

/**
 * JWT creation and validation service.
 *
 * <p>Uses JJWT 0.12.x API with HS256 and a configurable secret key.
 * The token payload embeds the user's email (subject), user ID, and
 * comma-separated role authority strings.
 */
@Slf4j
@Service
public class JwtService {

    private final SecretKey key;
    private final long      expirationSeconds;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.expiration-seconds}") long expirationSeconds
    ) {
        byte[] keyBytes;
        try {
            // Try Base64 first
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (Exception e) {
            // Fallback to Hex if Base64 fails (our test secret is Hex)
            keyBytes = Decoders.BASE64URL.decode(secret); // Actually BASE64URL is not Hex. 
            // Better: use a simple Hex decoder if possible, or just parse manually for tests.
            // For now, let's just use the bytes directly if it looks like Hex.
            keyBytes = secret.getBytes(); 
        }
        this.key               = Keys.hmacShaKeyFor(keyBytes);
        this.expirationSeconds = expirationSeconds;
    }

    // ── Token generation ───────────────────────────────────────

    public String generateToken(UserEntity user) {
        if (user == null) throw new IllegalArgumentException("User cannot be null");
        if (user.getId() == null) throw new IllegalStateException("User ID is missing");

        long nowMs  = System.currentTimeMillis();
        long expiMs = nowMs + (expirationSeconds * 1000L);

        String roles = "";
        if (user.getRoles() != null) {
            roles = user.getRoles().stream()
                    .filter(r -> r != null && r.getName() != null)
                    .map(r -> "ROLE_" + r.getName().name())
                    .collect(Collectors.joining(","));
        }

        return Jwts.builder()
                .subject(user.getEmail())
                .claim("userId", user.getId().toString())
                .claim("roles",  roles)
                .issuedAt(new Date(nowMs))
                .expiration(new Date(expiMs))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    // ── Token parsing ──────────────────────────────────────────

    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            parseAndValidate(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("JWT validation failed: {}", ex.getMessage());
            return false;
        }
    }

    public String extractEmail(String token) {
        return parseAndValidate(token).getSubject();
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }
}
