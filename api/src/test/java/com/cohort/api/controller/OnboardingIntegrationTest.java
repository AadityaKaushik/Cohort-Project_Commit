package com.cohort.api.controller;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
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
class OnboardingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void newUserStartsNotOnboardedAndCompletingItSeedsAStarterPlan() throws Exception {
        String token = register("onboard-new@example.com");

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.onboarded", is(false)));

        String targetDate = LocalDate.now().plusDays(30).toString();
        mockMvc.perform(post("/api/v1/users/me/onboarding")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"target_role\": \"Backend Engineer\", \"target_date\": \"%s\"}".formatted(targetDate)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks_created", greaterThan(0)))
                .andExpect(jsonPath("$.profile.onboarded", is(true)))
                .andExpect(jsonPath("$.profile.target_role", is("Backend Engineer")))
                .andExpect(jsonPath("$.profile.target_date", is(targetDate)));

        mockMvc.perform(get("/api/v1/preparation/tasks").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThan(0)))
                .andExpect(jsonPath("$[0].due_date", lessThanOrEqualTo(targetDate)));
    }

    @Test
    void completingOnboardingTwiceDoesNotSeedDuplicateTasks() throws Exception {
        String token = register("onboard-twice@example.com");
        String body = "{\"target_role\": \"Data Engineer\"}";

        mockMvc.perform(post("/api/v1/users/me/onboarding").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk());
        int firstCount = objectMapper.readTree(mockMvc.perform(get("/api/v1/preparation/tasks")
                        .header("Authorization", "Bearer " + token))
                .andReturn().getResponse().getContentAsString()).size();

        mockMvc.perform(post("/api/v1/users/me/onboarding").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks_created", is(0)));

        mockMvc.perform(get("/api/v1/preparation/tasks").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(firstCount)));
    }

    @Test
    void skippingCompletesOnboardingWithoutTasks() throws Exception {
        String token = register("onboard-skip@example.com");

        mockMvc.perform(post("/api/v1/users/me/onboarding").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"skip\": true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tasks_created", is(0)))
                .andExpect(jsonPath("$.profile.onboarded", is(true)));

        mockMvc.perform(get("/api/v1/preparation/tasks").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void anotherUsersTasksAreNeverSeededOrVisible() throws Exception {
        String owner = register("onboard-owner@example.com");
        String other = register("onboard-other@example.com");

        mockMvc.perform(post("/api/v1/users/me/onboarding").header("Authorization", "Bearer " + owner)
                .contentType(MediaType.APPLICATION_JSON).content("{\"target_role\": \"SRE\"}")).andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/preparation/tasks").header("Authorization", "Bearer " + other))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "a-genuinely-fine-password", "display_name": "Onboard"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("access_token").asText();
    }
}
