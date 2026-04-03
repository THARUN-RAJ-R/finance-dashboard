package com.finance.dashboard.record.dto;

import com.finance.dashboard.record.entity.RecordType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "Request to create a new financial record")
public class CreateRecordRequest {

    @NotNull(message = "Record date is required")
    @PastOrPresent(message = "Record date cannot be in the future")
    @Schema(example = "2024-03-15")
    private LocalDate recordDate;

    @NotNull(message = "Type is required (INCOME or EXPENSE)")
    @Schema(example = "EXPENSE")
    private RecordType type;

    @Schema(description = "Optional category UUID", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID categoryId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than zero")
    @Digits(integer = 17, fraction = 2, message = "Amount must have at most 2 decimal places")
    @Schema(example = "250.00")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a 3-letter ISO-4217 code")
    @Schema(example = "USD")
    private String currency;

    @Size(max = 1000, message = "Notes may not exceed 1000 characters")
    @Schema(example = "Monthly rent payment")
    private String notes;
}
