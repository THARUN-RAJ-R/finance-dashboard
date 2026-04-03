package com.finance.dashboard.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "JWT token response")
public class LoginResponse {

    @Schema(description = "Bearer JWT access token", example = "eyJhbGci...")
    private final String accessToken;

    @Schema(description = "Access token lifetime in seconds", example = "86400")
    private final long expiresInSeconds;

    @Schema(description = "Opaque refresh token (UUID) — use with POST /auth/refresh")
    private final String refreshToken;

    @Schema(description = "Refresh token lifetime in seconds", example = "604800")
    private final long refreshExpiresInSeconds;

    @Schema(description = "Token type", example = "Bearer")
    @Builder.Default
    private final String tokenType = "Bearer";
}
