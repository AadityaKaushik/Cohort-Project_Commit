package com.cohort.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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
class MockInterviewIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/prep/mock-interview")).andExpect(status().isUnauthorized());
    }

    @Test
    void defaultsToFiveQuestionsFromDifferentTopics() throws Exception {
        String token = register("mock-default@example.com");

        JsonNode questions = mockInterview(token, "").get("questions");

        assertThat(questions).hasSize(5);
        assertThat(questions.findValuesAsText("topic_id")).doesNotHaveDuplicates();
        assertThat(questions.get(0).get("reason").asText()).isEqualTo("Not practised yet");
        assertThat(questions.get(0).get("mastery_score").isNull()).isTrue();
    }

    @Test
    void countOutsideTheRangeIsAFieldError() throws Exception {
        String token = register("mock-range@example.com");

        for (String count : List.of("2", "11")) {
            mockMvc.perform(get("/api/v1/prep/mock-interview?count=" + count)
                            .header("Authorization", "Bearer " + token))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.error.field_errors.count").exists());
        }
    }

    @Test
    void anotherUsersOwnQuestionIsNeverPicked() throws Exception {
        String owner = register("mock-owner@example.com");
        String other = register("mock-other@example.com");
        JsonNode firstTopic = firstTopicByName(other);
        String privateText = "000 private question of the owner";
        mockMvc.perform(post("/api/v1/questions")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"topic_id": "%s", "question_text": "%s", "category": "technical"}
                                """.formatted(firstTopic.get("id").asText(), privateText)))
                .andExpect(status().isCreated());

        List<String> ownerTexts = mockInterview(owner, "?count=10").get("questions").findValuesAsText("question_text");
        List<String> otherTexts = mockInterview(other, "?count=10").get("questions").findValuesAsText("question_text");

        assertThat(ownerTexts).contains(privateText);
        assertThat(otherTexts).doesNotContain(privateText);
    }

    private JsonNode firstTopicByName(String token) throws Exception {
        JsonNode topics = objectMapper.readTree(mockMvc.perform(get("/api/v1/topics")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
        List<JsonNode> sorted = new ArrayList<>();
        topics.forEach(sorted::add);
        sorted.sort(Comparator.comparing(topic -> topic.get("name").asText()));
        return sorted.get(0);
    }

    private JsonNode mockInterview(String token, String query) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get("/api/v1/prep/mock-interview" + query)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "a-genuinely-fine-password", "display_name": "Mock"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("access_token").asText();
    }
}
