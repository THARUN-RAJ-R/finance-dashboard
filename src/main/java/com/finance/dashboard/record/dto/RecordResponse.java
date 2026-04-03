package com.finance.dashboard.record.dto;

import com.finance.dashboard.record.entity.FinancialRecordEntity;
import com.finance.dashboard.record.entity.RecordType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter
@Builder
@Schema(description = "Financial record response")
public class RecordResponse {

    private final UUID       id;
    private final Long       version;
    private final LocalDate  recordDate;
    private final RecordType type;
    private final UUID       categoryId;
    private final String     categoryName;
    private final BigDecimal amount;
    private final String     currency;
    private final String     notes;
    private final UUID       createdById;
    private final String     createdByEmail;
    private final Instant    createdAt;
    private final Instant    updatedAt;

    public static RecordResponse from(FinancialRecordEntity r) {
        return RecordResponse.builder()
                .id(r.getId())
                .version(r.getVersion())
                .recordDate(r.getRecordDate())
                .type(r.getType())
                .categoryId(r.getCategory() != null ? r.getCategory().getId() : null)
                .categoryName(r.getCategory() != null ? r.getCategory().getName() : null)
                .amount(r.getAmount())
                .currency(r.getCurrency())
                .notes(r.getNotes())
                .createdById(r.getCreatedBy() != null ? r.getCreatedBy().getId() : null)
                .createdByEmail(r.getCreatedBy() != null ? r.getCreatedBy().getEmail() : null)
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
