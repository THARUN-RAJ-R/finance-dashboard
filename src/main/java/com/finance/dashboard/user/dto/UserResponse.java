package com.finance.dashboard.user.dto;

import com.finance.dashboard.user.entity.RoleName;
import com.finance.dashboard.user.entity.UserEntity;
import com.finance.dashboard.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Builder
@Schema(description = "User details response")
public class UserResponse {

    private final UUID      id;
    private final String    email;
    private final UserStatus status;
    private final Set<RoleName> roles;
    private final Instant   createdAt;
    private final Instant   updatedAt;

    public static UserResponse from(UserEntity user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .status(user.getStatus())
                .roles(user.getRoles().stream()
                        .map(r -> r.getName())
                        .collect(Collectors.toSet()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
