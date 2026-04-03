package com.finance.dashboard.dashboard.controller;

import com.finance.dashboard.dashboard.dto.CategoryTotalResponse;
import com.finance.dashboard.dashboard.dto.Granularity;
import com.finance.dashboard.dashboard.dto.SummaryResponse;
import com.finance.dashboard.dashboard.dto.TrendPointResponse;
import com.finance.dashboard.dashboard.service.DashboardService;
import com.finance.dashboard.record.dto.RecordResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard", description = "Financial analytics and summary endpoints")
@SecurityRequirement(name = "bearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    @Operation(summary = "Get total income, expenses, and net balance (all roles)")
    public ResponseEntity<SummaryResponse> getSummary(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Start date (yyyy-MM-dd)") LocalDate from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "End date (yyyy-MM-dd)") LocalDate to
    ) {
        return ResponseEntity.ok(dashboardService.getSummary(from, to));
    }

    @GetMapping("/by-category")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    @Operation(summary = "Get income/expense totals grouped by category (all roles)")
    public ResponseEntity<List<CategoryTotalResponse>> getByCategory(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ResponseEntity.ok(dashboardService.getByCategory(from, to));
    }

    @GetMapping("/trends")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    @Operation(summary = "Get income vs expense trend (all roles)")
    public ResponseEntity<List<TrendPointResponse>> getTrends(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,

            @RequestParam(defaultValue = "MONTHLY") Granularity granularity
    ) {
        return ResponseEntity.ok(dashboardService.getTrends(from, to, granularity));
    }

    @GetMapping("/recent-activity")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST', 'VIEWER')")
    @Operation(summary = "Get recent financial activity. " +
            "ANALYST/ADMIN see full records; VIEWER sees only summary totals.")
    public ResponseEntity<?> getRecentActivity(
            @RequestParam(defaultValue = "10") int limit,
            Authentication authentication
    ) {
        boolean isViewer = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_VIEWER"))
                && authentication.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_ANALYST")
                        || a.getAuthority().equals("ROLE_ADMIN"));

        if (isViewer) {
            // VIEWER: return only totals summary
            List<RecordResponse> records = dashboardService.getRecentActivity(limit);
            long incomeCount  = records.stream().filter(r -> r.getType().name().equals("INCOME")).count();
            long expenseCount = records.stream().filter(r -> r.getType().name().equals("EXPENSE")).count();
            return ResponseEntity.ok(Map.of(
                    "totalRecords",  records.size(),
                    "incomeCount",   incomeCount,
                    "expenseCount",  expenseCount,
                    "message",       "Upgrade to ANALYST role for full record details"
            ));
        }

        return ResponseEntity.ok(dashboardService.getRecentActivity(limit));
    }
}
