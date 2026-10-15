package com.cohort.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class InterviewIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createListGetUpdateDeleteHappyPath() throws Exception {
        String token = registerAndGetAccessToken("interview-owner-crud@example.com", "Owner");
        String applicationId = createApplication(token);

        MvcResult createResult = mockMvc.perform(post("/api/v1/interviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"application_id": "%s", "round_number": 1, "round_type": "PHONE_SCREEN",
                                 "mode": "video", "interviewer_name": "Jane Recruiter"}
                                """.formatted(applicationId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.round_number", is(1)))
                .andExpect(jsonPath("$.round_type", is("PHONE_SCREEN")))
                .andExpect(jsonPath("$.mode", is("video")))
                .andExpect(jsonPath("$.outcome", is("pending")))
                .andExpect(jsonPath("$.has_feedback", is(false)))
                .andReturn();

        String interviewId = idOf(createResult);

        mockMvc.perform(get("/api/v1/interviews").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(1)))
                .andExpect(jsonPath("$.items[0].round_type", is("PHONE_SCREEN")))
                .andExpect(jsonPath("$.items[0].job_title", is("Test Job")));

        mockMvc.perform(get("/api/v1/interviews/" + interviewId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interviewer_name", is("Jane Recruiter")));

        mockMvc.perform(patch("/api/v1/interviews/" + interviewId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"outcome": "passed", "notes": "Went well"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.outcome", is("passed")))
                .andExpect(jsonPath("$.notes", is("Went well")));

        mockMvc.perform(get("/api/v1/applications/" + applicationId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.interviews", hasSize(1)))
                .andExpect(jsonPath("$.interviews[0].round_number", is(1)))
                .andExpect(jsonPath("$.interviews[0].outcome", is("passed")));

        mockMvc.perform(delete("/api/v1/interviews/" + interviewId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/interviews/" + interviewId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("INTERVIEW_NOT_FOUND")));
    }

    @Test
    void creatingAnInterviewAgainstAForeignOrMissingApplicationReturns404() throws Exception {
        String ownerToken = registerAndGetAccessToken("interview-app-owner@example.com", "Owner");
        String intruderToken = registerAndGetAccessToken("interview-app-intruder@example.com", "Intruder");
        String applicationId = createApplication(ownerToken);

        mockMvc.perform(post("/api/v1/interviews")
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"application_id": "%s", "round_number": 1, "round_type": "OA"}
                                """.formatted(applicationId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("APPLICATION_NOT_FOUND")));
    }

    @Test
    void roundNumberMustBeUniquePerApplication() throws Exception {
        String token = registerAndGetAccessToken("interview-round-unique@example.com", "Owner");
        String applicationId = createApplication(token);
        createInterview(token, applicationId, 1, "OA");

        mockMvc.perform(post("/api/v1/interviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"application_id": "%s", "round_number": 1, "round_type": "TECHNICAL"}
                                """.formatted(applicationId)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("DUPLICATE_ROUND_NUMBER")));

        mockMvc.perform(post("/api/v1/interviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"application_id": "%s", "round_number": 2, "round_type": "TECHNICAL"}
                                """.formatted(applicationId)))
                .andExpect(status().isCreated());
    }

    @Test
    void recordingAndEditingQuestionsWithinAnInterview() throws Exception {
        String token = registerAndGetAccessToken("interview-questions@example.com", "Owner");
        String applicationId = createApplication(token);
        String interviewId = createInterview(token, applicationId, 1, "TECHNICAL");

        MvcResult createResult = mockMvc.perform(post("/api/v1/interviews/" + interviewId + "/questions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_text": "Reverse a linked list", "category": "dsa",
                                 "difficulty": "medium", "self_rating": 4}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.question_text", is("Reverse a linked list")))
                .andExpect(jsonPath("$.category", is("dsa")))
                .andReturn();

        String questionId = idOf(createResult);

        mockMvc.perform(get("/api/v1/interviews/" + interviewId + "/questions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(patch("/api/v1/interview-questions/" + questionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"self_rating": 2, "notes": "Struggled with edge cases"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.self_rating", is(2)))
                .andExpect(jsonPath("$.notes", is("Struggled with edge cases")));

        mockMvc.perform(delete("/api/v1/interview-questions/" + questionId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/interviews/" + interviewId + "/questions").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void feedbackUpsertSupportsADraftThenAFinalSubmission() throws Exception {
        String token = registerAndGetAccessToken("interview-feedback@example.com", "Owner");
        String applicationId = createApplication(token);
        String interviewId = createInterview(token, applicationId, 1, "BEHAVIOURAL");

        mockMvc.perform(get("/api/v1/interviews/" + interviewId + "/feedback").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("INTERVIEW_FEEDBACK_NOT_FOUND")));

        mockMvc.perform(put("/api/v1/interviews/" + interviewId + "/feedback")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"what_went_well": "Communicated clearly", "is_draft": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_draft", is(true)))
                .andExpect(jsonPath("$.submitted_at").doesNotExist());

        mockMvc.perform(get("/api/v1/interviews/" + interviewId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.has_feedback", is(true)));

        mockMvc.perform(put("/api/v1/interviews/" + interviewId + "/feedback")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"self_rating": 4, "what_went_well": "Communicated clearly",
                                 "what_went_poorly": "Rambled on one answer", "is_draft": false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_draft", is(false)))
                .andExpect(jsonPath("$.self_rating", is(4)))
                .andExpect(jsonPath("$.submitted_at").exists());

        mockMvc.perform(get("/api/v1/interviews/" + interviewId + "/feedback").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.what_went_poorly", is("Rambled on one answer")));
    }

    @Test
    void anotherUserGetsNotFoundForInterviewQuestionsOrFeedback() throws Exception {
        String ownerToken = registerAndGetAccessToken("interview-owner-isolation@example.com", "Owner");
        String intruderToken = registerAndGetAccessToken("interview-intruder-isolation@example.com", "Intruder");
        String applicationId = createApplication(ownerToken);
        String interviewId = createInterview(ownerToken, applicationId, 1, "HR");

        MvcResult questionResult = mockMvc.perform(post("/api/v1/interviews/" + interviewId + "/questions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_text": "Tell me about yourself", "category": "hr"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String questionId = idOf(questionResult);

        mockMvc.perform(put("/api/v1/interviews/" + interviewId + "/feedback")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"self_rating": 3, "is_draft": false}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/interviews/" + interviewId).header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("INTERVIEW_NOT_FOUND")));

        mockMvc.perform(patch("/api/v1/interviews/" + interviewId)
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"notes": "Hijacked"}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/interviews/" + interviewId).header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/interviews/" + interviewId + "/questions").header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(patch("/api/v1/interview-questions/" + questionId)
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"notes": "Hijacked"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("INTERVIEW_QUESTION_NOT_FOUND")));

        mockMvc.perform(delete("/api/v1/interview-questions/" + questionId).header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/interviews/" + interviewId + "/feedback").header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("INTERVIEW_NOT_FOUND")));

        mockMvc.perform(put("/api/v1/interviews/" + interviewId + "/feedback")
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"self_rating": 1, "is_draft": false}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/interviews/" + interviewId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/interviews").header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(0)));
    }

    private String createInterview(String token, String applicationId, int roundNumber, String roundType) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/interviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"application_id": "%s", "round_number": %d, "round_type": "%s"}
                                """.formatted(applicationId, roundNumber, roundType)))
                .andExpect(status().isCreated())
                .andReturn();

        return idOf(result);
    }

    @Test
    void calendarExportReturnsAnIcsFileAndHidesForeignInterviews() throws Exception {
        String ownerToken = registerAndGetAccessToken("interview-ics-owner@example.com", "Owner");
        String intruderToken = registerAndGetAccessToken("interview-ics-intruder@example.com", "Intruder");
        String applicationId = createApplication(ownerToken);

        MvcResult created = mockMvc.perform(post("/api/v1/interviews")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"application_id": "%s", "round_number": 1, "round_type": "TECHNICAL",
                                 "scheduled_at": "%s"}
                                """.formatted(applicationId, Instant.now().plus(Duration.ofDays(3)))))
                .andExpect(status().isCreated())
                .andReturn();
        String interviewId = idOf(created);

        MvcResult single = mockMvc.perform(get("/api/v1/interviews/" + interviewId + "/calendar.ics")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        String body = single.getResponse().getContentAsString();
        Assertions.assertTrue(body.contains("SUMMARY:Test Co interview\\, round 1"));
        Assertions.assertTrue(body.contains("TRIGGER:-PT30M"));

        mockMvc.perform(get("/api/v1/interviews/" + interviewId + "/calendar.ics")
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());

        String upcoming = mockMvc.perform(get("/api/v1/calendar/upcoming.ics")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Assertions.assertTrue(upcoming.contains("UID:interview-" + interviewId + "@cohort"));

        String foreign = mockMvc.perform(get("/api/v1/calendar/upcoming.ics")
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        Assertions.assertFalse(foreign.contains("VEVENT"));
    }

    private String createApplication(String token) throws Exception {
        String body = """
                {"company_name": "Test Co", "job_title": "Test Job",
                 "date_applied": "2026-01-10", "status": "APPLIED"}
                """;

        MvcResult result = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        return idOf(result);
    }

    private String idOf(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asText();
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

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("access_token").asText();
    }
}
