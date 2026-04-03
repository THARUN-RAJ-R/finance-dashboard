package com.finance.dashboard.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Builder
@Schema(description = "A single point on the trend chart (daily or monthly)")
public class TrendPointResponse {

    @JsonProperty("date")
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Schema(description = "The date this point represents", example = "2024-03-01")
    private final LocalDate periodStart;

    @Schema(description = "Total income for the period", example = "1000.00")
    private final BigDecimal income;

    @Schema(description = "Total expenses for the period", example = "400.50")
    private final BigDecimal expense;

    @Schema(description = "Net for the period (income - expense)", example = "599.50")
    private final BigDecimal net;
}
