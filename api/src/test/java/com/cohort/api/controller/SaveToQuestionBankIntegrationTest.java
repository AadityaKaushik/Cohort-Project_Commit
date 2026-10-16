package com.cohort.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
class SaveToQuestionBankIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void savesTheAskedQuestionIntoTheUsersOwnBankOnce() throws Exception {
        String token = register();
        String askedId = askedQuestion(token, null);
        String topicId = topicId("graphs");

        MvcResult created = save(token, askedId, Map.of("topic_id", topicId));
        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        JsonNode copy = objectMapper.readTree(created.getResponse().getContentAsString());
        assertThat(copy.get("question_text").asText()).isEqualTo("Find the shortest route between two stations on a metro map");
        assertThat(copy.get("topic_id").asText()).isEqualTo(topicId);
        assertThat(copy.get("is_system").asBoolean()).isFalse();
        assertThat(copy.get("difficulty").asText()).isEqualTo("medium");

        MvcResult again = save(token, askedId, Map.of());
        assertThat(again.getResponse().getStatus()).isEqualTo(200);
        assertThat(objectMapper.readTree(again.getResponse().getContentAsString()).get("id").asText())
                .isEqualTo(copy.get("id").asText());

        JsonNode asked = getJson(token, "/api/v1/interviews/" + interviewIdOf(askedId) + "/questions").get(0);
        assertThat(asked.get("linked_question_id").asText()).isEqualTo(copy.get("id").asText());
        assertThat(asked.get("topic_id").asText()).isEqualTo(topicId);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM questions WHERE source_question_id IS NULL AND user_id = (SELECT user_id FROM interview_questions WHERE id = ?::uuid)",
                Integer.class, askedId)).isEqualTo(1);
    }

    @Test
    void usesTheTopicAlreadyOnTheAskedQuestion() throws Exception {
        String token = register();
        String topicId = topicId("hashing");
        String askedId = askedQuestion(token, topicId);

        MvcResult created = save(token, askedId, null);

        assertThat(created.getResponse().getStatus()).isEqualTo(201);
        assertThat(objectMapper.readTree(created.getResponse().getContentAsString()).get("topic_id").asText()).isEqualTo(topicId);
    }

    @Test
    void asksForATopicWhenNoneIsKnown() throws Exception {
        String token = register();
        String askedId = askedQuestion(token, null);

        MvcResult result = save(token, askedId, Map.of());

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(objectMapper.readTree(result.getResponse().getContentAsString())
                .get("error").get("field_errors").get("topicId").asText()).isEqualTo("Pick a topic for this question.");
    }

    @Test
    void theSavedCopyCanBePractisedLikeAnyOtherQuestion() throws Exception {
        String token = register();
        String askedId = askedQuestion(token, topicId("graphs"));
        String copyId = objectMapper.readTree(save(token, askedId, null).getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/questions/" + copyId + "/attempts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"needs_review\", \"confidence\": 2}"))
                .andExpect(status().isCreated());
    }

    @Test
    void anotherUsersQuestionsStayHidden() throws Exception {
        String owner = register();
        String other = register();
        String askedId = askedQuestion(owner, topicId("graphs"));
        String copyId = objectMapper.readTree(save(owner, askedId, null).getResponse().getContentAsString()).get("id").asText();

        assertThat(save(other, askedId, Map.of("topic_id", topicId("graphs"))).getResponse().getStatus()).isEqualTo(404);
        mockMvc.perform(get("/api/v1/questions/" + copyId).header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());
    }

    @Test
    void requiresSignIn() throws Exception {
        mockMvc.perform(post("/api/v1/interview-questions/" + UUID.randomUUID() + "/save-to-bank"))
                .andExpect(status().isUnauthorized());
    }

    private MvcResult save(String token, String askedId, Map<String, Object> body) throws Exception {
        var request = post("/api/v1/interview-questions/" + askedId + "/save-to-bank").header("Authorization", "Bearer " + token);
        if (body != null) {
            request.contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(body));
        }
        return mockMvc.perform(request).andReturn();
    }

    private String askedQuestion(String token, String topicId) throws Exception {
        String applicationId = send(token, "/api/v1/applications", Map.of(
                "company_name", "Metro Systems", "job_title", "Engineer",
                "date_applied", LocalDate.now().toString(), "status", "APPLIED")).get("id").asText();
        String interviewId = send(token, "/api/v1/interviews", Map.of(
                "application_id", applicationId, "round_number", 1, "round_type", "TECHNICAL")).get("id").asText();
        Map<String, Object> question = new HashMap<>();
        question.put("question_text", "Find the shortest route between two stations on a metro map");
        question.put("category", "dsa");
        question.put("difficulty", "medium");
        if (topicId != null) {
            question.put("topic_id", topicId);
        }
        return send(token, "/api/v1/interviews/" + interviewId + "/questions", question).get("id").asText();
    }

    private String interviewIdOf(String askedId) {
        return jdbcTemplate.queryForObject("SELECT interview_id::text FROM interview_questions WHERE id = ?::uuid", String.class, askedId);
    }

    private String topicId(String slug) {
        return jdbcTemplate.queryForObject("SELECT id::text FROM topics WHERE slug = ?", String.class, slug);
    }

    private JsonNode getJson(String token, String path) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private JsonNode send(String token, String path, Map<String, Object> body) throws Exception {
        String response = mockMvc.perform(post(path)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private String register() throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "bank-%s@example.com", "password": "a-genuinely-fine-password", "display_name": "Tester"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("access_token").asText();
    }
}
