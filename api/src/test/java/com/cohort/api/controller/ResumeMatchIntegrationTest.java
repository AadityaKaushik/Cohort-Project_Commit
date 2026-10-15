package com.cohort.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.cohort.api.service.ai.AiTestConfig;
import com.cohort.api.service.ai.StubAiProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import({AbstractIntegrationTest.ContainerConfig.class, AiTestConfig.class})
class ResumeMatchIntegrationTest extends AbstractIntegrationTest {
    private static final String POSTING = "We build REST APIs in Java with Spring Boot on PostgreSQL. Docker and Kafka are a plus.";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StubAiProvider provider;

    @BeforeEach
    void reset() {
        provider.reset();
    }

    @Test
    void matchListsMatchedAndMissingSkillsWithBullets() throws Exception {
        String token = register("match-owner@example.com");
        String applicationId = applicationWithResume(token, "Acme", POSTING,
                "Built a Java service with Spring Boot\nTended the office plants");

        JsonNode match = getJson("/api/v1/applications/" + applicationId + "/match", token);

        assertThat(texts(match.get("matched"))).contains("Java", "Spring Boot");
        assertThat(texts(match.get("missing"))).contains("Docker", "Kafka").doesNotContain("Java");
        assertThat(match.get("score").asInt()).isBetween(1, 99);
        assertThat(match.get("explanation").asText()).contains("Docker");
        assertThat(match.get("bullets").size()).isEqualTo(1);
        assertThat(match.get("bullets").get(0).get("text").asText()).startsWith("Built a Java service");
    }

    @Test
    void matchWithoutResumeExplainsWhatIsMissing() throws Exception {
        String token = register("match-noresume@example.com");
        String applicationId = createApplication(token, "Acme", POSTING);

        JsonNode match = getJson("/api/v1/applications/" + applicationId + "/match", token);

        assertThat(match.get("has_resume").asBoolean()).isFalse();
        assertThat(match.get("score").isNull()).isTrue();
        assertThat(match.get("explanation").asText()).contains("Attach a resume");
    }

    @Test
    void anotherUsersApplicationIsNotFound() throws Exception {
        String owner = register("match-iso-owner@example.com");
        String other = register("match-iso-other@example.com");
        String applicationId = createApplication(owner, "Acme", POSTING);

        mockMvc.perform(get("/api/v1/applications/" + applicationId + "/match").header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/cover-letter-draft")
                        .header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());
    }

    @Test
    void coverLetterUsesTheModelReplyWhenItIsUsable() throws Exception {
        String token = register("match-letter@example.com");
        String applicationId = applicationWithResume(token, "Acme", POSTING, "Built a Java service with Spring Boot");
        String reply = "Dear hiring team, I would like to apply for the Engineer role at Acme. I have built a Java service "
                + "with Spring Boot and would enjoy bringing that to your team. Kind regards, [Your name]";
        provider.replyWith(reply);

        JsonNode draft = postJson("/api/v1/applications/" + applicationId + "/cover-letter-draft", token);

        assertThat(draft.get("source").asText()).isEqualTo("model");
        assertThat(draft.get("draft").asText()).isEqualTo(reply);
    }

    @Test
    void coverLetterFallsBackToTemplateWhenProviderIsDown() throws Exception {
        String token = register("match-down@example.com");
        String applicationId = applicationWithResume(token, "Acme", POSTING, "Built a Java service with Spring Boot");
        provider.goDown();

        JsonNode draft = postJson("/api/v1/applications/" + applicationId + "/cover-letter-draft", token);

        assertThat(draft.get("ai_available").asBoolean()).isFalse();
        assertThat(draft.get("source").asText()).isEqualTo("template");
        assertThat(draft.get("draft").asText()).contains("Engineer role at Acme").contains("Built a Java service");
    }

    @Test
    void replyWithInventedNumbersIsRejected() throws Exception {
        String token = register("match-numbers@example.com");
        String applicationId = applicationWithResume(token, "Acme", POSTING, "Built a Java service with Spring Boot");
        provider.replyWith("Dear hiring team, with 12 years of Java and Spring Boot I would be a strong fit for the role. "
                + "Kind regards, [Your name]");

        JsonNode draft = postJson("/api/v1/applications/" + applicationId + "/cover-letter-draft", token);

        assertThat(draft.get("source").asText()).isEqualTo("template");
        assertThat(draft.get("ai_available").asBoolean()).isTrue();
    }

    @Test
    void promptNeverContainsAnotherUsersRecords() throws Exception {
        String other = register("match-ground-other@example.com");
        applicationWithResume(other, "Zorbex Industries", "Zorbex wants Rust and Haskell.", "Secret Zorbex migration project in Java");
        String token = register("match-ground-owner@example.com");
        String applicationId = applicationWithResume(token, "Acme", POSTING, "Built a Java service with Spring Boot");
        provider.replyWith("Dear hiring team, thanks for considering me for the role. Kind regards, [Your name]");

        postJson("/api/v1/applications/" + applicationId + "/cover-letter-draft", token);

        assertThat(provider.everythingSent()).contains("Acme", "Built a Java service")
                .doesNotContain("Zorbex").doesNotContain("Haskell");
    }

    private JsonNode getJson(String path, String token) throws Exception {
        return objectMapper.readTree(mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private JsonNode postJson(String path, String token) throws Exception {
        return objectMapper.readTree(mockMvc.perform(post(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    private java.util.List<String> texts(JsonNode array) {
        java.util.List<String> values = new java.util.ArrayList<>();
        array.forEach(node -> values.add(node.asText()));
        return values;
    }

    private String applicationWithResume(String token, String company, String posting, String experience) throws Exception {
        String applicationId = createApplication(token, company, posting);
        String resumeId = send("/api/v1/resumes", token, Map.of("name", "Main")).get("id").asText();
        send("/api/v1/resumes/" + resumeId + "/sections", token,
                Map.of("section_type", "experience", "title", "Developer", "description", experience));
        send("/api/v1/applications/" + applicationId + "/resume", token, Map.of("resume_id", resumeId));
        return applicationId;
    }

    private String createApplication(String token, String company, String posting) throws Exception {
        return send("/api/v1/applications", token, Map.of(
                "company_name", company, "job_title", "Engineer", "job_description", posting,
                "date_applied", LocalDate.now().toString(), "status", "APPLIED")).get("id").asText();
    }

    private JsonNode send(String path, String token, Map<String, Object> body) throws Exception {
        String response = mockMvc.perform(post(path)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(200, 201))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private String register(String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "a-genuinely-fine-password", "display_name": "Tester"}
                                """.formatted(email)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("access_token").asText();
    }
}
