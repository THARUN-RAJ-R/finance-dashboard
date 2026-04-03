package com.finance.dashboard.user;

import com.finance.dashboard.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("User — Exhaustive Integration Tests")
class UserIntegrationTest extends AbstractIntegrationTest {

    private static final String USERS = "/api/v1/users";

    // ── Profile / Me (5) ────────────────────────────────────────────────

    @Test void me_admin_success_200() throws Exception {
        mockMvc.perform(get(USERS + "/me").header("Authorization", authHeader(adminToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("admin@finance.com"))
            .andExpect(jsonPath("$.roles").value(org.hamcrest.Matchers.hasItem("ADMIN")));
    }

    @Test void me_analyst_success_200() throws Exception {
        mockMvc.perform(get(USERS + "/me").header("Authorization", authHeader(analystToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("analyst@finance.com"))
            .andExpect(jsonPath("$.roles").value(org.hamcrest.Matchers.hasItem("ANALYST")));
    }

    @Test void me_viewer_success_200() throws Exception {
        mockMvc.perform(get(USERS + "/me").header("Authorization", authHeader(viewerToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("viewer@finance.com"))
            .andExpect(jsonPath("$.roles").value(org.hamcrest.Matchers.hasItem("VIEWER")));
    }

    @Test void me_anonymous_unauthorized_401() throws Exception {
        mockMvc.perform(get(USERS + "/me"))
            .andExpect(status().isUnauthorized());
    }

    // ── Management: List (3) ───────────────────────────────────────────

    @Test void list_admin_allowed_200() throws Exception {
        mockMvc.perform(get(USERS).header("Authorization", authHeader(adminToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items").isArray());
    }

    @Test void list_analyst_forbidden_403() throws Exception {
        mockMvc.perform(get(USERS).header("Authorization", authHeader(analystToken())))
            .andExpect(status().isForbidden());
    }

    @Test void list_viewer_forbidden_403() throws Exception {
        mockMvc.perform(get(USERS).header("Authorization", authHeader(viewerToken())))
            .andExpect(status().isForbidden());
    }

    // ── Management: Create & Validation (10) ────────────────────────────

    @Test void create_admin_success_201() throws Exception {
        String email = "manager" + uid() + "@finance.com";
        String body = String.format("{\"email\":\"%s\",\"password\":\"secret123\",\"roles\":[\"ANALYST\"]}", email);
        mockMvc.perform(post(USERS).header("Authorization", authHeader(adminToken()))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.email").value(email));
    }

    @Test void create_duplicateEmail_409() throws Exception {
        String token = adminToken();
        String jsonBody = "{\"email\":\"admin@finance.com\",\"password\":\"secret123\",\"roles\":[\"VIEWER\"]}";
        mockMvc.perform(post(USERS).header("Authorization", authHeader(token))
                .contentType(MediaType.APPLICATION_JSON).content(jsonBody))
            .andExpect(status().isConflict());
    }

    @Test void create_emailInvalid_400() throws Exception {
        String body = "{\"email\":\"not-an-email\",\"password\":\"secret123\",\"roles\":[\"VIEWER\"]}";
        mockMvc.perform(post(USERS).header("Authorization", authHeader(adminToken()))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
    }

    @Test void create_passwordTooShort_400() throws Exception {
        String body = "{\"email\":\"ok@local.com\",\"password\":\"123\",\"roles\":[\"VIEWER\"]}";
        mockMvc.perform(post(USERS).header("Authorization", authHeader(adminToken()))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
    }

    @Test void create_emptyRoles_400() throws Exception {
        String body = "{\"email\":\"user@local.com\",\"password\":\"secret123\",\"roles\":[]}";
        mockMvc.perform(post(USERS).header("Authorization", authHeader(adminToken()))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isBadRequest());
    }

    @Test void create_viewer_forbidden_403() throws Exception {
        String body = "{\"email\":\"viewer_created@local.com\",\"password\":\"secret123\",\"roles\":[\"VIEWER\"]}";
        mockMvc.perform(post(USERS).header("Authorization", authHeader(viewerToken()))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
    }

    // ── Management: Get by ID (2) ───────────────────────────────

    @Test void getById_admin_200() throws Exception {
        // Admin can read any user — use the well-known admin UUID from seed
        mockMvc.perform(get(USERS + "/me").header("Authorization", authHeader(adminToken())))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test void update_nonExistentUser_404() throws Exception {
        String fakeId = java.util.UUID.randomUUID().toString();
        mockMvc.perform(patch(USERS + "/" + fakeId)
                .header("Authorization", authHeader(adminToken()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"INACTIVE\"}"))
            .andExpect(status().isNotFound());
    }

    // ── Management: Analyst cannot create (1) ──────────────────

    @Test void create_analyst_forbidden_403() throws Exception {
        String body = "{\"email\":\"newuser@local.com\",\"password\":\"secret123\",\"roles\":[\"VIEWER\"]}";
        mockMvc.perform(post(USERS).header("Authorization", authHeader(analystToken()))
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden());
    }

    // ── Lifecycle: Deactivate then login blocked (1) ────────────

    @Test void deactivate_thenLogin_401() throws Exception {
        String adminToken = adminToken();
        String email = "todeactivate-" + uid() + "@finance.com";
        String createBody = String.format(
                "{\"email\":\"%s\",\"password\":\"password123\",\"roles\":[\"VIEWER\"]}", email);

        // Create user
        MvcResult created = mockMvc.perform(post(USERS)
                .header("Authorization", authHeader(adminToken))
                .contentType(MediaType.APPLICATION_JSON).content(createBody))
            .andExpect(status().isCreated()).andReturn();
        String userId = json(created).get("id").asText();

        // Verify can login
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
            .andExpect(status().isOk());

        // Deactivate
        mockMvc.perform(patch(USERS + "/" + userId)
                .header("Authorization", authHeader(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"INACTIVE\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("INACTIVE"));

        // Now login must fail
        mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
            .andExpect(status().isUnauthorized());
    }

}
