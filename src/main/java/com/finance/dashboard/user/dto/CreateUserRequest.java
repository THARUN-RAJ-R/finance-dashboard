package com.finance.dashboard.user.dto;

import com.finance.dashboard.user.entity.RoleName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request to create a new user account")
public class CreateUserRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Must be a valid email address")
    @Schema(description = "User's unique email address", example = "newuser@finance.com")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    @Schema(description = "Account password (min 8 characters)", example = "SecurePassword123!")
    private String password;

    @NotEmpty(message = "At least one role is required")
    @Schema(description = "Set of roles to assign (VIEWER, ANALYST, ADMIN)", example = "[\"ANALYST\"]")
    private Set<RoleName> roles;
}
