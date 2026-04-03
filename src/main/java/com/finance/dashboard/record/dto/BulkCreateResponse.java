package com.finance.dashboard.record.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

/** Response for bulk record creation. */
@Data
@Builder
public class BulkCreateResponse {
    private int                  createdCount;
    private List<RecordResponse> items;
}
