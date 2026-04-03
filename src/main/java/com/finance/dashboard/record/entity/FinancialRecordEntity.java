package com.finance.dashboard.record.entity;

import com.finance.dashboard.user.entity.UserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Maps to the {@code financial_records} table.
 *
 * Key invariants enforced at the DB and service layers:
 * <ul>
 *   <li>amount must be &gt; 0 (DB CHECK constraint + service validation)</li>
 *   <li>currency must be ISO-4217 3-char code</li>
 *   <li>soft-deleted records have {@code deletedAt} set; they are excluded
 *       from all business queries</li>
 * </ul>
 */
@Entity
@Table(name = "financial_records")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FinancialRecordEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** Hibernate optimistic locking — maps to the {@code version} DB column. */
    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private RecordType type;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    /** Amount in minor units expressed as decimal. e.g. 1234.56 */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    /** ISO-4217 3-letter currency code, e.g. "USD". */
    @Column(nullable = false, columnDefinition = "char(3)")
    private String currency;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private UserEntity createdBy;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    // ── Soft-delete helper ─────────────────────────────────────

    public boolean isDeleted() {
        return deletedAt != null;
    }

    public void softDelete() {
        this.deletedAt = Instant.now();
    }
}
