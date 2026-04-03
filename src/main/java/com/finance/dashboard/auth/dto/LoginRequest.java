package com.finance.dashboard.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "Credentials for user authentication")
public class LoginRequest {

    @NotBlank(message = "Email is required for authentication")
    @Email(message = "Must be a valid email address")
    @Pattern(regexp = "^[A-Za-z0-9+_.-]+@(.+)\\.[A-Za-z]{2,}$", message = "Must be a valid email address with a proper domain")
    @Schema(description = "Registered email address", example = "admin@finance.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(description = "User password", example = "password")
    private String password;
}
