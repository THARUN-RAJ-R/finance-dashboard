package com.finance.dashboard.dashboard.dto;

/**
 * Time granularity for dashboard trend queries.
 *
 * <ul>
 *   <li>{@code DAILY}   — one data point per calendar day</li>
 *   <li>{@code MONTHLY} — one data point per calendar month (default)</li>
 * </ul>
 *
 * Spring MVC automatically converts the query parameter string to this enum
 * (case-insensitive by default). An unrecognised value returns HTTP 400.
 */
public enum Granularity {
    DAILY,
    MONTHLY
}
