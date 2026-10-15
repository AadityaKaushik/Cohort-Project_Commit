package com.cohort.api.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/** US-27..31: input rules on interviews, feedback and company notes. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class InterviewInputValidationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;
    private String applicationId;
    private String companyId;

    @BeforeEach
    void setUp() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "iv-%s@example.com", "password": "a-genuinely-fine-password", "display_name": "Tester"}
                                """.formatted(System.nanoTime())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(body).get("access_token").asText();
        String application = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company_name": "Acme", "job_title": "Engineer", "date_applied": "%s", "status": "APPLIED"}
                                """.formatted(LocalDate.now())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        applicationId = objectMapper.readTree(application).get("id").asText();
        companyId = objectMapper.readTree(application).get("company").get("id").asText();
    }

    private ResultActions createInterview(int round, String extraJson) throws Exception {
        return mockMvc.perform(post("/api/v1/interviews")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"application_id": "%s", "round_number": %d, "round_type": "TECHNICAL"%s}
                        """.formatted(applicationId, round, extraJson)));
    }

    @Test
    void realInterviewDetailsAreAccepted() throws Exception {
        createInterview(1, """
                , "scheduled_at": "%s", "duration_minutes": 60, "interviewer_name": "Priya  Nair",
                "notes": "Two coding questions on arrays and one on caching."
                """.formatted(Instant.now().plusSeconds(86400 * 3)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.interviewer_name", is("Priya Nair")));
    }

    @Test
    void impossibleTimesAndDurationsAreRejected() throws Exception {
        createInterview(1, ", \"scheduled_at\": \"1970-01-01T00:00:00Z\"")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field_errors.scheduledAt").exists());
        createInterview(2, ", \"scheduled_at\": \"" + Instant.now().plusSeconds(86400L * 800) + "\"")
                .andExpect(status().isBadRequest());
        createInterview(3, ", \"duration_minutes\": 5000")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field_errors.durationMinutes").exists());
    }

    @Test
    void junkNotesAndInterviewerNamesAreRejected() throws Exception {
        createInterview(1, ", \"notes\": \"qwertyuiop\"")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field_errors.notes").exists());
        createInterview(1, ", \"interviewer_name\": \"@@@@\"")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field_errors.interviewerName").exists());
    }

    @Test
    void feedbackFieldsRejectJunkAndKeepShortRealAnswers() throws Exception {
        String interviewId = objectMapper.readTree(createInterview(1, "").andReturn().getResponse().getContentAsString())
                .get("id").asText();
        mockMvc.perform(put("/api/v1/interviews/" + interviewId + "/feedback")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"what_went_well\": \"asdfasdfasdf\", \"is_draft\": false}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field_errors.whatWentWell").exists());
        mockMvc.perform(put("/api/v1/interviews/" + interviewId + "/feedback")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"what_went_well\": \"Explained my approach clearly\","
                                + " \"what_went_poorly\": \"Ran out of time\", \"is_draft\": false}"))
                .andExpect(status().isOk());
    }

    @Test
    void companyNoteRejectsMashButAcceptsOffTopicText() throws Exception {
        String path = "/api/v1/companies/" + companyId + "/notes";
        mockMvc.perform(post(path)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"asdf asdf asdf asdf\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field_errors.body").exists());
        mockMvc.perform(post(path)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"the tea there was not good\"}"))
                .andExpect(status().isCreated());
    }
}
