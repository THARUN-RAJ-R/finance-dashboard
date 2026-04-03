package com.finance.dashboard.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
@Schema(description = "High-level financial summary for the dashboard")
public class SummaryResponse {

    @Schema(description = "Total income in the period", example = "12500.00")
    private final BigDecimal totalIncome;

    @Schema(description = "Total expenses in the period", example = "8300.50")
    private final BigDecimal totalExpenses;

    @Schema(description = "Net balance (income − expenses)", example = "4199.50")
    private final BigDecimal netBalance;

    @Schema(description = "Number of income records", example = "14")
    private final long incomeCount;

    @Schema(description = "Number of expense records", example = "32")
    private final long expenseCount;
}
