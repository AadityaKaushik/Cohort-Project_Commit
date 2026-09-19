package com.cohort.api.controller;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
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

/**
 * US-24/FR-34: attaching, changing, and clearing the one resume associated
 * with an application via {@code POST /applications/{id}/resume}, plus the
 * mandatory cross-user isolation check (FR-95) — including the case where
 * the application is the caller's own but the referenced resume belongs to
 * someone else, which must still 404 rather than silently succeed or leak
 * whether that resume ID exists.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class ApplicationResumeAssociationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void attachingChangingAndClearingTheResumeAssociationAllSucceedForTheOwner() throws Exception {
        String token = registerAndGetAccessToken("resume-assoc-owner@example.com", "Owner");
        String applicationId = createApplication(token);
        String firstResumeId = createResume(token, "Resume A");
        String secondResumeId = createResume(token, "Resume B");

        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/resume")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resume_id": "%s"}
                                """.formatted(firstResumeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resume_id", is(firstResumeId)));

        // Changing it is allowed.
        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/resume")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resume_id": "%s"}
                                """.formatted(secondResumeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resume_id", is(secondResumeId)));

        // A null resume_id clears the association.
        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/resume")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resume_id": null}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.resume_id", nullValue()));
    }

    @Test
    void attachingAResumeThatBelongsToAnotherUserReturns404() throws Exception {
        String ownerToken = registerAndGetAccessToken("resume-assoc-app-owner@example.com", "Application Owner");
        String intruderToken = registerAndGetAccessToken("resume-assoc-resume-owner@example.com", "Resume Owner");
        String applicationId = createApplication(ownerToken);
        String foreignResumeId = createResume(intruderToken, "Someone Else's Resume");

        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/resume")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resume_id": "%s"}
                                """.formatted(foreignResumeId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("RESUME_NOT_FOUND")));
    }

    @Test
    void attachingAResumeToAnApplicationOwnedByAnotherUserReturns404() throws Exception {
        String ownerToken = registerAndGetAccessToken("resume-assoc-owner-2@example.com", "Owner");
        String intruderToken = registerAndGetAccessToken("resume-assoc-intruder-2@example.com", "Intruder");
        String applicationId = createApplication(ownerToken);
        String intruderResumeId = createResume(intruderToken, "Intruder Resume");

        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/resume")
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"resume_id": "%s"}
                                """.formatted(intruderResumeId)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("APPLICATION_NOT_FOUND")));
    }

    private String createApplication(String token) throws Exception {
        String body = """
                {"company_name": "Test Co", "job_title": "Test Job",
                 "date_applied": "2026-01-10", "status": "APPLIED"}
                """;

        MvcResult result = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();

        return idOf(result);
    }

    private String createResume(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/resumes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "%s"}
                                """.formatted(name)))
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
