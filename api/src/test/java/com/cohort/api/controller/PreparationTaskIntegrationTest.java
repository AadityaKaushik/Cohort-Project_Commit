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

@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class PreparationTaskIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void creatingListingCompletingAndDeletingATask() throws Exception {
        String token = registerAndGetAccessToken("prep-task-crud@example.com", "Owner");
        String topicId = firstTopicId(token);

        MvcResult createResult = mockMvc.perform(post("/api/v1/preparation/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topic_id": "%s", "title": "Practice 10 array problems", "due_date": "2026-12-01",
                                 "estimated_effort_minutes": 60}
                                """.formatted(topicId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title", is("Practice 10 array problems")))
                .andExpect(jsonPath("$.status", is("open")))
                .andReturn();

        String taskId = idOf(createResult);

        mockMvc.perform(get("/api/v1/preparation/tasks").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(patch("/api/v1/preparation/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "completed"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("completed")))
                .andExpect(jsonPath("$.completed_at").exists());

        mockMvc.perform(delete("/api/v1/preparation/tasks/" + taskId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/preparation/tasks").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void progressViewReturnsCategoryRollupAndTopicGrid() throws Exception {
        String token = registerAndGetAccessToken("prep-progress@example.com", "Owner");

        mockMvc.perform(get("/api/v1/preparation/progress").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.topics", hasSize(greaterThanOrEqualTo(40))))
                .andExpect(jsonPath("$.topics[0].sufficient_evidence", is(false)));
    }

    @Test
    void completingATaskIsReflectedInTheCategoryProgressRollup() throws Exception {
        String token = registerAndGetAccessToken("prep-progress-rollup@example.com", "Owner");
        String topicId = firstTopicId(token);
        String category = topicCategory(token, topicId);

        MvcResult createResult = mockMvc.perform(post("/api/v1/preparation/tasks")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topic_id": "%s", "title": "Review notes"}
                                """.formatted(topicId)))
                .andExpect(status().isCreated())
                .andReturn();
        String taskId = idOf(createResult);

        MvcResult beforeResult = mockMvc.perform(get("/api/v1/preparation/progress").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertTasksCompletedForCategory(beforeResult, category, 0);

        mockMvc.perform(patch("/api/v1/preparation/tasks/" + taskId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "completed"}
                                """))
                .andExpect(status().isOk());

        MvcResult afterResult = mockMvc.perform(get("/api/v1/preparation/progress").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        assertTasksCompletedForCategory(afterResult, category, 1);
    }

    @Test
    void crossUserAccessReturns404NeverExposingAnotherUsersTask() throws Exception {
        String ownerToken = registerAndGetAccessToken("prep-task-owner-isolation@example.com", "Owner");
        String intruderToken = registerAndGetAccessToken("prep-task-intruder-isolation@example.com", "Intruder");
        String topicId = firstTopicId(ownerToken);

        MvcResult createResult = mockMvc.perform(post("/api/v1/preparation/tasks")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topic_id": "%s", "title": "My private task"}
                                """.formatted(topicId)))
                .andExpect(status().isCreated())
                .andReturn();
        String taskId = idOf(createResult);

        mockMvc.perform(patch("/api/v1/preparation/tasks/" + taskId)
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status": "completed"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("PREPARATION_TASK_NOT_FOUND")));

        mockMvc.perform(delete("/api/v1/preparation/tasks/" + taskId).header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/preparation/tasks").header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/v1/preparation/tasks").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    private void assertTasksCompletedForCategory(MvcResult result, String category, int expectedCompleted) throws Exception {
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        for (JsonNode categoryNode : body.get("categories")) {
            if (categoryNode.get("category").asText().equals(category)) {
                org.assertj.core.api.Assertions.assertThat(categoryNode.get("tasks_completed").asInt())
                        .isEqualTo(expectedCompleted);
                return;
            }
        }
        throw new AssertionError("category " + category + " not found in progress response");
    }

    private String topicCategory(String token, String topicId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/topics").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode topics = objectMapper.readTree(result.getResponse().getContentAsString());
        for (JsonNode topic : topics) {
            if (topic.get("id").asText().equals(topicId)) {
                return topic.get("category").asText();
            }
        }
        throw new AssertionError("topic " + topicId + " not found");
    }

    private String firstTopicId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/topics").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode topics = objectMapper.readTree(result.getResponse().getContentAsString());
        return topics.get(0).get("id").asText();
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
