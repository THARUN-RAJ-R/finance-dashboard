package com.finance.dashboard.user.dto;

import com.finance.dashboard.user.entity.RoleName;
import com.finance.dashboard.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Partial update: all fields optional — only non-null fields are applied.
 */
@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Partial update for a user (status and/or roles)")
public class UpdateUserRequest {

    @Schema(description = "New account status", example = "INACTIVE")
    private UserStatus status;

    @Schema(description = "Complete replacement of user's roles", example = "[\"VIEWER\"]")
    private Set<RoleName> roles;
}
