package com.cohort.api.controller;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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
class RecommendationIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void aFreshUserAlwaysGetsTheBaselineRecommendation() throws Exception {
        String token = registerAndGetAccessToken("rec-fresh@example.com", "Owner");

        mockMvc.perform(get("/api/v1/recommendations").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].type", is("baseline")))
                .andExpect(jsonPath("$[0].tier", is("D")))
                .andExpect(jsonPath("$[0].explanation").isNotEmpty())
                .andExpect(jsonPath("$[0].evidence", hasSize(0)));
    }

    @Test
    void includesAStalledApplicationRecommendationAlongsideBaseline() throws Exception {
        String token = registerAndGetAccessToken("rec-stalled@example.com", "Owner");
        String applicationId = createApplication(token, "Acme");
        backdateStageEnteredAt(applicationId, 20);

        MvcResult result = mockMvc.perform(get("/api/v1/recommendations").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        boolean hasStalled = false;
        for (JsonNode item : body) {
            if (item.get("type").asText().equals("stalled_application")) {
                hasStalled = true;
                assertThatEvidenceCitesTheApplication(item);
            }
        }
        org.assertj.core.api.Assertions.assertThat(hasStalled).isTrue();
    }

    @Test
    void acceptingATopiclessRecommendationJustMarksItAcceptedWithNoTask() throws Exception {
        String token = registerAndGetAccessToken("rec-accept@example.com", "Owner");
        String recommendationId = firstRecommendationId(token);

        mockMvc.perform(post("/api/v1/recommendations/" + recommendationId + "/accept")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("accepted")))
                .andExpect(jsonPath("$.accepted_task_id").doesNotExist());

        mockMvc.perform(get("/api/v1/recommendations").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].status", everyItem(is("active"))));
    }

    @Test
    void snoozingHidesTheRecommendationAndDoesNotRegenerateItImmediately() throws Exception {
        String token = registerAndGetAccessToken("rec-snooze@example.com", "Owner");
        String recommendationId = firstRecommendationId(token);

        mockMvc.perform(post("/api/v1/recommendations/" + recommendationId + "/snooze")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("snoozed")))
                .andExpect(jsonPath("$.snoozed_until").isNotEmpty());

        mockMvc.perform(get("/api/v1/recommendations").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void dismissingRecordsAReasonAndSuppressesTheTypeForThirtyDays() throws Exception {
        String token = registerAndGetAccessToken("rec-dismiss@example.com", "Owner");
        String recommendationId = firstRecommendationId(token);

        mockMvc.perform(post("/api/v1/recommendations/" + recommendationId + "/dismiss")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "Not relevant right now"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("dismissed")))
                .andExpect(jsonPath("$.dismissed_reason", is("Not relevant right now")));

        mockMvc.perform(get("/api/v1/recommendations").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void dismissWithoutAReasonIsRejected() throws Exception {
        String token = registerAndGetAccessToken("rec-dismiss-no-reason@example.com", "Owner");
        String recommendationId = firstRecommendationId(token);

        mockMvc.perform(post("/api/v1/recommendations/" + recommendationId + "/dismiss")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void crossUserAccessToAnyActionReturns404NeverExposingAnotherUsersRecommendation() throws Exception {
        String ownerToken = registerAndGetAccessToken("rec-owner-isolation@example.com", "Owner");
        String intruderToken = registerAndGetAccessToken("rec-intruder-isolation@example.com", "Intruder");
        String recommendationId = firstRecommendationId(ownerToken);

        mockMvc.perform(post("/api/v1/recommendations/" + recommendationId + "/accept")
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("RECOMMENDATION_NOT_FOUND")));

        mockMvc.perform(post("/api/v1/recommendations/" + recommendationId + "/snooze")
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/recommendations/" + recommendationId + "/dismiss")
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"reason": "not mine to dismiss"}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/recommendations").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("active")));
    }

    private void assertThatEvidenceCitesTheApplication(JsonNode recommendation) {
        org.assertj.core.api.Assertions.assertThat(recommendation.get("evidence").size()).isGreaterThan(0);
        org.assertj.core.api.Assertions.assertThat(recommendation.get("target_application_id").asText()).isNotBlank();
    }

    private String firstRecommendationId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/recommendations").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get(0).get("id").asText();
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
