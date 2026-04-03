package com.finance.dashboard.record.repository;

import com.finance.dashboard.record.dto.RecordFilterParams;
import com.finance.dashboard.record.entity.FinancialRecordEntity;
import org.springframework.data.jpa.domain.Specification;

/**
 * Composable JPA {@link Specification} predicates for financial record filtering.
 *
 * All predicates automatically exclude soft-deleted records.
 */
public final class RecordSpecification {

    private RecordSpecification() {}

    /** Build a full specification from filter params. */
    public static Specification<FinancialRecordEntity> fromFilter(RecordFilterParams f) {
        return notDeleted()
                .and(fromDate(f))
                .and(toDate(f))
                .and(byType(f))
                .and(byCategory(f))
                .and(minAmount(f))
                .and(maxAmount(f))
                .and(notesSearch(f));
    }

    public static Specification<FinancialRecordEntity> notDeleted() {
        return (root, query, cb) -> cb.isNull(root.get("deletedAt"));
    }

    private static Specification<FinancialRecordEntity> fromDate(RecordFilterParams f) {
        return (root, query, cb) ->
                f.getFrom() == null ? null
                        : cb.greaterThanOrEqualTo(root.get("recordDate"), f.getFrom());
    }

    private static Specification<FinancialRecordEntity> toDate(RecordFilterParams f) {
        return (root, query, cb) ->
                f.getTo() == null ? null
                        : cb.lessThanOrEqualTo(root.get("recordDate"), f.getTo());
    }

    private static Specification<FinancialRecordEntity> byType(RecordFilterParams f) {
        return (root, query, cb) ->
                f.getType() == null ? null
                        : cb.equal(root.get("type"), f.getType());
    }

    private static Specification<FinancialRecordEntity> byCategory(RecordFilterParams f) {
        return (root, query, cb) ->
                f.getCategoryId() == null ? null
                        : cb.equal(root.get("category").get("id"), f.getCategoryId());
    }

    private static Specification<FinancialRecordEntity> minAmount(RecordFilterParams f) {
        return (root, query, cb) ->
                f.getMinAmount() == null ? null
                        : cb.greaterThanOrEqualTo(root.get("amount"), f.getMinAmount());
    }

    private static Specification<FinancialRecordEntity> maxAmount(RecordFilterParams f) {
        return (root, query, cb) ->
                f.getMaxAmount() == null ? null
                        : cb.lessThanOrEqualTo(root.get("amount"), f.getMaxAmount());
    }

    private static Specification<FinancialRecordEntity> notesSearch(RecordFilterParams f) {
        return (root, query, cb) ->
                (f.getSearch() == null || f.getSearch().isBlank()) ? null
                        : cb.like(cb.lower(root.get("notes")),
                                  "%" + f.getSearch().toLowerCase() + "%");
    }
}
