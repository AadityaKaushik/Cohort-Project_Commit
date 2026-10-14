package com.cohort.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class ReadinessIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void scoreIsSuppressedUntilTwoMappedTopicsHaveEvidence() throws Exception {
        String token = register("readiness-thin@example.com");
        String applicationId = createApplication(token, "We use arrays and strings daily.");

        JsonNode thin = readiness(token, applicationId);
        assertThat(thin.get("sufficient_data").asBoolean()).isFalse();
        assertThat(thin.get("score").isNull()).isTrue();
        assertThat(thin.get("topics").size()).isEqualTo(2);

        logAttempts(token, "Arrays");
        JsonNode oneTopic = readiness(token, applicationId);
        assertThat(oneTopic.get("sample_size").asInt()).isEqualTo(1);
        assertThat(oneTopic.get("score").isNull()).isTrue();

        logAttempts(token, "Strings");
        JsonNode enough = readiness(token, applicationId);
        assertThat(enough.get("sufficient_data").asBoolean()).isTrue();
        assertThat(enough.get("score").asDouble()).isGreaterThan(0.8);
    }

    @Test
    void anotherUsersApplicationIsNotFound() throws Exception {
        String owner = register("readiness-owner@example.com");
        String other = register("readiness-other@example.com");
        String applicationId = createApplication(owner, "arrays");

        mockMvc.perform(get("/api/v1/applications/" + applicationId + "/readiness")
                        .header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());
    }

    private void logAttempts(String token, String topicName) throws Exception {
        String topicId = null;
        for (JsonNode topic : objectMapper.readTree(mockMvc.perform(get("/api/v1/topics")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString())) {
            if (topic.get("name").asText().equals(topicName)) {
                topicId = topic.get("id").asText();
            }
        }
        JsonNode question = objectMapper.readTree(mockMvc.perform(get("/api/v1/questions?size=1&topic_id=" + topicId)
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString()).get("items").get(0);
        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/v1/questions/" + question.get("id").asText() + "/attempts")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"status\": \"in_progress\", \"confidence\": 5}"))
                    .andExpect(status().isCreated());
        }
    }

    private JsonNode readiness(String token, String applicationId) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get("/api/v1/applications/" + applicationId + "/readiness")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private String createApplication(String token, String description) throws Exception {
        String body = objectMapper.writeValueAsString(java.util.Map.of(
                "company_name", "Acme", "job_title", "Engineer", "job_description", description,
                "date_applied", LocalDate.now().toString(), "status", "APPLIED"));
        String response = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private String register(String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "a-genuinely-fine-password", "display_name": "Reader"}
                                """.formatted(email)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("access_token").asText();
    }
}
