package com.cohort.api.controller;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Exercises /topics/*, /questions/* and /questions/{id}/attempts (US-35..38)
 * against a real Postgres instance seeded by V12-V15: browsing the shared
 * taxonomy and question bank, authoring/forking/deleting a personal
 * question, recording append-only practice attempts, and — the mandatory
 * check (CONTRIBUTING.md, FR-95) — that practice attempts and personal
 * questions are isolated per user (404, never 403).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class PreparationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void topicsAreSeededAndFormAGraph() throws Exception {
        String token = registerAndGetAccessToken("prep-topics@example.com", "Owner");

        mockMvc.perform(get("/api/v1/topics").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(40))));

        mockMvc.perform(get("/api/v1/topics/graph").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nodes", hasSize(greaterThanOrEqualTo(40))))
                .andExpect(jsonPath("$.edges", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    void questionBankIsSeededAndFilterable() throws Exception {
        String token = registerAndGetAccessToken("prep-questions@example.com", "Owner");

        mockMvc.perform(get("/api/v1/questions").header("Authorization", "Bearer " + token)
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", greaterThanOrEqualTo(150)));

        mockMvc.perform(get("/api/v1/questions").header("Authorization", "Bearer " + token)
                        .param("category", "dsa")
                        .param("difficulty", "easy")
                        .param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].category", is("dsa")))
                .andExpect(jsonPath("$.items[0].difficulty", is("easy")));
    }

    @Test
    void creatingUpdatingAndDeletingAPersonalQuestion() throws Exception {
        String token = registerAndGetAccessToken("prep-own-question@example.com", "Owner");
        String topicId = firstTopicId(token);

        MvcResult createResult = mockMvc.perform(post("/api/v1/questions")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topic_id": "%s", "question_text": "What did I get asked at Acme?", "category": "dsa"}
                                """.formatted(topicId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.is_system", is(false)))
                .andExpect(jsonPath("$.question_text", is("What did I get asked at Acme?")))
                .andReturn();

        String questionId = idOf(createResult);

        mockMvc.perform(patch("/api/v1/questions/" + questionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_text": "Edited question text"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question_text", is("Edited question text")));

        mockMvc.perform(delete("/api/v1/questions/" + questionId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/questions/" + questionId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("QUESTION_NOT_FOUND")));
    }

    @Test
    void seededQuestionsAreReadOnlyButCanBeForked() throws Exception {
        String token = registerAndGetAccessToken("prep-fork@example.com", "Owner");
        String systemQuestionId = firstSystemQuestionId(token);

        // US-36: "seeded questions are read-only" -- a write against a system question 404s, same as a foreign one.
        mockMvc.perform(patch("/api/v1/questions/" + systemQuestionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_text": "Hijacked"}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/questions/" + systemQuestionId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());

        MvcResult forkResult = mockMvc.perform(post("/api/v1/questions/" + systemQuestionId + "/fork")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.is_system", is(false)))
                .andExpect(jsonPath("$.source_question_id", is(systemQuestionId)))
                .andReturn();

        String forkedId = idOf(forkResult);

        // The fork is a personal question now, so it CAN be edited/deleted.
        mockMvc.perform(patch("/api/v1/questions/" + forkedId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_text": "My edited copy"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.question_text", is("My edited copy")));
    }

    @Test
    void practiceAttemptsAreAppendOnlyAndBuildAConfidenceTrajectory() throws Exception {
        String token = registerAndGetAccessToken("prep-attempts@example.com", "Owner");
        String questionId = firstSystemQuestionId(token);

        mockMvc.perform(post("/api/v1/questions/" + questionId + "/attempts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "in_progress", "confidence": 2, "attempted_at": "2026-01-01T00:00:00Z"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.confidence", is(2)));

        mockMvc.perform(post("/api/v1/questions/" + questionId + "/attempts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "confident", "confidence": 4, "attempted_at": "2026-02-01T00:00:00Z"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.confidence", is(4)));

        // Two attempts, not one overwritten row.
        mockMvc.perform(get("/api/v1/questions/" + questionId + "/attempts").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].confidence", is(4))); // most recent first
    }

    @Test
    void crossUserAccessReturns404NeverExposingAnotherUsersQuestionOrAttempts() throws Exception {
        String ownerToken = registerAndGetAccessToken("prep-owner-isolation@example.com", "Owner");
        String intruderToken = registerAndGetAccessToken("prep-intruder-isolation@example.com", "Intruder");
        String topicId = firstTopicId(ownerToken);

        MvcResult createResult = mockMvc.perform(post("/api/v1/questions")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topic_id": "%s", "question_text": "My private interview question", "category": "hr"}
                                """.formatted(topicId)))
                .andExpect(status().isCreated())
                .andReturn();
        String questionId = idOf(createResult);

        // The owner's personal question is invisible to another user entirely.
        mockMvc.perform(get("/api/v1/questions/" + questionId).header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("QUESTION_NOT_FOUND")));
        mockMvc.perform(patch("/api/v1/questions/" + questionId)
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"question_text": "Hijacked"}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/questions/" + questionId).header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());

        // Recording a practice attempt against a question the intruder cannot see is also 404.
        mockMvc.perform(post("/api/v1/questions/" + questionId + "/attempts")
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "not_started"}
                                """))
                .andExpect(status().isNotFound());

        // Both users can practise the SAME system question; each only ever sees their own attempts.
        String systemQuestionId = firstSystemQuestionId(ownerToken);
        mockMvc.perform(post("/api/v1/questions/" + systemQuestionId + "/attempts")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "confident", "confidence": 5}
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/questions/" + systemQuestionId + "/attempts").header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/v1/questions/" + systemQuestionId + "/attempts").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    private String firstTopicId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/topics").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode topics = objectMapper.readTree(result.getResponse().getContentAsString());
        return topics.get(0).get("id").asText();
    }

    private String firstSystemQuestionId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/questions")
                        .header("Authorization", "Bearer " + token)
                        .param("is_system", "true")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("items").get(0).get("id").asText();
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
