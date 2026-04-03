package com.finance.dashboard.auth.service;

import com.finance.dashboard.auth.dto.LoginRequest;
import com.finance.dashboard.auth.dto.LoginResponse;
import com.finance.dashboard.auth.security.JwtService;
import com.finance.dashboard.common.exception.ApiException;
import com.finance.dashboard.user.entity.UserEntity;
import com.finance.dashboard.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Authentication business logic.
 * Issues both a short-lived JWT access token and a long-lived opaque refresh token.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository      userRepository;
    private final PasswordEncoder     passwordEncoder;
    private final JwtService          jwtService;
    private final RefreshTokenService refreshTokenService;

    @Value("${app.jwt.refresh-expiration-seconds:604800}")
    private long refreshExpirationSeconds;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        UserEntity user = userRepository.findByEmailWithRoles(request.getEmail())
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw ApiException.unauthorized("Invalid email or password");
        }

        if (!user.isActive()) {
            throw ApiException.unauthorized("Account is disabled. Please contact an administrator.");
        }

        String accessToken  = jwtService.generateToken(user);
        String refreshToken = refreshTokenService.createRefreshToken(user);

        log.info("User {} logged in successfully", user.getEmail());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .expiresInSeconds(jwtService.getExpirationSeconds())
                .refreshToken(refreshToken)
                .refreshExpiresInSeconds(refreshExpirationSeconds)
                .build();
    }

    /** Exchange a valid refresh token for a new access token (token is rotated). */
    @Transactional(readOnly = true)
    public LoginResponse refresh(String rawRefreshToken) {
        String email = refreshTokenService.validateAndGetEmail(rawRefreshToken);

        UserEntity user = userRepository.findByEmailWithRoles(email)
                .orElseThrow(() -> ApiException.unauthorized("User not found"));

        if (!user.isActive()) {
            throw ApiException.unauthorized("Account is disabled");
        }

        String newAccessToken  = jwtService.generateToken(user);
        String newRefreshToken = refreshTokenService.createRefreshToken(user);

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .expiresInSeconds(jwtService.getExpirationSeconds())
                .refreshToken(newRefreshToken)
                .refreshExpiresInSeconds(refreshExpirationSeconds)
                .build();
    }
}
