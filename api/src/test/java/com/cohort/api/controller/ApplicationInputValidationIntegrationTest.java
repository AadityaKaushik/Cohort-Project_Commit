package com.cohort.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
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

/** US-09..15: input rules on application create and update, and the dry-run text check. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class ApplicationInputValidationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String token;

    @BeforeEach
    void registerUser() throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "input-%s@example.com", "password": "a-genuinely-fine-password", "display_name": "Tester"}
                                """.formatted(System.nanoTime())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        token = objectMapper.readTree(body).get("access_token").asText();
    }

    private ResultActions postApplication(String json) throws Exception {
        return mockMvc.perform(post("/api/v1/applications")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private ResultActions create(String extraJson) throws Exception {
        return postApplication("""
                {"company_name": "Acme", "job_title": "Backend Engineer", "date_applied": "%s", "status": "APPLIED"%s}
                """.formatted(LocalDate.now(), extraJson));
    }

    private ResultActions createWith(String field, String value) throws Exception {
        return create(", \"" + field + "\": " + value);
    }

    @Test
    void validFullApplicationIsAccepted() throws Exception {
        create("""
                , "job_url": "https://careers.acme.com/jobs/42?ref=x", "location": "Bengaluru, KA",
                "salary_min": 1200000, "salary_max": 1800000, "salary_currency": "inr",
                "notes": "Referred by a friend, applied through the careers page."
                """)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.salary_currency", is("INR")));
    }

    @Test
    void whitespaceInNamesIsTrimmedAndCollapsed() throws Exception {
        postApplication("""
                {"company_name": "  Acme    Corp ", "job_title": "Backend   Engineer", "date_applied": "%s", "status": "APPLIED"}
                """.formatted(LocalDate.now()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.company.name", is("Acme Corp")))
                .andExpect(jsonPath("$.job_title", is("Backend Engineer")));
    }

    @Test
    void symbolOnlyCompanyNameIsRejected() throws Exception {
        postApplication("""
                {"company_name": "!!!", "job_title": "Engineer", "date_applied": "%s", "status": "APPLIED"}
                """.formatted(LocalDate.now()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.error.field_errors.companyName", is("Include at least one letter or number.")));
    }

    @Test
    void emojiInTitleIsRejected() throws Exception {
        postApplication("""
                {"company_name": "Acme", "job_title": "Engineer 🚀", "date_applied": "%s", "status": "APPLIED"}
                """.formatted(LocalDate.now()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field_errors.jobTitle", is("Use letters, numbers and common punctuation only.")));
    }

    @Test
    void jobUrlNeedsHttpAndHost() throws Exception {
        createWith("job_url", "\"javascript:alert(1)\"").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field_errors.jobUrl").exists());
        createWith("job_url", "\"https://\"").andExpect(status().isBadRequest());
        createWith("job_url", "\"not a link\"").andExpect(status().isBadRequest());
        createWith("job_url", "\"https://jobs.example.com/a\"").andExpect(status().isCreated());
    }

    @Test
    void absurdDatesAreRejected() throws Exception {
        postApplication("""
                {"company_name": "Acme", "job_title": "Engineer", "date_applied": "1990-05-01", "status": "APPLIED"}
                """)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field_errors.dateApplied").exists());
        createWith("deadline", "\"" + LocalDate.now().plusYears(3) + "\"").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field_errors.deadline").exists());
        createWith("deadline", "\"" + LocalDate.now().plusMonths(2) + "\"").andExpect(status().isCreated());
    }

    @Test
    void salaryMustBeOrderedAndHaveACurrency() throws Exception {
        create(", \"salary_min\": 200, \"salary_max\": 100, \"salary_currency\": \"USD\"")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field_errors.salaryMax", is("The maximum cannot be lower than the minimum.")));
        create(", \"salary_min\": 100")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field_errors.salaryCurrency").exists());
        create(", \"salary_min\": 100, \"salary_currency\": \"ABC\"")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field_errors.salaryCurrency").exists());
        create(", \"salary_max\": -5, \"salary_currency\": \"USD\"")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field_errors.salaryMax").exists());
    }

    @Test
    void junkNotesAreRejectedButShortRealNotesPass() throws Exception {
        createWith("notes", "\"asdfghjkl\"").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field_errors.notes").exists());
        createWith("notes", "\"😂😂😂\"").andExpect(status().isBadRequest());
        createWith("notes", "\"Rejected after round 2\"").andExpect(status().isCreated());
        createWith("notes", "\"आवेदन भेज दिया, जवाब का इंतज़ार है\"").andExpect(status().isCreated());
    }

    @Test
    void updateAppliesTheSameRules() throws Exception {
        String id = objectMapper.readTree(create("").andReturn().getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(patch("/api/v1/applications/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"job_url\": \"ftp://files.example.com\", \"notes\": \"zzzzzzzzzz\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field_errors.jobUrl").exists())
                .andExpect(jsonPath("$.error.field_errors.notes").exists());
        mockMvc.perform(patch("/api/v1/applications/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"salary_min\": 500000, \"salary_max\": 700000}"))
                .andExpect(status().isOk());
    }

    private ResultActions check(String json) throws Exception {
        return mockMvc.perform(post("/api/v1/validate/text")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    @Test
    void dryRunReturnsWarningForOffTopicExperience() throws Exception {
        check("{\"text\": \"the tea there was not good\", \"kind\": \"EXPERIENCE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.errors", hasSize(0)))
                .andExpect(jsonPath("$.warnings[0].code", is("OFF_TOPIC")));
    }

    @Test
    void dryRunReturnsErrorsForMash() throws Exception {
        check("{\"text\": \"asdfghjkl\", \"kind\": \"EXPERIENCE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(false)))
                .andExpect(jsonPath("$.errors[0].code", is("KEYBOARD_MASH")))
                .andExpect(jsonPath("$.warnings", hasSize(0)));
    }

    @Test
    void dryRunAcceptsShortLegitimateAnswers() throws Exception {
        check("{\"text\": \"Rejected after round 2\", \"kind\": \"EXPERIENCE\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid", is(true)))
                .andExpect(jsonPath("$.warnings", hasSize(0)));
    }

    @Test
    void dryRunRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/validate/text")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"text\": \"hello\"}"))
                .andExpect(status().isUnauthorized());
    }
}
