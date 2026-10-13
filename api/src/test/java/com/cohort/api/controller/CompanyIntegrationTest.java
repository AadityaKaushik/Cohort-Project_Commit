package com.cohort.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** US-96: the aggregated company page, its notes, and per-user isolation of both. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class CompanyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void companyPageAggregatesApplicationsContactsAndNotes() throws Exception {
        String token = register("company-page@example.com");
        String applicationId = createApplication(token, "Globex", "Backend Engineer");
        createApplication(token, "Globex", "Platform Engineer");
        createApplication(token, "Initech", "Engineer");
        String companyId = getJson(token, "/api/v1/applications/" + applicationId).get("company").get("id").asText();

        mockMvc.perform(post("/api/v1/interviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"application_id": "%s", "round_number": 1, "round_type": "TECHNICAL", "scheduled_at": "%s"}
                                """.formatted(applicationId, Instant.now().plusSeconds(86400 * 5))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/contacts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Gina Globex\", \"company_id\": \"%s\"}".formatted(companyId)))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/companies/" + companyId + "/notes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"Ask about the on-call rotation\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.body", is("Ask about the on-call rotation")));

        mockMvc.perform(get("/api/v1/companies/" + companyId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Globex")))
                .andExpect(jsonPath("$.stats.applications", is(2)))
                .andExpect(jsonPath("$.stats.active", is(2)))
                .andExpect(jsonPath("$.stats.interviews", is(1)))
                .andExpect(jsonPath("$.applications", hasSize(2)))
                .andExpect(jsonPath("$.contacts", hasSize(1)))
                .andExpect(jsonPath("$.notes", hasSize(1)));

        mockMvc.perform(get("/api/v1/companies").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name", is("Globex")))
                .andExpect(jsonPath("$[0].application_count", is(2)))
                .andExpect(jsonPath("$[0].contact_count", is(1)));
        mockMvc.perform(get("/api/v1/companies?q=init").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(patch("/api/v1/companies/" + companyId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"industry\": \"Logistics\", \"website\": \"https://globex.example\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.industry", is("Logistics")));
    }

    @Test
    void notesCanBeEditedAndDeleted() throws Exception {
        String token = register("company-notes@example.com");
        String applicationId = createApplication(token, "Hooli", "Engineer");
        String companyId = getJson(token, "/api/v1/applications/" + applicationId).get("company").get("id").asText();
        String body = mockMvc.perform(post("/api/v1/companies/" + companyId + "/notes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"first\"}"))
                .andReturn().getResponse().getContentAsString();
        String noteId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(put("/api/v1/companies/" + companyId + "/notes/" + noteId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"second\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.body", is("second")));
        mockMvc.perform(post("/api/v1/companies/" + companyId + "/notes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \" \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(delete("/api/v1/companies/" + companyId + "/notes/" + noteId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/companies/" + companyId).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.notes", hasSize(0)));
    }

    @Test
    void anotherUserCannotSeeOrChangeSomeoneElsesCompanyOrNotes() throws Exception {
        String owner = register("company-iso-owner@example.com");
        String intruder = register("company-iso-intruder@example.com");
        String applicationId = createApplication(owner, "Umbrella", "Engineer");
        String companyId = getJson(owner, "/api/v1/applications/" + applicationId).get("company").get("id").asText();
        String body = mockMvc.perform(post("/api/v1/companies/" + companyId + "/notes")
                        .header("Authorization", "Bearer " + owner)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"private\"}"))
                .andReturn().getResponse().getContentAsString();
        String noteId = objectMapper.readTree(body).get("id").asText();

        mockMvc.perform(get("/api/v1/companies/" + companyId).header("Authorization", "Bearer " + intruder))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("COMPANY_NOT_FOUND")));
        mockMvc.perform(patch("/api/v1/companies/" + companyId)
                        .header("Authorization", "Bearer " + intruder)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"industry\": \"x\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/companies/" + companyId + "/notes")
                        .header("Authorization", "Bearer " + intruder)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"x\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(put("/api/v1/companies/" + companyId + "/notes/" + noteId)
                        .header("Authorization", "Bearer " + intruder)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"x\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("COMPANY_NOTE_NOT_FOUND")));
        mockMvc.perform(delete("/api/v1/companies/" + companyId + "/notes/" + noteId)
                        .header("Authorization", "Bearer " + intruder))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/companies").header("Authorization", "Bearer " + intruder))
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/v1/companies")).andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/companies/" + companyId).header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$.notes[0].body", is("private")));
    }

    private JsonNode getJson(String token, String path) throws Exception {
        String body = mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private String createApplication(String token, String companyName, String title) throws Exception {
        String body = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company_name": "%s", "job_title": "%s", "date_applied": "%s", "status": "APPLIED"}
                                """.formatted(companyName, title, LocalDate.now())))
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
        return objectMapper.readTree(body).get("access_token").asText();
    }
}
