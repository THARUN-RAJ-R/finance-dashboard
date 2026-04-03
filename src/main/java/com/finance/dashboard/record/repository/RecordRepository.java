package com.finance.dashboard.record.repository;

import com.finance.dashboard.record.entity.FinancialRecordEntity;
import com.finance.dashboard.record.entity.RecordType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RecordRepository extends
        JpaRepository<FinancialRecordEntity, UUID>,
        JpaSpecificationExecutor<FinancialRecordEntity> {

    /** Fetch by id only if not soft-deleted. */
    @Query("SELECT r FROM FinancialRecordEntity r " +
           "LEFT JOIN FETCH r.category " +
           "LEFT JOIN FETCH r.createdBy " +
           "WHERE r.id = :id AND r.deletedAt IS NULL")
    Optional<FinancialRecordEntity> findActiveById(@Param("id") UUID id);

    // ── Dashboard aggregation queries (native SQL for performance) ─────

    @Query(value = """
            SELECT COALESCE(SUM(amount), 0)
            FROM financial_records
            WHERE type = :type
              AND deleted_at IS NULL
              AND (CAST(:fromDate AS date) IS NULL OR record_date >= CAST(:fromDate AS date))
              AND (CAST(:toDate   AS date) IS NULL OR record_date <= CAST(:toDate   AS date))
            """, nativeQuery = true)
    BigDecimal sumByType(
            @Param("type")     String    type,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate")   LocalDate toDate
    );

    @Query(value = """
            SELECT COUNT(*)
            FROM financial_records
            WHERE type = :type
              AND deleted_at IS NULL
              AND (CAST(:fromDate AS date) IS NULL OR record_date >= CAST(:fromDate AS date))
              AND (CAST(:toDate   AS date) IS NULL OR record_date <= CAST(:toDate   AS date))
            """, nativeQuery = true)
    long countByType(
            @Param("type")     String    type,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate")   LocalDate toDate
    );

    @Query(value = """
            SELECT c.id          AS categoryId,
                   c.name        AS categoryName,
                   r.type        AS type,
                   SUM(r.amount) AS total
            FROM financial_records r
            LEFT JOIN categories c ON c.id = r.category_id
            WHERE r.deleted_at IS NULL
              AND (CAST(:fromDate AS date) IS NULL OR r.record_date >= CAST(:fromDate AS date))
              AND (CAST(:toDate   AS date) IS NULL OR r.record_date <= CAST(:toDate   AS date))
            GROUP BY c.id, c.name, r.type
            ORDER BY total DESC
            """, nativeQuery = true)
    List<Object[]> aggregateByCategory(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate")   LocalDate toDate
    );

    @Query(value = """
            SELECT date_trunc('month', record_date)::date AS periodStart,
                   SUM(CASE WHEN type = 'INCOME'  THEN amount ELSE 0 END) AS income,
                   SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) AS expense
            FROM financial_records
            WHERE deleted_at IS NULL
              AND (CAST(:fromDate AS date) IS NULL OR record_date >= CAST(:fromDate AS date))
              AND (CAST(:toDate   AS date) IS NULL OR record_date <= CAST(:toDate   AS date))
            GROUP BY date_trunc('month', record_date)
            ORDER BY periodStart ASC
            """, nativeQuery = true)
    List<Object[]> monthlyTrends(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate")   LocalDate toDate
    );

    @Query(value = """
            SELECT record_date::date AS periodStart,
                   SUM(CASE WHEN type = 'INCOME'  THEN amount ELSE 0 END) AS income,
                   SUM(CASE WHEN type = 'EXPENSE' THEN amount ELSE 0 END) AS expense
            FROM financial_records
            WHERE deleted_at IS NULL
              AND (CAST(:fromDate AS date) IS NULL OR record_date >= CAST(:fromDate AS date))
              AND (CAST(:toDate   AS date) IS NULL OR record_date <= CAST(:toDate   AS date))
            GROUP BY record_date
            ORDER BY periodStart ASC
            """, nativeQuery = true)
    List<Object[]> dailyTrends(
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate")   LocalDate toDate
    );

    /** Recent N active records ordered by record_date DESC. */
    @Query(value = """
            SELECT r.* FROM financial_records r
            WHERE r.deleted_at IS NULL
            ORDER BY r.record_date DESC, r.created_at DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<FinancialRecordEntity> findRecentActive(@Param("limit") int limit);

    /** Count active (non-deleted) records referencing a category — used by category soft-delete guard. */
    @Query("SELECT COUNT(r) FROM FinancialRecordEntity r WHERE r.category.id = :categoryId AND r.deletedAt IS NULL")
    long countActiveByCategory(@Param("categoryId") UUID categoryId);
}
