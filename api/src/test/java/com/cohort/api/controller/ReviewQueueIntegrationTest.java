package com.cohort.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class ReviewQueueIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/prep/review-queue")).andExpect(status().isUnauthorized());
    }

    @Test
    void topicPractisedLongAgoShowsUpAndAFreshAttemptClearsIt() throws Exception {
        String token = register("review-flow@example.com");
        String questionId = firstQuestionId(token);

        attempt(token, questionId, 2, "2026-01-01T10:00:00Z");
        JsonNode queue = queue(token);
        assertThat(queue.get("items")).hasSize(1);
        assertThat(queue.get("items").get(0).get("question_id").asText()).isEqualTo(questionId);
        assertThat(queue.get("items").get(0).get("reason").asText()).startsWith("Last practised ");

        attempt(token, questionId, 5, java.time.Instant.now().toString());
        assertThat(queue(token).get("items")).isEmpty();
    }

    @Test
    void anotherUsersAttemptsNeverAppearInMyQueue() throws Exception {
        String owner = register("review-owner@example.com");
        String other = register("review-other@example.com");

        attempt(owner, firstQuestionId(owner), 2, "2026-01-01T10:00:00Z");

        assertThat(queue(owner).get("items")).hasSize(1);
        assertThat(queue(other).get("items")).isEmpty();
    }

    private JsonNode queue(String token) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get("/api/v1/prep/review-queue")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private String firstQuestionId(String token) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get("/api/v1/questions?size=1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString())
                .get("items").get(0).get("id").asText();
    }

    private void attempt(String token, String questionId, int confidence, String attemptedAt) throws Exception {
        mockMvc.perform(post("/api/v1/questions/" + questionId + "/attempts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "in_progress", "confidence": %d, "attempted_at": "%s"}
                                """.formatted(confidence, attemptedAt)))
                .andExpect(status().isCreated());
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "a-genuinely-fine-password", "display_name": "Review"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("access_token").asText();
    }
}
