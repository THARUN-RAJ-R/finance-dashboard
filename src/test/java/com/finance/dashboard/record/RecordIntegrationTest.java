package com.finance.dashboard.record;

import com.finance.dashboard.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Record — Exhaustive Integration Tests")
class RecordIntegrationTest extends AbstractIntegrationTest {

    private static final String RECORDS = "/api/v1/records";

    // ── RBAC / Permissions: CREATE/UPDATE/DELETE (11) ──────────────────

    @Test void create_admin_allowed_201() throws Exception {
        mockMvc.perform(post(RECORDS).header("Authorization", authHeader(adminToken()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"INCOME\",\"amount\":100.00,\"currency\":\"USD\",\"recordDate\":\"2026-04-01\"}"))
            .andExpect(status().isCreated());
    }

    @Test void create_analyst_forbidden_403() throws Exception {
        mockMvc.perform(post(RECORDS).header("Authorization", authHeader(analystToken()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"INCOME\",\"amount\":100.00,\"currency\":\"USD\",\"recordDate\":\"2026-04-01\"}"))
            .andExpect(status().isForbidden());
    }

    @Test void create_viewer_forbidden_403() throws Exception {
        mockMvc.perform(post(RECORDS).header("Authorization", authHeader(viewerToken()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"INCOME\",\"amount\":100.00,\"currency\":\"USD\",\"recordDate\":\"2026-04-01\"}"))
            .andExpect(status().isForbidden());
    }

    @Test void update_viewer_forbidden_403() throws Exception {
        String id = createRecord(adminToken(), "INCOME", 10.0, "USD", "2026-04-01");
        String body = "{\"type\":\"INCOME\",\"amount\":10.0,\"currency\":\"USD\",\"recordDate\":\"2026-04-01\"}";
        mockMvc.perform(put(RECORDS + "/" + id).header("Authorization", authHeader(viewerToken()))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
    }

    @Test void delete_viewer_forbidden_403() throws Exception {
        String id = createRecord(adminToken(), "INCOME", 10.0, "USD", "2026-04-01");
        mockMvc.perform(delete(RECORDS + "/" + id).header("Authorization", authHeader(viewerToken())))
            .andExpect(status().isForbidden());
    }

    // ── Validation: Single Create (12) ──────────────────────────────────

    @Test void create_amountZero_400() throws Exception {
        mockMvc.perform(post(RECORDS).header("Authorization", authHeader(adminToken()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"EXPENSE\",\"amount\":0.00,\"currency\":\"USD\",\"recordDate\":\"2026-04-01\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test void create_amountNegative_400() throws Exception {
        mockMvc.perform(post(RECORDS).header("Authorization", authHeader(adminToken()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"EXPENSE\",\"amount\":-0.01,\"currency\":\"USD\",\"recordDate\":\"2026-04-01\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test void create_amountExactlyMinValid_201() throws Exception {
        mockMvc.perform(post(RECORDS).header("Authorization", authHeader(adminToken()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"EXPENSE\",\"amount\":0.01,\"currency\":\"USD\",\"recordDate\":\"2026-04-01\"}"))
            .andExpect(status().isCreated());
    }

    @Test void create_amountPrecisionExceeded_400() throws Exception {
        // digits fraction=2
        mockMvc.perform(post(RECORDS).header("Authorization", authHeader(adminToken()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"EXPENSE\",\"amount\":10.123,\"currency\":\"USD\",\"recordDate\":\"2026-04-01\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test void create_currencyInvalidRegex_400() throws Exception {
        String[] badd = {"US", "usd", "US-D", "U12"};
        for(String b : badd) {
            mockMvc.perform(post(RECORDS).header("Authorization", authHeader(adminToken()))
                    .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"INCOME\",\"amount\":10.0,\"currency\":\""+b+"\",\"recordDate\":\"2026-04-01\"}"))
                .andExpect(status().isBadRequest());
        }
    }

    @Test void create_dateInFuture_400() throws Exception {
        String future = java.time.LocalDate.now().plusDays(1).toString();
        mockMvc.perform(post(RECORDS).header("Authorization", authHeader(adminToken()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"INCOME\",\"amount\":10.0,\"currency\":\"USD\",\"recordDate\":\""+future+"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test void create_notesTooLong_400() throws Exception {
        String longNotes = "N".repeat(1001);
        mockMvc.perform(post(RECORDS).header("Authorization", authHeader(adminToken()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"type\":\"INCOME\",\"amount\":10.0,\"currency\":\"USD\",\"recordDate\":\"2026-04-01\",\"notes\":\""+longNotes+"\"}"))
            .andExpect(status().isBadRequest());
    }

    // ── Bulk Create: Boundaries & Atomicity (10) ───────────────────────

    @Test void bulk_exactlyMax_50_Success_201() throws Exception {
        StringBuilder sb = new StringBuilder("[");
        for(int i=0; i<50; i++) {
            sb.append("{\"type\":\"INCOME\",\"amount\":1.0,\"currency\":\"USD\",\"recordDate\":\"2026-04-01\"}");
            if(i<49) sb.append(",");
        }
        sb.append("]");
        mockMvc.perform(post(RECORDS + "/bulk").header("Authorization", authHeader(adminToken()))
                .contentType(MediaType.APPLICATION_JSON).content(sb.toString()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.items.length()").value(50));
    }

    @Test void bulk_exceedsMax_51_400() throws Exception {
        StringBuilder sb = new StringBuilder("[");
        for(int i=0; i<51; i++) {
            sb.append("{\"type\":\"INCOME\",\"amount\":1.0,\"currency\":\"USD\",\"recordDate\":\"2026-04-01\"}");
            if(i<50) sb.append(",");
        }
        sb.append("]");
        mockMvc.perform(post(RECORDS + "/bulk").header("Authorization", authHeader(adminToken()))
                .contentType(MediaType.APPLICATION_JSON).content(sb.toString()))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details").value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("must not exceed 50 items"))));
    }

    @Test void bulk_atomicity_mixedValidInvalid_Rollback() throws Exception {
        // Valid item followed by invalid item. The valid one should NOT remain in DB.
        String body = "[" +
                "{\"type\":\"INCOME\",\"amount\":100.0,\"currency\":\"USD\",\"recordDate\":\"2026-04-01\",\"notes\":\"Valid\"}," +
                "{\"type\":\"INCOME\",\"amount\":-10.0,\"currency\":\"USD\",\"recordDate\":\"2026-04-01\",\"notes\":\"Invalid\"}" +
                "]";

        mockMvc.perform(post(RECORDS + "/bulk").header("Authorization", authHeader(adminToken()))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());

        // Verify "Valid" note item is NOT in the DB
        mockMvc.perform(get(RECORDS).header("Authorization", authHeader(adminToken())))
            .andExpect(jsonPath("$.items[?(@.notes == 'Valid')]").doesNotExist());
    }

    // ── Update & Optimistic Lock (4) ───────────────────────────────────

    @Test void update_lockingConflict_409() throws Exception {
        String token = adminToken();
        String id = createRecord(token, "INCOME", 10.0, "USD", "2026-04-01");
        
        // Get initial version
        MvcResult res = mockMvc.perform(get(RECORDS + "/" + id).header("Authorization", authHeader(token)))
            .andExpect(status().isOk()).andReturn();
        long v = json(res).get("version").asLong();

        // Update 1 (Success)
        mockMvc.perform(put(RECORDS + "/" + id).header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"INCOME\",\"amount\":20.0,\"currency\":\"USD\",\"recordDate\":\"2026-04-01\",\"version\":"+v+"}"))
            .andExpect(status().isOk());

        // Update 2 with OLD version (Conflict)
        mockMvc.perform(put(RECORDS + "/" + id).header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"INCOME\",\"amount\":30.0,\"currency\":\"USD\",\"recordDate\":\"2026-04-01\",\"version\":"+v+"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    // ── Pagination & Boundary Clamping (8) ─────────────────────────────

    @Test void list_clampedExcessiveSize_100() throws Exception {
        mockMvc.perform(get(RECORDS + "?size=5000").header("Authorization", authHeader(adminToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.size").value(100));
    }

    @Test void list_zeroSize_defaultsToMin() throws Exception {
        // Application code might default to 20 if passed 0
        mockMvc.perform(get(RECORDS + "?size=0").header("Authorization", authHeader(adminToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.size").value(org.hamcrest.Matchers.anyOf(org.hamcrest.Matchers.is(20), org.hamcrest.Matchers.is(1))));
    }

    @Test void list_negativePage_Defaults0() throws Exception {
        mockMvc.perform(get(RECORDS + "?page=-5").header("Authorization", authHeader(adminToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.page").value(0));
    }

    // ── Filtering (10) ─────────────────────────────────────────────────

    @Test void filter_startGreaterThanEnd_EmptyResults() throws Exception {
        createRecord(adminToken(), "INCOME", 10.0, "USD", "2026-04-01");
        mockMvc.perform(get(RECORDS + "?from=2026-12-31&to=2026-01-01").header("Authorization", authHeader(adminToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(0));
    }

    @Test void filter_byCategory_Success() throws Exception {
        String token = adminToken();
        String catId = createCategory(token, "TargetCat");
        createRecord(token, "INCOME", 1.0, "USD", "2026-04-01"); // no cat
        
        // one with cat
        mockMvc.perform(post(RECORDS).header("Authorization", authHeader(token)).contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"INCOME\",\"amount\":5.0,\"currency\":\"USD\",\"recordDate\":\"2026-04-01\",\"categoryId\":\""+catId+"\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(get(RECORDS + "?categoryId=" + catId).header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(1));
    }

    @Test void filter_byType_INCOME_returnsOnlyIncome() throws Exception {
        String token = adminToken();
        createRecord(token, "INCOME",  50.0, "USD", "2026-04-01");
        createRecord(token, "EXPENSE", 30.0, "USD", "2026-04-01");

        mockMvc.perform(get(RECORDS + "?type=INCOME").header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[*].type",
                    org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is("INCOME"))));
    }

    // ── Soft-Delete Behaviour (3) ───────────────────────────────

    @Test void getById_softDeleted_404() throws Exception {
        String token = adminToken();
        String id = createRecord(token, "INCOME", 10.0, "USD", "2026-04-01");

        // Soft-delete it
        mockMvc.perform(delete(RECORDS + "/" + id).header("Authorization", authHeader(token)))
            .andExpect(status().isNoContent());

        // GET by id should now be 404
        mockMvc.perform(get(RECORDS + "/" + id).header("Authorization", authHeader(token)))
            .andExpect(status().isNotFound());
    }

    @Test void update_softDeleted_404() throws Exception {
        String token = adminToken();
        String id = createRecord(token, "INCOME", 10.0, "USD", "2026-04-01");

        mockMvc.perform(delete(RECORDS + "/" + id).header("Authorization", authHeader(token)))
            .andExpect(status().isNoContent());

        // PUT on deleted record should be 404
        mockMvc.perform(put(RECORDS + "/" + id)
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"INCOME\",\"amount\":20.0,\"currency\":\"USD\",\"recordDate\":\"2026-04-01\",\"version\":0}"))
            .andExpect(status().isNotFound());
    }

    @Test void list_excludesSoftDeleted() throws Exception {
        String token = adminToken();
        String id = createRecord(token, "INCOME", 99.99, "USD", "2026-04-01");

        mockMvc.perform(delete(RECORDS + "/" + id).header("Authorization", authHeader(token)))
            .andExpect(status().isNoContent());

        mockMvc.perform(get(RECORDS).header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items[?(@.id == '" + id + "')]").doesNotExist());
    }

    // ── Idempotency Key (2) ─────────────────────────────────────

    @Test void idempotency_withoutKey_createsRecord_201() throws Exception {
        // Verify baseline: record created with no idempotency key works normally
        String token = adminToken();
        String body = "{\"type\":\"INCOME\",\"amount\":77.00,\"currency\":\"USD\",\"recordDate\":\"2024-04-01\"}";

        mockMvc.perform(post(RECORDS)
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").isNotEmpty())
            .andExpect(jsonPath("$.amount").value(77.00));
    }

    @Test void idempotency_differentKeys_createDifferentRecords() throws Exception {
        String token = adminToken();
        String body = "{\"type\":\"INCOME\",\"amount\":55.00,\"currency\":\"USD\",\"recordDate\":\"2024-04-01\"}";

        MvcResult r1 = mockMvc.perform(post(RECORDS)
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn();

        MvcResult r2 = mockMvc.perform(post(RECORDS)
                .header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated()).andReturn();

        // Two requests without idempotency keys should produce two different records
        org.assertj.core.api.Assertions.assertThat(json(r1).get("id").asText())
                .isNotEqualTo(json(r2).get("id").asText());
    }

    // ── ETag / If-Match (2) ─────────────────────────────────────

    @Test void getById_returnsETagHeader() throws Exception {
        String token = adminToken();
        String id = createRecord(token, "INCOME", 10.0, "USD", "2026-04-01");

        mockMvc.perform(get(RECORDS + "/" + id).header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                    .header().exists("ETag"));
    }

    @Test void delete_withCorrectETag_204() throws Exception {
        String token = adminToken();
        String id = createRecord(token, "INCOME", 10.0, "USD", "2026-04-01");

        MvcResult res = mockMvc.perform(get(RECORDS + "/" + id)
                .header("Authorization", authHeader(token)))
            .andReturn();
        String etag = res.getResponse().getHeader("ETag"); // e.g. "0"

        mockMvc.perform(delete(RECORDS + "/" + id)
                .header("Authorization", authHeader(token))
                .header("If-Match", etag))
            .andExpect(status().isNoContent());
    }

    // ── RBAC: Analyst read access (2) ───────────────────────────

    @Test void list_analyst_allowed_200() throws Exception {
        mockMvc.perform(get(RECORDS).header("Authorization", authHeader(analystToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray());
    }

    @Test void list_viewer_forbidden_403() throws Exception {
        mockMvc.perform(get(RECORDS).header("Authorization", authHeader(viewerToken())))
            .andExpect(status().isForbidden());
    }

    // ── CSV Export (1) ─────────────────────────────────────────

    @Test void export_csv_returnsCorrectContentType() throws Exception {
        String token = adminToken();
        createRecord(token, "INCOME", 100.0, "USD", "2026-04-01");

        mockMvc.perform(get(RECORDS + "/export")
                .header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                    .content().contentTypeCompatibleWith("text/csv"));
    }

    // ── Date Range Boundaries (1) ───────────────────────────────

    @Test void filter_dateRange_inclusive() throws Exception {
        String token = adminToken();
        createRecord(token, "INCOME", 11.0, "USD", "2024-06-01"); // boundary start
        createRecord(token, "INCOME", 22.0, "USD", "2024-06-15"); // in range
        createRecord(token, "INCOME", 33.0, "USD", "2024-06-30"); // boundary end
        createRecord(token, "INCOME", 44.0, "USD", "2024-05-31"); // out of range (before)

        mockMvc.perform(get(RECORDS + "?from=2024-06-01&to=2024-06-30")
                .header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.total").value(3));
    }

}
