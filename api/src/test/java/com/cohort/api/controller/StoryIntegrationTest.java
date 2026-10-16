package com.cohort.api.controller;

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
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class StoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void storyLifecycle() throws Exception {
        String token = register("story-crud@example.com");
        String id = createStory(token, """
                {"title": "Led the billing migration", "situation": "Billing ran on a legacy cron job.",
                 "task": "Move it to the new queue before quarter end.", "action": "Split the work and wrote the rollout plan.",
                 "result": "Cut failed invoices by 40 percent.", "competencies": ["leadership", "impact"]}
                """);

        mockMvc.perform(get("/api/v1/stories/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Led the billing migration")))
                .andExpect(jsonPath("$.competencies", hasSize(2)))
                .andExpect(jsonPath("$.interview_id").doesNotExist());

        mockMvc.perform(patch("/api/v1/stories/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Led the billing move\", \"competencies\": [\"ownership\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Led the billing move")))
                .andExpect(jsonPath("$.competencies[0]", is("ownership")))
                .andExpect(jsonPath("$.result", is("Cut failed invoices by 40 percent.")));

        mockMvc.perform(delete("/api/v1/stories/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/stories/" + id).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void listIsNewestFirstAndFiltersByCompetency() throws Exception {
        String token = register("story-filter@example.com");
        createStory(token, "{\"title\": \"First\", \"competencies\": [\"teamwork\"]}");
        createStory(token, "{\"title\": \"Second\", \"competencies\": [\"teamwork\", \"conflict\"]}");
        createStory(token, "{\"title\": \"Third\", \"competencies\": [\"failure\"]}");

        mockMvc.perform(get("/api/v1/stories").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].title", is("Third")))
                .andExpect(jsonPath("$[2].title", is("First")));
        mockMvc.perform(get("/api/v1/stories?competency=teamwork").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].title", is("Second")));
        mockMvc.perform(get("/api/v1/stories?competency=deadline").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void validationErrorsAreReportedPerField() throws Exception {
        String token = register("story-validation@example.com");

        postStory(token, "{\"title\": \" \"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field_errors.title").exists());
        postStory(token, "{\"title\": \"%s\"}".formatted("a".repeat(201)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field_errors.title").exists());
        postStory(token, "{\"title\": \"Ok\", \"result\": \"%s\"}".formatted("word ".repeat(1001)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field_errors.result").exists());
        postStory(token, "{\"title\": \"Ok\", \"situation\": \"zzzzzzzzzz\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field_errors.situation").exists());
        postStory(token, "{\"title\": \"Ok\", \"competencies\": [\"charisma\"]}")
                .andExpect(status().isBadRequest());
    }

    @Test
    void anInterviewLinkMustBelongToTheCaller() throws Exception {
        String owner = register("story-link-owner@example.com");
        String intruder = register("story-link-intruder@example.com");
        String interviewId = createInterview(owner);

        String linked = createStory(owner, "{\"title\": \"Linked\", \"interview_id\": \"%s\"}".formatted(interviewId));
        mockMvc.perform(get("/api/v1/stories/" + linked).header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$.interview_id", is(interviewId)));

        postStory(intruder, "{\"title\": \"Sneaky\", \"interview_id\": \"%s\"}".formatted(interviewId))
                .andExpect(status().isNotFound());
        String own = createStory(intruder, "{\"title\": \"Mine\"}");
        mockMvc.perform(patch("/api/v1/stories/" + own)
                        .header("Authorization", "Bearer " + intruder)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"interview_id\": \"%s\"}".formatted(interviewId)))
                .andExpect(status().isNotFound());
        postStory(owner, "{\"title\": \"Missing\", \"interview_id\": \"%s\"}".formatted(UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    @Test
    void anotherUserCanNeitherReadChangeNorDeleteAStory() throws Exception {
        String owner = register("story-iso-owner@example.com");
        String intruder = register("story-iso-intruder@example.com");
        String id = createStory(owner, "{\"title\": \"Private story\", \"competencies\": [\"learning\"]}");

        mockMvc.perform(get("/api/v1/stories/" + id).header("Authorization", "Bearer " + intruder))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("STORY_NOT_FOUND")));
        mockMvc.perform(patch("/api/v1/stories/" + id)
                        .header("Authorization", "Bearer " + intruder)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\": \"Hijacked\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/stories/" + id).header("Authorization", "Bearer " + intruder))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/stories").header("Authorization", "Bearer " + intruder))
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/v1/stories")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/stories/" + id).header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$.title", is("Private story")));
    }

    private ResultActions postStory(String token, String json) throws Exception {
        return mockMvc.perform(post("/api/v1/stories")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private String createStory(String token, String json) throws Exception {
        String body = postStory(token, json).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String createInterview(String token) throws Exception {
        String applicationBody = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company_name": "Acme", "job_title": "Engineer", "date_applied": "%s", "status": "APPLIED"}
                                """.formatted(LocalDate.now())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String applicationId = objectMapper.readTree(applicationBody).get("id").asText();
        String body = mockMvc.perform(post("/api/v1/interviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"application_id": "%s", "round_number": 1, "round_type": "BEHAVIOURAL"}
                                """.formatted(applicationId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String register(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "a-genuinely-fine-password", "display_name": "Tester"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return json.get("access_token").asText();
    }
}
