package com.finance.dashboard.record.dto;

import com.finance.dashboard.record.entity.RecordType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Parameters for filtering financial records")
public class RecordFilterParams {

    @Schema(description = "Filter records after this date (ISO YYYY-MM-DD)")
    private java.time.LocalDate from;

    @Schema(description = "Filter records before this date (ISO YYYY-MM-DD)")
    private java.time.LocalDate to;

    @Schema(description = "Filter by record type")
    private RecordType type;

    @Schema(description = "Filter by category ID")
    private UUID categoryId;

    @Schema(description = "Minimum amount")
    private BigDecimal minAmount;

    @Schema(description = "Maximum amount")
    private BigDecimal maxAmount;

    @Schema(description = "Search in notes")
    private String search;

    @Builder.Default
    @Schema(description = "Page number (0-indexed)", example = "0")
    private int page = 0;

    @Builder.Default
    @Schema(description = "Page size", example = "20")
    private int size = 20;

    @Builder.Default
    @Schema(description = "Field to sort by", example = "recordDate")
    private String sort = "recordDate";

    @Builder.Default
    @Schema(description = "Sort direction (asc or desc)", example = "desc")
    private String direction = "desc";
}
