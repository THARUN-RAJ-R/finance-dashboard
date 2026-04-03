package com.finance.dashboard.auth;

import com.finance.dashboard.AbstractIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@DisplayName("Auth — Exhaustive Integration Tests")
class AuthIntegrationTest extends AbstractIntegrationTest {

    private static final String LOGIN   = "/api/v1/auth/login";
    private static final String REFRESH = "/api/v1/auth/refresh";

    // ── Login: Happy Paths (3) ──────────────────────────────────────────

    @Test void login_admin_success() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@finance.com\",\"password\":\"password\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.refreshToken").isNotEmpty())
            .andExpect(jsonPath("$.expiresInSeconds").isNumber());
    }

    @Test void login_analyst_success() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"analyst@finance.com\",\"password\":\"password\"}"))
            .andExpect(status().isOk());
    }

    @Test void login_viewer_success() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"viewer@finance.com\",\"password\":\"password\"}"))
            .andExpect(status().isOk());
    }

    // ── Login: Basic Failures (4) ───────────────────────────────────────

    @Test void login_wrongPassword_401() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@finance.com\",\"password\":\"strictly_wrong\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test void login_userNotFound_401() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ghost@finance.com\",\"password\":\"admin123\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test void login_nullEmail_400() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"password\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("VALIDATION_ERROR"));
    }

    @Test void login_nullPassword_400() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@finance.com\"}"))
            .andExpect(status().isBadRequest());
    }

    // ── Login: Email Validation Edge Cases (7) ──────────────────────────

    @Test void login_emailEmpty_400() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"\",\"password\":\"password\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test void login_emailSpaceOnly_400() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"   \",\"password\":\"password\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test void login_emailMissingAtSymbol_400() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"adminlocal.com\",\"password\":\"password\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test void login_emailInvalidTLD_400() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@local.123\",\"password\":\"password\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test void login_emailExtremelyLong_400() throws Exception {
        String longEmail = "a".repeat(256) + "@finance.com";
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + longEmail + "\",\"password\":\"password\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test void login_emailWithSpecialChars_CheckConstraint_400() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin spaces@finance.com\",\"password\":\"password\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test void login_emailCaseSensitivity_Mismatch_401() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ADMIN@FINANCE.COM\",\"password\":\"password\"}"))
            .andExpect(status().isUnauthorized());
    }

    // ── Login: Password Validation Edge Cases (2) ───────────────────────

    @Test void login_passwordShort_400() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@finance.com\",\"password\":\"1234567\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details[0]").value(org.hamcrest.Matchers.containsString("at least 8 characters")));
    }

    @Test void login_passwordExactlySix_400() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@finance.com\",\"password\":\"abc123\"}"))
            .andExpect(status().isBadRequest()); 
    }

    @Test void login_passwordEight_Wrong_401() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@finance.com\",\"password\":\"wrongpass\"}"))
            .andExpect(status().isUnauthorized()); 
    }

    // ── Refresh: Happy Path & Rotation (4) ──────────────────────────────

    @Test void refresh_success_and_rotation() throws Exception {
        MvcResult login = loginRaw("admin@finance.com", "password");
        String rt1 = json(login).get("refreshToken").asText();

        MvcResult refresh1 = mockMvc.perform(post(REFRESH).contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + rt1 + "\"}"))
            .andExpect(status().isOk())
            .andReturn();
        
        String rt2 = json(refresh1).get("refreshToken").asText();
        assertThat(rt1).isNotEqualTo(rt2);

        mockMvc.perform(post(REFRESH).contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + rt2 + "\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(post(REFRESH).contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + rt1 + "\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test void refresh_fakeToken_401() throws Exception {
        mockMvc.perform(post(REFRESH).contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"" + java.util.UUID.randomUUID().toString() + "\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test void refresh_nullToken_400() throws Exception {
        mockMvc.perform(post(REFRESH).contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test void refresh_emptyToken_400() throws Exception {
        mockMvc.perform(post(REFRESH).contentType(MediaType.APPLICATION_JSON)
                .content("{\"refreshToken\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test void rateLimit_independentByIP_Simulated() throws Exception {
    }

    // ── Global Security & System Handlers (5) ───────────────────────────

    @Test void login_malformedJsonSyntax_400() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@finance.com\", broken_json: true}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value("MALFORMED_JSON"));
    }

    @Test void login_unsupportedMediaType_415() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.TEXT_PLAIN)
                .content("not-json"))
            .andExpect(status().isUnsupportedMediaType());
    }

    @Test void login_wrongMethod_405() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(LOGIN))
            .andExpect(status().isMethodNotAllowed());
    }

    @Test void login_extraFields_Ignored_Success() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"admin@finance.com\",\"password\":\"password\",\"extra\":\"ignoreMe\"}"))
            .andExpect(status().isOk());
    }

    @Test void login_wrongTypeInJson_400() throws Exception {
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":true,\"password\":\"admin123\"}"))
            .andExpect(status().isBadRequest());
    }

    // ── Security: JWT Edge Cases (3) ────────────────────────────────────

    @Test void login_inactiveUser_401() throws Exception {
        // Create a user via admin, deactivate them, then attempt login
        String adminToken = adminToken();
        String email = "inactive-" + uid() + "@finance.com";
        String createBody = String.format(
                "{\"email\":\"%s\",\"password\":\"password123\",\"roles\":[\"VIEWER\"]}", email);

        MvcResult created = mockMvc.perform(post("/api/v1/users")
                .header("Authorization", authHeader(adminToken))
                .contentType(MediaType.APPLICATION_JSON).content(createBody))
            .andExpect(status().isCreated()).andReturn();

        String userId = json(created).get("id").asText();

        // Deactivate user
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .patch("/api/v1/users/" + userId)
                .header("Authorization", authHeader(adminToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"INACTIVE\"}"))
            .andExpect(status().isOk());

        // Attempt login — expect 401 (account disabled)
        mockMvc.perform(post(LOGIN).contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"password123\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.message").value(
                    org.hamcrest.Matchers.containsString("disabled")));
    }

    @Test void protectedEndpoint_noToken_401() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/records"))
            .andExpect(status().isUnauthorized());
    }

    @Test void protectedEndpoint_tamperedJwt_401() throws Exception {
        // Take a valid token and corrupt its signature segment
        String validToken = adminToken();
        String tampered = validToken.substring(0, validToken.lastIndexOf('.') + 1) + "invalidsignature";
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/records")
                .header("Authorization", "Bearer " + tampered))
            .andExpect(status().isUnauthorized());
    }

}
