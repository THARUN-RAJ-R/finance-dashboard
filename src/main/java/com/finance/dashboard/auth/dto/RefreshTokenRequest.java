package com.finance.dashboard.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/** Request body for refreshing an access token. */
@Data
public class RefreshTokenRequest {

    @NotBlank(message = "refreshToken must not be blank")
    private String refreshToken;
}
