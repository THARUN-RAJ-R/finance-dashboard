package com.finance.dashboard;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.finance.dashboard.config.TestRedisConfig;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestRedisConfig.class)
public abstract class AbstractIntegrationTest {

    @Autowired protected MockMvc       mockMvc;
    @Autowired protected ObjectMapper  objectMapper;
    @Autowired protected DataSource    dataSource;
    protected String currentTestIp;

    @BeforeEach
    public void setupTest() throws Exception {
        currentTestIp = "192.168.1." + (int)(Math.random() * 254 + 1);
        try (Connection c = dataSource.getConnection(); Statement s = c.createStatement()) {
            s.execute("TRUNCATE TABLE audit_log, financial_records CASCADE;");
            s.execute("DELETE FROM categories WHERE CAST(id AS VARCHAR) NOT LIKE 'a0000000%';");
        }
    }

    protected MvcResult loginRaw(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .header("X-Forwarded-For", currentTestIp)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andReturn();
    }

    protected String obtainToken(String email, String password) throws Exception {
        MvcResult r = loginRaw(email, password);
        if (r.getResponse().getStatus() != 200) {
            throw new RuntimeException("Login failed with status " + r.getResponse().getStatus());
        }
        return json(r).get("accessToken").asText();
    }

    protected String adminToken()   throws Exception { return obtainToken("admin@finance.com",   "password");   }
    protected String analystToken() throws Exception { return obtainToken("analyst@finance.com", "password"); }
    protected String viewerToken()  throws Exception { return obtainToken("viewer@finance.com",  "password");  }

    protected String createRecord(String token, String type, double amount,
                                   String currency, String date) throws Exception {
        String body = String.format(
                "{\"type\":\"%s\",\"amount\":%.2f,\"currency\":\"%s\",\"recordDate\":\"%s\"}",
                type, amount, currency, date);
        MvcResult r = mockMvc.perform(post("/api/v1/records")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(body))
                .andReturn();
        if (r.getResponse().getStatus() != 201) return null;
        return json(r).get("id").asText();
    }

    protected String createCategory(String token, String name) throws Exception {
        MvcResult r = mockMvc.perform(post("/api/v1/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content("{\"name\":\"" + name + "\"}"))
                .andReturn();
        if (r.getResponse().getStatus() != 201) return null;
        return json(r).get("id").asText();
    }

    protected JsonNode json(MvcResult r) throws Exception {
        return objectMapper.readTree(r.getResponse().getContentAsString());
    }

    protected String jsonField(MvcResult r, String field) throws Exception {
        return json(r).get(field).asText();
    }

    protected String uid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    protected String authHeader(String token) {
        return "Bearer " + token;
    }
}
