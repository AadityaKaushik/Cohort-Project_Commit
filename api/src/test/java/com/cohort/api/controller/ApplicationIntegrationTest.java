package com.cohort.api.controller;

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
class ApplicationIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createListGetUpdateDeleteHappyPath() throws Exception {
        String token = registerAndGetAccessToken("owner-crud@example.com", "Owner");

        String createBody = """
                {"company_name": "Acme Corp", "job_title": "Backend Engineer",
                 "date_applied": "2026-01-10", "status": "APPLIED"}
                """;

        MvcResult createResult = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.job_title", is("Backend Engineer")))
                .andExpect(jsonPath("$.company.name", is("Acme Corp")))
                .andExpect(jsonPath("$.status", is("APPLIED")))
                .andExpect(jsonPath("$.current_stage", is("APPLIED")))
                .andExpect(jsonPath("$.history[0].to_status", is("APPLIED")))
                .andReturn();

        String applicationId = idOf(createResult);

        mockMvc.perform(get("/api/v1/applications").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(1)))
                .andExpect(jsonPath("$.items[0].job_title", is("Backend Engineer")));

        mockMvc.perform(get("/api/v1/applications/" + applicationId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job_title", is("Backend Engineer")));

        String patchBody = """
                {"job_title": "Senior Backend Engineer", "priority": "high"}
                """;
        mockMvc.perform(patch("/api/v1/applications/" + applicationId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job_title", is("Senior Backend Engineer")))
                .andExpect(jsonPath("$.priority", is("high")));

        mockMvc.perform(delete("/api/v1/applications/" + applicationId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/applications/" + applicationId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void createRejectsAFutureDateAppliedWith400() throws Exception {
        String token = registerAndGetAccessToken("future-date@example.com", "Future Dater");

        String body = """
                {"company_name": "Acme", "job_title": "Engineer",
                 "date_applied": "2999-01-01", "status": "APPLIED"}
                """;

        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("VALIDATION_ERROR")));
    }

    @Test
    void createReusesCompanyIgnoringCaseAndWhitespace() throws Exception {
        String token = registerAndGetAccessToken("company-reuse@example.com", "Reuser");

        String first = """
                {"company_name": "Acme Corp", "job_title": "Engineer A",
                 "date_applied": "2026-01-10", "status": "APPLIED"}
                """;
        String second = """
                {"company_name": "  acme corp  ", "job_title": "Engineer B",
                 "date_applied": "2026-01-11", "status": "APPLIED"}
                """;

        MvcResult firstResult = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(first))
                .andExpect(status().isCreated())
                .andReturn();
        MvcResult secondResult = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(second))
                .andExpect(status().isCreated())
                .andReturn();

        String firstCompanyId = objectMapper.readTree(firstResult.getResponse().getContentAsString())
                .get("company").get("id").asText();
        String secondCompanyId = objectMapper.readTree(secondResult.getResponse().getContentAsString())
                .get("company").get("id").asText();

        org.assertj.core.api.Assertions.assertThat(secondCompanyId).isEqualTo(firstCompanyId);
    }

    @Test
    void statusChangeAppliesTheStateMachineAndRecordsHistory() throws Exception {
        String token = registerAndGetAccessToken("status-flow@example.com", "Status Flow");
        String applicationId = createApplication(token, "SAVED");

        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"to_status": "APPLIED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPLIED")));

        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"to_status": "SAVED"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("INVALID_STATUS_TRANSITION")));

        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"to_status": "SAVED", "note": "misclicked, correcting"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("SAVED")));

        mockMvc.perform(get("/api/v1/applications/" + applicationId + "/history")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(3)))
                .andExpect(jsonPath("$[0].to_status", is("SAVED")))
                .andExpect(jsonPath("$[0].from_status", is("APPLIED")))
                .andExpect(jsonPath("$[0].note", is("misclicked, correcting")))
                .andExpect(jsonPath("$[1].to_status", is("APPLIED")))
                .andExpect(jsonPath("$[1].from_status", is("SAVED")))
                .andExpect(jsonPath("$[2].to_status", is("SAVED")))
                .andExpect(jsonPath("$[2].from_status", org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void terminalStatusRejectsAnyFurtherTransitionEvenWithANote() throws Exception {
        String token = registerAndGetAccessToken("terminal@example.com", "Terminal Tester");
        String applicationId = createApplication(token, "APPLIED");

        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"to_status": "REJECTED"}
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"to_status": "APPLIED", "note": "please let me back in"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("INVALID_STATUS_TRANSITION")));
    }

    @Test
    void anotherUserGetsNotFoundForApplication() throws Exception {
        String ownerToken = registerAndGetAccessToken("owner-isolation@example.com", "Owner");
        String intruderToken = registerAndGetAccessToken("intruder-isolation@example.com", "Intruder");
        String applicationId = createApplication(ownerToken, "APPLIED");

        mockMvc.perform(get("/api/v1/applications/" + applicationId).header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("APPLICATION_NOT_FOUND")));

        mockMvc.perform(patch("/api/v1/applications/" + applicationId)
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"job_title": "Hijacked"}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/applications/" + applicationId).header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/status")
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"to_status": "OA"}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/applications/" + applicationId + "/history")
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/applications/" + applicationId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.job_title", is("Test Job")));

        // The intruder's own (empty) list never includes the owner's application.
        mockMvc.perform(get("/api/v1/applications").header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(0)));
    }

    private String createApplication(String token, String status) throws Exception {
        String body = """
                {"company_name": "Test Co", "job_title": "Test Job",
                 "date_applied": "2026-01-10", "status": "%s"}
                """.formatted(status);

        MvcResult result = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        return idOf(result);
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
