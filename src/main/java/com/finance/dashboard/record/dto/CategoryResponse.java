package com.finance.dashboard.record.dto;

import com.finance.dashboard.record.entity.CategoryEntity;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/** Response DTO for a single category. */
@Data
@Builder
public class CategoryResponse {
    private UUID   id;
    private String name;
    private Instant createdAt;

    public static CategoryResponse from(CategoryEntity e) {
        return CategoryResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
