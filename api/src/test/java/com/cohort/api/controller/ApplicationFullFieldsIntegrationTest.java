package com.cohort.api.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
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
class ApplicationFullFieldsIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void everyFormFieldRoundTripsThroughCreateAndUpdate() throws Exception {
        String token = register("full-fields@example.com");

        MvcResult created = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company_name": "Full Co", "job_title": "Engineer", "date_applied": "2026-01-10",
                                 "status": "APPLIED", "location": "Berlin", "work_mode": "hybrid",
                                 "job_type": "full_time", "salary_min": 90000, "salary_max": 120000,
                                 "salary_currency": "EUR", "priority": "high", "source": "Referral",
                                 "is_referral": true, "deadline": "2026-02-01", "recruiter_name": "Sam",
                                 "recruiter_email": "sam@example.com", "job_url": "https://example.com/job",
                                 "job_description": "Build things", "notes": "Warm intro", "cover_letter": "Hello"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/v1/applications/" + id).header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.work_mode", is("hybrid")))
                .andExpect(jsonPath("$.job_type", is("full_time")))
                .andExpect(jsonPath("$.salary_currency", is("EUR")))
                .andExpect(jsonPath("$.priority", is("high")))
                .andExpect(jsonPath("$.is_referral", is(true)))
                .andExpect(jsonPath("$.deadline", is("2026-02-01")))
                .andExpect(jsonPath("$.recruiter_email", is("sam@example.com")))
                .andExpect(jsonPath("$.cover_letter", is("Hello")));

        mockMvc.perform(patch("/api/v1/applications/" + id)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\": \"low\", \"work_mode\": \"remote\", \"salary_max\": 130000}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priority", is("low")))
                .andExpect(jsonPath("$.work_mode", is("remote")))
                .andExpect(jsonPath("$.salary_min", is(90000)))
                .andExpect(jsonPath("$.salary_max", is(130000)));
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "a-genuinely-fine-password", "display_name": "Full"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("access_token").asText();
    }
}
