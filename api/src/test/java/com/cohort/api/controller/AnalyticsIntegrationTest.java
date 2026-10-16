package com.cohort.api.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class AnalyticsIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void funnelAndConversionsMatchAHandComputedPipeline() throws Exception {
        String token = registerAndGetAccessToken("analytics-funnel@example.com", "Owner");
        LocalDate today = LocalDate.now();

        String app1 = createApplication(token, "Acme", today);
        changeStatus(token, app1, "OA", null);
        changeStatus(token, app1, "INTERVIEW", null);
        changeStatus(token, app1, "REJECTED", null);

        String app2 = createApplication(token, "Globex", today);
        changeStatus(token, app2, "OA", null);
        changeStatus(token, app2, "INTERVIEW", null);
        changeStatus(token, app2, "FINAL_ROUND", null);
        changeStatus(token, app2, "OFFER", null);
        changeStatus(token, app2, "ACCEPTED", null);

        createApplication(token, "Initech", today);

        // app4: never applied at all (Saved), excluded from "applied."
        createSavedApplication(token, "Hooli", today);

        mockMvc.perform(get("/api/v1/analytics/funnel").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied", is(3)))
                .andExpect(jsonPath("$.reached_oa", is(2)))
                .andExpect(jsonPath("$.reached_interview", is(2)))
                .andExpect(jsonPath("$.reached_final", is(1)))
                .andExpect(jsonPath("$.reached_offer", is(1)))
                .andExpect(jsonPath("$.sample_size", is(4)));

        mockMvc.perform(get("/api/v1/analytics/conversions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application_to_oa.value", is(1.0)))
                .andExpect(jsonPath("$.application_to_oa.numerator", is(2)))
                .andExpect(jsonPath("$.application_to_oa.denominator", is(2)))
                .andExpect(jsonPath("$.application_to_oa.sufficient_data", is(false)))
                .andExpect(jsonPath("$.oa_to_interview.value", is(1.0)))
                .andExpect(jsonPath("$.interview_to_final.value", is(0.5)))
                .andExpect(jsonPath("$.final_to_offer.value", is(1.0)))
                .andExpect(jsonPath("$.overall_success.value", is(0.5)))
                .andExpect(jsonPath("$.overall_success.numerator", is(1)))
                .andExpect(jsonPath("$.overall_success.denominator", is(2)));
    }

    @Test
    void timelineBucketsTodaysApplicationsIntoTheCurrentWeek() throws Exception {
        String token = registerAndGetAccessToken("analytics-timeline@example.com", "Owner");
        LocalDate today = LocalDate.now();
        createApplication(token, "Acme", today);
        createApplication(token, "Globex", today);

        mockMvc.perform(get("/api/v1/analytics/timeline")
                        .header("Authorization", "Bearer " + token)
                        .param("from", today.minusDays(1).toString())
                        .param("to", today.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sample_size", is(2)));
    }

    @Test
    void rejectionDistributionSeparatesStagesAndNeverCountsStillOpenApplications() throws Exception {
        String token = registerAndGetAccessToken("analytics-rejection@example.com", "Owner");
        LocalDate today = LocalDate.now();

        String appRejectedAtOa = createApplication(token, "Acme", today);
        changeStatus(token, appRejectedAtOa, "OA", null);
        changeStatus(token, appRejectedAtOa, "REJECTED", null);

        String appRejectedAtInterview = createApplication(token, "Globex", today);
        changeStatus(token, appRejectedAtInterview, "OA", null);
        changeStatus(token, appRejectedAtInterview, "INTERVIEW", null);
        changeStatus(token, appRejectedAtInterview, "REJECTED", null);

        // Still open at Applied, so not a rejection.
        createApplication(token, "Initech", today);

        mockMvc.perform(get("/api/v1/analytics/rejections").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.by_stage.OA", is(1)))
                .andExpect(jsonPath("$.by_stage.INTERVIEW", is(1)))
                .andExpect(jsonPath("$.by_stage.FINAL_ROUND").doesNotExist())
                .andExpect(jsonPath("$.no_response_count", is(0)))
                .andExpect(jsonPath("$.sample_size", is(2)))
                .andExpect(jsonPath("$.sufficient_data", is(false)));
    }

    @Test
    void dwellTimesReportsCompletedTransitionsAndCensoredCountsSeparately() throws Exception {
        String token = registerAndGetAccessToken("analytics-dwell@example.com", "Owner");
        LocalDate today = LocalDate.now();

        String app = createApplication(token, "Acme", today);
        changeStatus(token, app, "OA", null);

        mockMvc.perform(get("/api/v1/analytics/dwell-times").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.by_stage.APPLIED.completed_count", is(1)))
                .andExpect(jsonPath("$.by_stage.APPLIED.sufficient_data", is(false)))
                .andExpect(jsonPath("$.by_stage.OA.censored_count", is(1)))
                .andExpect(jsonPath("$.by_stage.OA.completed_count", is(0)));
    }

    @Test
    void aSecondUserSeesOnlyItsOwnEmptyAnalytics() throws Exception {
        String ownerToken = registerAndGetAccessToken("analytics-owner@example.com", "Owner");
        createApplication(ownerToken, "Acme", LocalDate.now());
        changeStatus(ownerToken, firstApplicationId(ownerToken), "OA", null);

        String otherToken = registerAndGetAccessToken("analytics-other@example.com", "Other");

        mockMvc.perform(get("/api/v1/analytics/funnel").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applied", is(0)))
                .andExpect(jsonPath("$.reached_oa", is(0)))
                .andExpect(jsonPath("$.sample_size", is(0)));

        mockMvc.perform(get("/api/v1/analytics/conversions").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.application_to_oa.numerator", is(0)))
                .andExpect(jsonPath("$.application_to_oa.denominator", is(0)));

        mockMvc.perform(get("/api/v1/analytics/rejections").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sample_size", is(0)))
                .andExpect(jsonPath("$.no_response_count", is(0)));

        mockMvc.perform(get("/api/v1/analytics/dwell-times").header("Authorization", "Bearer " + otherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.by_stage.OA.completed_count", is(0)))
                .andExpect(jsonPath("$.by_stage.OA.censored_count", is(0)));
    }

    private String createApplication(String token, String companyName, LocalDate dateApplied) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company_name": "%s", "job_title": "Engineer", "date_applied": "%s", "status": "APPLIED"}
                                """.formatted(companyName, dateApplied)))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private String createSavedApplication(String token, String companyName, LocalDate dateApplied) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company_name": "%s", "job_title": "Engineer", "date_applied": "%s", "status": "SAVED"}
                                """.formatted(companyName, dateApplied)))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private void changeStatus(String token, String applicationId, String toStatus, String note) throws Exception {
        String body = note == null
                ? """
                        {"to_status": "%s"}
                        """.formatted(toStatus)
                : """
                        {"to_status": "%s", "note": "%s"}
                        """.formatted(toStatus, note);
        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());
    }

    private String firstApplicationId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/applications").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("items").get(0).get("id").asText();
    }

    private String idOf(MvcResult result) throws Exception {
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
