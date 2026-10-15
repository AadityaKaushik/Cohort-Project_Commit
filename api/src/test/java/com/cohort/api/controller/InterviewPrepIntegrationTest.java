package com.cohort.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.cohort.api.service.ai.AiTestConfig;
import com.cohort.api.service.ai.StubAiProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
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
class InterviewPrepIntegrationTest extends AbstractIntegrationTest {
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

    private record Setup(String token, String upcomingInterviewId) {
    }

    @Test
    void packCollectsPastRoundsQuestionsAndCompanyNotes() throws Exception {
        Setup owner = setup("prep-owner@example.com", "Acme", "Review the payments service");

        JsonNode pack = getJson("/api/v1/interviews/" + owner.upcomingInterviewId() + "/prep-pack", owner.token(), 200);

        assertThat(pack.get("company_name").asText()).isEqualTo("Acme");
        assertThat(pack.get("round_number").asInt()).isEqualTo(2);
        assertThat(pack.get("company_notes").get(0).asText()).isEqualTo("Review the payments service");
        assertThat(pack.get("past_rounds").size()).isEqualTo(1);
        assertThat(pack.get("past_rounds").get(0).get("improvement_areas").asText()).contains("explain trade offs");
        assertThat(pack.get("questions_asked").get(0).get("text").asText()).contains("rate limiter");
    }

    @Test
    void anotherUsersInterviewIsNotFound() throws Exception {
        Setup owner = setup("prep-iso-owner@example.com", "Acme", "note");
        String other = register("prep-iso-other@example.com");

        getJson("/api/v1/interviews/" + owner.upcomingInterviewId() + "/prep-pack", other, 404);
        mockMvc.perform(post("/api/v1/interviews/" + owner.upcomingInterviewId() + "/prep-pack/summary")
                        .header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());
    }

    @Test
    void packAndPromptNeverContainAnotherUsersRecordsAtTheSameCompany() throws Exception {
        setup("prep-ground-other@example.com", "Acme", "OTHERUSERNOTE hidden salary talk");
        Setup owner = setup("prep-ground-owner@example.com", "Acme", "Review the payments service");
        provider.replyWith("Round 2 at Acme is a technical interview, so revisit the earlier feedback before you go in.");

        JsonNode pack = getJson("/api/v1/interviews/" + owner.upcomingInterviewId() + "/prep-pack", owner.token(), 200);
        postJson("/api/v1/interviews/" + owner.upcomingInterviewId() + "/prep-pack/summary", owner.token());

        assertThat(pack.toString()).doesNotContain("OTHERUSERNOTE");
        assertThat(provider.everythingSent()).contains("Round 2").doesNotContain("OTHERUSERNOTE");
    }

    @Test
    void summaryFallsBackWhenProviderIsDown() throws Exception {
        Setup owner = setup("prep-down@example.com", "Acme", "note");
        provider.goDown();

        JsonNode summary = postJson("/api/v1/interviews/" + owner.upcomingInterviewId() + "/prep-pack/summary", owner.token());

        assertThat(summary.get("ai_available").asBoolean()).isFalse();
        assertThat(summary.get("source").asText()).isEqualTo("template");
        assertThat(summary.get("draft").asText()).contains("Round 2 at Acme");
    }

    @Test
    void summaryUsesTheModelReply() throws Exception {
        Setup owner = setup("prep-model@example.com", "Acme", "note");
        String reply = "This is round 2 at Acme. Go back over the feedback from round 1 and practise explaining trade offs.";
        provider.replyWith(reply);

        JsonNode summary = postJson("/api/v1/interviews/" + owner.upcomingInterviewId() + "/prep-pack/summary", owner.token());

        assertThat(summary.get("source").asText()).isEqualTo("model");
        assertThat(summary.get("draft").asText()).isEqualTo(reply);
    }

    private Setup setup(String email, String company, String note) throws Exception {
        String token = register(email);
        JsonNode application = send("/api/v1/applications", token, Map.of(
                "company_name", company, "job_title", "Engineer", "job_description", "Java and Docker",
                "date_applied", LocalDate.now().toString(), "status", "APPLIED"));
        String applicationId = application.get("id").asText();
        send("/api/v1/companies/" + application.get("company").get("id").asText() + "/notes", token, Map.of("body", note));

        Map<String, Object> first = new HashMap<>();
        first.put("application_id", applicationId);
        first.put("round_number", 1);
        first.put("round_type", "TECHNICAL");
        first.put("outcome", "passed");
        String pastId = send("/api/v1/interviews", token, first).get("id").asText();
        send("/api/v1/interviews/" + pastId + "/questions", token,
                Map.of("question_text", "Design a rate limiter for an API", "category", "technical"));
        mockMvc.perform(put("/api/v1/interviews/" + pastId + "/feedback")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "improvement_areas", "Practise how to explain trade offs out loud", "is_draft", false))))
                .andExpect(status().isOk());

        Map<String, Object> second = new HashMap<>();
        second.put("application_id", applicationId);
        second.put("round_number", 2);
        second.put("round_type", "TECHNICAL");
        second.put("scheduled_at", Instant.now().plus(3, ChronoUnit.DAYS).toString());
        String upcomingId = send("/api/v1/interviews", token, second).get("id").asText();
        return new Setup(token, upcomingId);
    }

    private JsonNode getJson(String path, String token, int expected) throws Exception {
        String body = mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return body.isBlank() ? null : objectMapper.readTree(body);
    }

    private JsonNode postJson(String path, String token) throws Exception {
        return objectMapper.readTree(mockMvc.perform(post(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
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
