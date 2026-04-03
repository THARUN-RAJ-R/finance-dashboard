package com.finance.dashboard.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Builder
@Schema(description = "Category-level aggregation entry")
public class CategoryTotalResponse {

    @Schema(description = "Category UUID (null = uncategorised)")
    private final UUID       categoryId;

    @Schema(description = "Category name", example = "Groceries")
    private final String     categoryName;

    @Schema(description = "Record type (INCOME or EXPENSE)", example = "EXPENSE")
    private final String     type;

    @Schema(description = "Total amount for this category + type", example = "1250.75")
    private final BigDecimal total;
}
