package com.finance.dashboard.dashboard.service;

import com.finance.dashboard.dashboard.dto.CategoryTotalResponse;
import com.finance.dashboard.dashboard.dto.Granularity;
import com.finance.dashboard.dashboard.dto.SummaryResponse;
import com.finance.dashboard.dashboard.dto.TrendPointResponse;
import com.finance.dashboard.record.dto.RecordResponse;
import com.finance.dashboard.record.entity.RecordType;
import com.finance.dashboard.record.repository.RecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Dashboard aggregation service.
 *
 * <p>Uses native SQL queries for all aggregations for performance.
 * Results are cached in Redis for 5 minutes (configured in RedisConfig).
 * Cache is keyed by date range to avoid cross-query pollution.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final RecordRepository recordRepository;

    /**
     * High-level summary: total income, total expenses, net balance.
     *
     * @param from optional start date (ISO format yyyy-MM-dd), null = no lower bound
     * @param to   optional end date,   null = no upper bound
     */
    @Transactional(readOnly = true)
    public SummaryResponse getSummary(LocalDate from, LocalDate to) {
        BigDecimal totalIncome  = recordRepository.sumByType("INCOME",  from, to);
        BigDecimal totalExpense = recordRepository.sumByType("EXPENSE", from, to);
        long incomeCount = recordRepository.countByType("INCOME", from, to);
        long expenseCount = recordRepository.countByType("EXPENSE", from, to);

        BigDecimal netBalance = totalIncome.subtract(totalExpense);

        return SummaryResponse.builder()
                .totalIncome(totalIncome)
                .totalExpenses(totalExpense)
                .netBalance(netBalance)
                .incomeCount(incomeCount)
                .expenseCount(expenseCount)
                .build();
    }

    /**
     * Category-level breakdown — income and expense totals per category.
     */
    @Transactional(readOnly = true)
    public List<CategoryTotalResponse> getByCategory(LocalDate from, LocalDate to) {
        List<Object[]> rows = recordRepository.aggregateByCategory(from, to);

        return rows.stream().map(row -> {
            UUID   catId   = row[0] != null ? UUID.fromString(row[0].toString()) : null;
            String catName = row[1] != null ? row[1].toString() : "Uncategorised";
            String type    = row[2] != null ? row[2].toString() : null;
            BigDecimal total = row[3] != null
                    ? new BigDecimal(row[3].toString()) : BigDecimal.ZERO;

            return CategoryTotalResponse.builder()
                    .categoryId(catId)
                    .categoryName(catName)
                    .type(type)
                    .total(total)
                    .build();
        }).collect(Collectors.toList());
    }

    /**
     * Monthly trend data — income vs expense per calendar month.
     */
    @Transactional(readOnly = true)
    public List<TrendPointResponse> getTrends(LocalDate from, LocalDate to, Granularity granularity) {
        // 1. Fetch raw points
        List<Object[]> rows = (granularity == Granularity.DAILY)
                ? recordRepository.dailyTrends(from, to)
                : recordRepository.monthlyTrends(from, to);

        // 2. Map realized points
        Map<LocalDate, TrendPointResponse> realized = rows.stream()
                .map(row -> {
                    Object periodObj = row[0];
                    LocalDate periodStart;
                    if (periodObj instanceof java.sql.Date sd) {
                        periodStart = sd.toLocalDate();
                    } else if (periodObj instanceof java.sql.Timestamp ts) {
                        periodStart = ts.toLocalDateTime().toLocalDate();
                    } else if (periodObj != null) {
                        try {
                            String dateStr = periodObj.toString();
                            if (dateStr.length() > 10) dateStr = dateStr.substring(0, 10);
                            periodStart = LocalDate.parse(dateStr);
                        } catch (Exception e) {
                            periodStart = LocalDate.now();
                        }
                    } else {
                        periodStart = LocalDate.now();
                    }

                    BigDecimal income  = row[1] != null ? new BigDecimal(row[1].toString()) : BigDecimal.ZERO;
                    BigDecimal expense = row[2] != null ? new BigDecimal(row[2].toString()) : BigDecimal.ZERO;
                    BigDecimal net     = income.subtract(expense);

                    return TrendPointResponse.builder()
                            .periodStart(periodStart)
                            .income(income)
                            .expense(expense)
                            .net(net)
                            .build();
                })
                .collect(java.util.stream.Collectors.toMap(
                        TrendPointResponse::getPeriodStart,
                        tp -> tp,
                        (tp1, tp2) -> tp1 // Keep first on collision
                ));

        // 3. Padded sequence generation
        List<TrendPointResponse> result = new java.util.ArrayList<>();
        if (from == null || to == null) return new java.util.ArrayList<>(realized.values());

        boolean isDaily = (granularity == Granularity.DAILY);
        LocalDate current = isDaily ? from : from.withDayOfMonth(1);
        LocalDate end     = isDaily ? to   : to.withDayOfMonth(1);

        while (!current.isAfter(end)) {
            TrendPointResponse point = realized.get(current);
            if (point == null) {
                point = TrendPointResponse.builder()
                        .periodStart(current)
                        .income(BigDecimal.ZERO)
                        .expense(BigDecimal.ZERO)
                        .net(BigDecimal.ZERO)
                        .build();
            }
            result.add(point);
            current = isDaily ? current.plusDays(1) : current.plusMonths(1);
        }

        return result;
    }

    /**
     * Recent activity — last N financial records.
     * VIEWER sees only totals; ANALYST/ADMIN see full records.
     * Role differentiation is handled at the controller level.
     */
    @Transactional(readOnly = true)
    public List<RecordResponse> getRecentActivity(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return recordRepository.findRecentActive(safeLimit).stream()
                .map(RecordResponse::from)
                .collect(Collectors.toList());
    }
}
