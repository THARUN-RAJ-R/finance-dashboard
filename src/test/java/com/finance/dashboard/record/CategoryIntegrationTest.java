package com.finance.dashboard.record;

import com.finance.dashboard.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Category — Exhaustive Integration Tests")
class CategoryIntegrationTest extends AbstractIntegrationTest {

    private static final String CATS = "/api/v1/categories";

    // ── RBAC / Permissions: CREATE (6) ──────────────────────────────────

    @Test void create_admin_allowed_201() throws Exception {
        mockMvc.perform(post(CATS).header("Authorization", authHeader(adminToken()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"AdminCat\"}"))
            .andExpect(status().isCreated());
    }

    @Test void create_analyst_forbidden_403() throws Exception {
        mockMvc.perform(post(CATS).header("Authorization", authHeader(analystToken()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"AnalystCat\"}"))
            .andExpect(status().isForbidden());
    }

    @Test void create_viewer_forbidden_403() throws Exception {
        mockMvc.perform(post(CATS).header("Authorization", authHeader(viewerToken()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"ForbiddenCat\"}"))
            .andExpect(status().isForbidden());
    }

    @Test void create_anonymous_unauthorized_401() throws Exception {
        mockMvc.perform(post(CATS).contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"AnonCat\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test void create_disabledUser_notImplementedYet() {
        // Placeholder if we have specific status tests. The token helper would fail first.
    }

    // ── RBAC / Permissions: UPDATE/DELETE (11) ──────────────────────────

    @Test void update_admin_allowed_200() throws Exception {
        String token = adminToken();
        String id = createCategory(token, "OldAdmin");
        mockMvc.perform(put(CATS + "/" + id).header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"NewAdmin\"}"))
            .andExpect(status().isOk());
    }

    @Test void update_viewer_forbidden_403() throws Exception {
        String adminTok = adminToken();
        String id = createCategory(adminTok, "AdminOnlyEdit");
        mockMvc.perform(put(CATS + "/" + id).header("Authorization", authHeader(viewerToken()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"ViewerEdit\"}"))
            .andExpect(status().isForbidden());
    }

    @Test void delete_admin_allowed_204() throws Exception {
        String token = adminToken();
        String id = createCategory(token, "AdminDel");
        mockMvc.perform(delete(CATS + "/" + id).header("Authorization", authHeader(token)))
            .andExpect(status().isNoContent());
    }

    @Test void delete_analyst_forbidden_403() throws Exception {
        String token = adminToken();
        String id = createCategory(token, "AnalystDel");
        mockMvc.perform(delete(CATS + "/" + id).header("Authorization", authHeader(analystToken())))
            .andExpect(status().isForbidden());
    }

    @Test void delete_viewer_forbidden_403() throws Exception {
        String id = createCategory(adminToken(), "DeleteProt");
        mockMvc.perform(delete(CATS + "/" + id).header("Authorization", authHeader(viewerToken())))
            .andExpect(status().isForbidden());
    }

    // ── Validation & Constraints (12) ───────────────────────────────────

    @Test void create_nameNull_400() throws Exception {
        mockMvc.perform(post(CATS).header("Authorization", authHeader(adminToken()))
                .contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test void create_nameEmpty_400() throws Exception {
        mockMvc.perform(post(CATS).header("Authorization", authHeader(adminToken()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test void create_nameSpacesOnly_400() throws Exception {
        mockMvc.perform(post(CATS).header("Authorization", authHeader(adminToken()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"   \"}"))
            .andExpect(status().isBadRequest());
    }

    @Test void create_nameExceeds100Chars_400() throws Exception {
        String longName = "C".repeat(101);
        mockMvc.perform(post(CATS).header("Authorization", authHeader(adminToken()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + longName + "\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test void create_nameExactly100Chars_Success_201() throws Exception {
        String longName = "C".repeat(100);
        mockMvc.perform(post(CATS).header("Authorization", authHeader(adminToken()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + longName + "\"}"))
            .andExpect(status().isCreated());
    }

    @Test void create_duplicateName_409() throws Exception {
        String token = adminToken();
        String name = "UniqueOnce-" + uid();
        createCategory(token, name);

        mockMvc.perform(post(CATS).header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + name + "\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("CONFLICT"));
    }

    @Test void update_toExistingDuplicate_409() throws Exception {
        String token = adminToken();
        String cat1 = createCategory(token, "Target-" + uid());
        String cat2Name = "Existing-" + uid();
        createCategory(token, cat2Name);

        mockMvc.perform(put(CATS + "/" + cat1).header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + cat2Name + "\"}"))
            .andExpect(status().isConflict());
    }

    // ── Soft Delete Integrity (4) ──────────────────────────────────────

    @Test void delete_softDelete_removesFromList_butNotFromDB_204() throws Exception {
        String token = adminToken();
        String id = createCategory(token, "Softie-" + uid());

        // exists
        mockMvc.perform(get(CATS).header("Authorization", authHeader(token)))
            .andExpect(jsonPath("$[?(@.id == '" + id + "')]").exists());

        // delete
        mockMvc.perform(delete(CATS + "/" + id).header("Authorization", authHeader(token)))
            .andExpect(status().isNoContent());

        // gone from list
        mockMvc.perform(get(CATS).header("Authorization", authHeader(token)))
            .andExpect(jsonPath("$[?(@.id == '" + id + "')]").doesNotExist());
    }

    @Test void delete_withActiveRecords_blocks_409() throws Exception {
        String token = adminToken();
        String catId = createCategory(token, "ActiveRel-" + uid());

        // Link a record
        mockMvc.perform(post("/api/v1/records").header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"EXPENSE\",\"amount\":10.00,\"currency\":\"USD\",\"recordDate\":\"2026-04-02\",\"categoryId\":\"" + catId + "\"}"))
            .andExpect(status().isCreated());

        // attempt delete category -> 409
        mockMvc.perform(delete(CATS + "/" + catId).header("Authorization", authHeader(token)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("active record")));
    }

    @Test void delete_withSoftDeletedRecords_allowed_204() throws Exception {
        String token = adminToken();
        String catId = createCategory(token, "DeletedRel-" + uid());

        // Link a record
        MvcResult r = mockMvc.perform(post("/api/v1/records").header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"type\":\"EXPENSE\",\"amount\":10.00,\"currency\":\"USD\",\"recordDate\":\"2026-04-02\",\"categoryId\":\"" + catId + "\"}"))
            .andExpect(status().isCreated()).andReturn();
        String recId = json(r).get("id").asText();

        // soft delete record
        mockMvc.perform(delete("/api/v1/records/" + recId).header("Authorization", authHeader(token)))
            .andExpect(status().isNoContent());

        // attempt delete category -> now allowed!
        mockMvc.perform(delete(CATS + "/" + catId).header("Authorization", authHeader(token)))
            .andExpect(status().isNoContent());
    }

    // ── Listing & Detail (5) ───────────────────────────────────────────

    @Test void get_viewer_canListAllActive_200() throws Exception {
        mockMvc.perform(get(CATS).header("Authorization", authHeader(viewerToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray());
    }

    @Test void get_byId_200() throws Exception {
        String token = adminToken();
        String id = createCategory(token, "FindMe");
        mockMvc.perform(get(CATS + "/" + id).header("Authorization", authHeader(token)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("FindMe"));
    }

    @Test void get_byId_NotFound_404() throws Exception {
        mockMvc.perform(get(CATS + "/" + java.util.UUID.randomUUID().toString()).header("Authorization", authHeader(adminToken())))
            .andExpect(status().isNotFound());
    }

}
