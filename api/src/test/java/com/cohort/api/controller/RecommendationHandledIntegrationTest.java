package com.cohort.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

/** P78: accepting is visible and idempotent, and snoozed/dismissed items can be reviewed and restored. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class RecommendationHandledIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void acceptingATopicRecommendationCreatesOneTaskAndShowsUpAsHandled() throws Exception {
        String token = register("handled-accept@example.com");
        weakenFirstTopic(token);
        JsonNode topicRecommendation = firstTopicRecommendation(token);

        String id = topicRecommendation.get("id").asText();
        accept(token, id).andExpect(jsonPath("$.status", is("accepted")))
                .andExpect(jsonPath("$.accepted_task_id", notNullValue()))
                .andExpect(jsonPath("$.suppressed_until", notNullValue()));
        accept(token, id).andExpect(jsonPath("$.status", is("accepted")));

        mockMvc.perform(get("/api/v1/preparation/tasks").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/v1/recommendations/handled").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("accepted")));
        mockMvc.perform(get("/api/v1/recommendations").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[*].status", everyItem(is("active"))));
        assertThat(firstTopicRecommendationOrNull(token)).isNull();
    }

    @Test
    void snoozedAndDismissedItemsAreListedAndCanBeRestored() throws Exception {
        String token = register("handled-restore@example.com");
        String id = firstId(token);

        mockMvc.perform(post("/api/v1/recommendations/" + id + "/dismiss")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\": \"not now\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/recommendations").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/v1/recommendations/handled").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("dismissed")))
                .andExpect(jsonPath("$[0].dismissed_reason", is("not now")))
                .andExpect(jsonPath("$[0].suppressed_until", notNullValue()));

        mockMvc.perform(post("/api/v1/recommendations/" + id + "/restore").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("active")));

        mockMvc.perform(get("/api/v1/recommendations").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.length()", is(1)));
        mockMvc.perform(get("/api/v1/recommendations/handled").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void anotherUserCannotSeeOrRestoreMyHandledRecommendations() throws Exception {
        String owner = register("handled-owner@example.com");
        String other = register("handled-other@example.com");
        String id = firstId(owner);
        mockMvc.perform(post("/api/v1/recommendations/" + id + "/snooze").header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/recommendations/" + id + "/restore").header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/recommendations/handled").header("Authorization", "Bearer " + other))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private org.springframework.test.web.servlet.ResultActions accept(String token, String id) throws Exception {
        return mockMvc.perform(post("/api/v1/recommendations/" + id + "/accept").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    private void weakenFirstTopic(String token) throws Exception {
        JsonNode question = objectMapper.readTree(mockMvc.perform(get("/api/v1/questions?size=1")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString()).get("items").get(0);
        for (int i = 0; i < 3; i++) {
            mockMvc.perform(post("/api/v1/questions/" + question.get("id").asText() + "/attempts")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\": \"needs_review\", \"confidence\": 1}"))
                    .andExpect(status().isCreated());
        }
    }

    private JsonNode firstTopicRecommendation(String token) throws Exception {
        JsonNode found = firstTopicRecommendationOrNull(token);
        assertThat(found).as("a recommendation targeting a topic").isNotNull();
        return found;
    }

    private JsonNode firstTopicRecommendationOrNull(String token) throws Exception {
        JsonNode all = objectMapper.readTree(mockMvc.perform(get("/api/v1/recommendations")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString());
        for (JsonNode recommendation : all) {
            if (!recommendation.get("target_topic_id").isNull()) {
                return recommendation;
            }
        }
        return null;
    }

    private String firstId(String token) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get("/api/v1/recommendations")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString()).get(0).get("id").asText();
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "a-genuinely-fine-password", "display_name": "Handled"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("access_token").asText();
    }
}
