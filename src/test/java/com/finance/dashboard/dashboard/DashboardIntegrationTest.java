package com.finance.dashboard.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.finance.dashboard.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Dashboard — Exhaustive Integration Tests")
class DashboardIntegrationTest extends AbstractIntegrationTest {

    private static final String DASHBOARD = "/api/v1/dashboard";

    // ── Summary Metrics Accuracy (10) ───────────────────────────────────

    @Test void summary_mathAccuracy_net() throws Exception {
        String token = adminToken();
        createRecord(token, "INCOME", 1000.0, "USD", "2026-03-01");
        createRecord(token, "EXPENSE", 400.0, "USD", "2026-03-02");
        createRecord(token, "EXPENSE", 200.0, "USD", "2026-03-03");
        createRecord(token, "INCOME", 150.0, "USD", "2026-03-04");

        MvcResult r = mockMvc.perform(get(DASHBOARD + "/summary?from=2026-03-01&to=2026-03-31")
                .header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andReturn();

        JsonNode json = json(r);
        assertThat(json.get("totalIncome").asDouble()).isEqualTo(1150.0);
        assertThat(json.get("totalExpenses").asDouble()).isEqualTo(600.0);
        assertThat(json.get("netBalance").asDouble()).isEqualTo(550.0);
    }

    @Test void summary_excludeSoftDeleted_Calculations() throws Exception {
        String token = adminToken();
        createRecord(token, "INCOME", 1000.0, "USD", "2026-03-01");
        
        // one to delete
        String id = createRecord(token, "INCOME", 500.0, "USD", "2026-03-01");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/v1/records/" + id).header("Authorization", authHeader(token)))
            .andExpect(status().isNoContent());

        MvcResult r = mockMvc.perform(get(DASHBOARD + "/summary?from=2026-03-01&to=2026-03-01")
                .header("Authorization", authHeader(token)))
            .andExpect(status().isOk()).andReturn();

        assertThat(json(r).get("totalIncome").asDouble()).isEqualTo(1000.0); // 1500 - 500
    }

    @Test void summary_emptyRange_Zeros() throws Exception {
        MvcResult r = mockMvc.perform(get(DASHBOARD + "/summary?from=2000-01-01&to=2000-01-01")
                .header("Authorization", authHeader(adminToken())))
            .andExpect(status().isOk()).andReturn();

        assertThat(json(r).get("totalIncome").asDouble()).isEqualTo(0.0);
        assertThat(json(r).get("totalExpenses").asDouble()).isEqualTo(0.0);
    }

    @Test void summary_viewerRolePermitted_200() throws Exception {
        mockMvc.perform(get(DASHBOARD + "/summary").header("Authorization", authHeader(viewerToken())))
            .andExpect(status().isOk());
    }

    @Test void summary_badDateFormat_400() throws Exception {
        mockMvc.perform(get(DASHBOARD + "/summary?from=NOTADATE").header("Authorization", authHeader(adminToken())))
            .andExpect(status().isBadRequest());
    }

    // ── Trends Padding & Granularity (10) ───────────────────────────────

    @Test void trends_dailyPadding_correctPointsCount() throws Exception {
        String token = adminToken();
        createRecord(token, "INCOME", 100.0, "USD", "2026-01-01");
        
        // range of 7 days
        MvcResult r = mockMvc.perform(get(DASHBOARD + "/trends?from=2026-01-01&to=2026-01-07&granularity=DAILY")
                .header("Authorization", authHeader(token)))
            .andExpect(status().isOk()).andReturn();

        JsonNode arr = json(r);
        assertThat(arr.size()).isEqualTo(7);
        assertThat(arr.get(0).get("date").asText()).isEqualTo("2026-01-01");
        assertThat(arr.get(0).get("income").asDouble()).isEqualTo(100.0);
        assertThat(arr.get(6).get("date").asText()).isEqualTo("2026-01-07");
        assertThat(arr.get(6).get("income").asDouble()).isEqualTo(0.0); // padded
    }

    @Test void trends_granularityLogic_MONTHLY() throws Exception {
        String token = adminToken();
        createRecord(token, "INCOME", 100.0, "USD", "2026-01-01");
        createRecord(token, "INCOME", 200.0, "USD", "2026-02-15");

        MvcResult r = mockMvc.perform(get(DASHBOARD + "/trends?from=2026-01-01&to=2026-03-01&granularity=MONTHLY")
                .header("Authorization", authHeader(token)))
            .andExpect(status().isOk()).andReturn();

        // Jan, Feb, Mar = 3 points
        JsonNode arr = json(r);
        assertThat(arr.size()).isEqualTo(3);
        assertThat(arr.get(0).get("date").asText()).isEqualTo("2026-01-01");
        assertThat(arr.get(1).get("date").asText()).isEqualTo("2026-02-01");
        assertThat(arr.get(1).get("income").asDouble()).isEqualTo(200.0);
    }

    // ── Activity (5) ────────────────────────────────────────────────────

    @Test void activity_limitsCorrectly() throws Exception {
        String token = adminToken();
        for(int i=0; i<15; i++) createRecord(token, "INCOME", 1.0, "USD", "2026-04-02");

        mockMvc.perform(get(DASHBOARD + "/recent-activity?limit=5").header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(5));
    }

    @Test void activity_viewerRoleAllowed_200() throws Exception {
        mockMvc.perform(get(DASHBOARD + "/recent-activity").header("Authorization", authHeader(viewerToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Upgrade to ANALYST")));
    }

    // ── By-Category Endpoint (2) ─────────────────────────────────────────

    @Test void byCategory_emptyDataset_returnsEmptyList() throws Exception {
        // Fresh DB — no records seeded in current test epoch
        mockMvc.perform(get(DASHBOARD + "/by-category?from=2000-01-01&to=2000-01-02")
                .header("Authorization", authHeader(viewerToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test void byCategory_groupsByCategory_correctly() throws Exception {
        String token = adminToken();
        String catId = createCategory(token, "Housing-" + uid());

        // 2 records in same category
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/v1/records")
                .header("Authorization", authHeader(token))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"type\":\"EXPENSE\",\"amount\":500.00,\"currency\":\"USD\"," +
                        "\"recordDate\":\"2024-05-01\",\"categoryId\":\"" + catId + "\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/v1/records")
                .header("Authorization", authHeader(token))
                .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                .content("{\"type\":\"EXPENSE\",\"amount\":300.00,\"currency\":\"USD\"," +
                        "\"recordDate\":\"2024-05-02\",\"categoryId\":\"" + catId + "\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get(DASHBOARD + "/by-category?from=2024-05-01&to=2024-05-31")
                .header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[?(@.categoryId == '" + catId + "')].total")
                    .value(org.hamcrest.Matchers.hasItem(
                            org.hamcrest.Matchers.closeTo(800.0, 0.01))));
    }

    // ── Trends: Invalid Granularity → 400 (1) ───────────────────────────

    @Test void trends_invalidGranularity_400() throws Exception {
        mockMvc.perform(get(DASHBOARD + "/trends?from=2026-01-01&to=2026-03-01&granularity=HOURLY")
                .header("Authorization", authHeader(adminToken())))
            .andExpect(status().isBadRequest());
    }

    // ── RBAC: Analyst can access all dashboard endpoints (1) ────────────

    @Test void allDashboardEndpoints_analystAllowed_200() throws Exception {
        String token = analystToken();

        mockMvc.perform(get(DASHBOARD + "/summary")
                .header("Authorization", authHeader(token)))
            .andExpect(status().isOk());

        mockMvc.perform(get(DASHBOARD + "/by-category")
                .header("Authorization", authHeader(token)))
            .andExpect(status().isOk());

        mockMvc.perform(get(DASHBOARD + "/trends?granularity=MONTHLY")
                .header("Authorization", authHeader(token)))
            .andExpect(status().isOk());

        mockMvc.perform(get(DASHBOARD + "/recent-activity")
                .header("Authorization", authHeader(token)))
            .andExpect(status().isOk());
    }

}
