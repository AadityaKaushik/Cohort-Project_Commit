package com.cohort.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class SprintOneJourneyIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerLoginCreateApplicationTransitionStatusAndReadHistory() throws Exception {
        String email = "journey@example.com";
        String password = "a-genuinely-fine-password";

        String registerBody = """
                {"email": "%s", "password": "%s", "display_name": "Journey Tester"}
                """.formatted(email, password);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email", is(email)));

        String loginBody = """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andReturn();

        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("access_token").asText();

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.display_name", is("Journey Tester")))
                .andExpect(jsonPath("$.fl_opt_in", is(false)));

        String createBody = """
                {"company_name": "Journey Co", "job_title": "Platform Engineer",
                 "date_applied": "2026-01-05", "status": "SAVED"}
                """;

        MvcResult createResult = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("SAVED")))
                .andExpect(jsonPath("$.current_stage", is("SAVED")))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String applicationId = created.get("id").asText();

        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/status")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"to_status": "APPLIED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPLIED")))
                .andExpect(jsonPath("$.current_stage", is("APPLIED")));

        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/status")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"to_status": "INTERVIEW"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("INTERVIEW")));

        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/status")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"to_status": "ACCEPTED"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("INVALID_STATUS_TRANSITION")));

        mockMvc.perform(get("/api/v1/applications/" + applicationId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("INTERVIEW")))
                .andExpect(jsonPath("$.history", org.hamcrest.Matchers.hasSize(3)));

        MvcResult historyResult = mockMvc.perform(get("/api/v1/applications/" + applicationId + "/history")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(3)))
                .andExpect(jsonPath("$[0].to_status", is("INTERVIEW")))
                .andExpect(jsonPath("$[0].from_status", is("APPLIED")))
                .andExpect(jsonPath("$[1].to_status", is("APPLIED")))
                .andExpect(jsonPath("$[1].from_status", is("SAVED")))
                .andExpect(jsonPath("$[2].to_status", is("SAVED")))
                .andExpect(jsonPath("$[2].from_status", org.hamcrest.Matchers.nullValue()))
                .andReturn();

        JsonNode history = objectMapper.readTree(historyResult.getResponse().getContentAsString());
        assertThat(history).hasSize(3);
    }
}
