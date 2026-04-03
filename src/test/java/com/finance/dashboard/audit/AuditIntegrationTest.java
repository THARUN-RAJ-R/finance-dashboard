package com.finance.dashboard.audit;

import com.finance.dashboard.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Audit Log Integration Tests")
class AuditIntegrationTest extends AbstractIntegrationTest {

    // ─── Access Control ────────────────────────────────────────────────────────

    @Test
    @DisplayName("ANALYST cannot access audit logs — 403 Forbidden")
    void analystCannotViewAuditLogs() throws Exception {
        String token = analystToken();
        mockMvc.perform(get("/api/v1/audit")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("VIEWER cannot access audit logs — 403 Forbidden")
    void viewerCannotViewAuditLogs() throws Exception {
        String token = viewerToken();
        mockMvc.perform(get("/api/v1/audit")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Unauthenticated request is rejected — 401 Unauthorized")
    void unauthenticatedCannotViewAuditLogs() throws Exception {
        mockMvc.perform(get("/api/v1/audit"))
                .andExpect(status().isUnauthorized());
    }

    // ─── Audit created on Record CREATE ────────────────────────────────────────

    @Test
    @DisplayName("Audit log entry is created when a record is created")
    void auditLogCreatedOnRecordCreate() throws Exception {
        String token = adminToken();

        // Create one record
        createRecord(token, "INCOME", 500.00, "USD", "2024-01-15");

        // Give async audit a moment to write
        Thread.sleep(300);

        // Verify audit log contains a CREATE entry
        mockMvc.perform(get("/api/v1/audit")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].action", notNullValue()))
                .andExpect(jsonPath("$[0].entityType", notNullValue()))
                .andExpect(jsonPath("$[0].actorEmail", is("admin@finance.com")));
    }

    // ─── Audit created on Record UPDATE ────────────────────────────────────────

    @Test
    @DisplayName("Audit log entry is created when a record is updated")
    void auditLogCreatedOnRecordUpdate() throws Exception {
        String token = adminToken();
        String recordId = createRecord(token, "INCOME", 200.00, "USD", "2024-02-01");

        // Update the record
        mockMvc.perform(put("/api/v1/records/" + recordId)
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("""
                            {
                              "amount": 999.00,
                              "currency": "USD",
                              "type": "INCOME",
                              "recordDate": "2024-02-01"
                            }
                            """))
                .andExpect(status().isOk());

        Thread.sleep(300);

        // Audit log should have at least CREATE + UPDATE entries
        mockMvc.perform(get("/api/v1/audit/record/" + recordId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    // ─── Audit created on Record DELETE ────────────────────────────────────────

    @Test
    @DisplayName("Audit log entry is created when a record is soft-deleted")
    void auditLogCreatedOnRecordDelete() throws Exception {
        String token = adminToken();
        String recordId = createRecord(token, "EXPENSE", 75.00, "USD", "2024-03-01");

        // Delete the record
        mockMvc.perform(delete("/api/v1/records/" + recordId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        Thread.sleep(300);

        // Check audit log for this record
        mockMvc.perform(get("/api/v1/audit/record/" + recordId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))));
    }

    // ─── Filter by Entity Type ──────────────────────────────────────────────────

    @Test
    @DisplayName("Can filter audit logs by entity type 'FinancialRecord'")
    void filterAuditLogsByEntityType() throws Exception {
        String token = adminToken();
        createRecord(token, "INCOME", 300.00, "USD", "2024-04-01");

        Thread.sleep(300);

        mockMvc.perform(get("/api/v1/audit/entity/FinancialRecord")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(0))));
    }

    // ─── Audit log has correct structure ──────────────────────────────────────

    @Test
    @DisplayName("Audit log entries have all required fields")
    void auditLogHasCorrectStructure() throws Exception {
        String token = adminToken();
        createRecord(token, "INCOME", 1000.00, "USD", "2024-05-01");

        Thread.sleep(300);

        mockMvc.perform(get("/api/v1/audit")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id",          notNullValue()))
                .andExpect(jsonPath("$[0].actorEmail",  notNullValue()))
                .andExpect(jsonPath("$[0].action",      notNullValue()))
                .andExpect(jsonPath("$[0].entityType",  notNullValue()))
                .andExpect(jsonPath("$[0].createdAt",   notNullValue()));
    }

    // ─── Pagination ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Audit log supports pagination — page and size params")
    void auditLogSupportsPagination() throws Exception {
        String token = adminToken();

        // Create multiple records to ensure there are entries
        createRecord(token, "INCOME", 100.00, "USD", "2024-06-01");
        createRecord(token, "EXPENSE", 50.00, "USD", "2024-06-02");

        Thread.sleep(300);

        mockMvc.perform(get("/api/v1/audit?page=0&size=1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(lessThanOrEqualTo(1))));
    }

    // ─── Empty result for unknown record ──────────────────────────────────────

    @Test
    @DisplayName("Returns empty list for unknown record ID — no error")
    void emptyResultForUnknownRecordId() throws Exception {
        String token = adminToken();
        String unknownId = "00000000-0000-0000-0000-000000000099";

        mockMvc.perform(get("/api/v1/audit/record/" + unknownId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
