package com.finance.dashboard.record.dto;

import com.finance.dashboard.record.entity.RecordType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Full replacement update (PUT semantics).
 * All fields required unless explicitly marked optional.
 */
@Getter
@NoArgsConstructor
@Schema(description = "Request to fully update a financial record")
public class UpdateRecordRequest {

    @NotNull(message = "Record date is required")
    @PastOrPresent(message = "Record date cannot be in the future")
    @Schema(example = "2024-03-20")
    private LocalDate recordDate;

    @NotNull(message = "Type is required")
    @Schema(example = "INCOME")
    private RecordType type;

    @Schema(description = "Optional category UUID")
    private UUID categoryId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 17, fraction = 2, message = "Amount must have at most 2 decimal places")
    @Schema(example = "5000.00")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO-4217 code")
    @Schema(example = "EUR")
    private String currency;

    @Size(max = 1000)
    @Schema(example = "Updated notes")
    private String notes;

    @Schema(description = "Optional version for optimistic locking (fallback for If-Match)")
    private Long version;
}
