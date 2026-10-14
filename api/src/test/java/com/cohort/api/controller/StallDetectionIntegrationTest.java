package com.cohort.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.sql.Timestamp;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class StallDetectionIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void flagsAnApplicationThatHasSatInStageLongerThanTheSystemDefault() throws Exception {
        String token = registerAndGetAccessToken("stall-flagged@example.com", "Owner");
        String applicationId = createApplication(token, "Acme");
        backdateStageEnteredAt(applicationId, 20);

        mockMvc.perform(get("/api/v1/applications/stalled").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].company_name", is("Acme")))
                .andExpect(jsonPath("$[0].current_stage", is("APPLIED")))
                .andExpect(jsonPath("$[0].basis", is("system_default")))
                .andExpect(jsonPath("$[0].threshold_days", is(14.0)));
    }

    @Test
    void doesNotFlagAFreshApplicationWellWithinTheDefault() throws Exception {
        String token = registerAndGetAccessToken("stall-fresh@example.com", "Owner");
        createApplication(token, "Acme");

        mockMvc.perform(get("/api/v1/applications/stalled").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void aSecondUserNeverSeesAnotherUsersStalledApplications() throws Exception {
        String ownerToken = registerAndGetAccessToken("stall-owner@example.com", "Owner");
        String applicationId = createApplication(ownerToken, "Acme");
        backdateStageEnteredAt(applicationId, 20);

        String otherToken = registerAndGetAccessToken("stall-other@example.com", "Other");

        mockMvc.perform(get("/api/v1/applications/stalled").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/v1/applications/stalled").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private void backdateStageEnteredAt(String applicationId, int daysAgo) {
        Instant backdated = Instant.now().minus(daysAgo, ChronoUnit.DAYS);
        jdbcTemplate.update(
                "UPDATE applications SET stage_entered_at = ? WHERE id = ?::uuid",
                Timestamp.from(backdated), applicationId);
    }

    private String createApplication(String token, String companyName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company_name": "%s", "job_title": "Engineer", "date_applied": "%s", "status": "APPLIED"}
                                """.formatted(companyName, java.time.LocalDate.now())))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String registerAndGetAccessToken(String email, String displayName) throws Exception {
        String registerBody = """
                {"email": "%s", "password": "a-genuinely-fine-password", "display_name": "%s"}
                """.formatted(email, displayName);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("access_token").asText();
    }
}
