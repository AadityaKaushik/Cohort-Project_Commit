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

/** Logging a practice attempt must move the topic mastery the progress view reports. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class PracticeMasteryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void loggingAttemptsMovesTheTopicMasteryAndUnlocksEvidence() throws Exception {
        String token = register("mastery-flow@example.com");
        JsonNode question = objectMapper.readTree(mockMvc.perform(get("/api/v1/questions?size=1")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString()).get("items").get(0);
        String questionId = question.get("id").asText();
        String topicId = question.get("topic_id").asText();

        JsonNode before = topic(token, topicId);
        assertThat(before.get("observation_count").asInt()).isZero();
        assertThat(before.get("sufficient_evidence").asBoolean()).isFalse();

        attempt(token, questionId, 5);
        JsonNode afterOne = topic(token, topicId);
        assertThat(afterOne.get("observation_count").asInt()).isEqualTo(1);
        assertThat(afterOne.get("sufficient_evidence").asBoolean()).isFalse();

        attempt(token, questionId, 5);
        JsonNode afterTwo = topic(token, topicId);
        assertThat(afterTwo.get("sufficient_evidence").asBoolean()).isTrue();
        double strong = afterTwo.get("mastery_score").asDouble();
        assertThat(strong).isGreaterThan(0.9);

        attempt(token, questionId, 1);
        attempt(token, questionId, 1);
        double weakened = topic(token, topicId).get("mastery_score").asDouble();
        assertThat(weakened).isLessThan(strong);
    }

    @Test
    void anotherUsersAttemptsNeverMoveMyMastery() throws Exception {
        String owner = register("mastery-owner@example.com");
        String other = register("mastery-other@example.com");
        JsonNode question = objectMapper.readTree(mockMvc.perform(get("/api/v1/questions?size=1")
                        .header("Authorization", "Bearer " + owner))
                .andReturn().getResponse().getContentAsString()).get("items").get(0);

        attempt(owner, question.get("id").asText(), 5);
        attempt(owner, question.get("id").asText(), 5);

        JsonNode mine = topic(other, question.get("topic_id").asText());
        assertThat(mine.get("observation_count").asInt()).isZero();
    }

    private void attempt(String token, String questionId, int confidence) throws Exception {
        mockMvc.perform(post("/api/v1/questions/" + questionId + "/attempts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"in_progress\", \"confidence\": %d}".formatted(confidence)))
                .andExpect(status().isCreated());
    }

    private JsonNode topic(String token, String topicId) throws Exception {
        JsonNode progress = objectMapper.readTree(mockMvc.perform(get("/api/v1/preparation/progress")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        for (JsonNode topic : progress.get("topics")) {
            if (topic.get("topic_id").asText().equals(topicId)) {
                return topic;
            }
        }
        throw new AssertionError("topic missing from progress view");
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "a-genuinely-fine-password", "display_name": "Mastery"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("access_token").asText();
    }
}
