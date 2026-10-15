package com.cohort.api.controller;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class SampleDataIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void loadCreatesMarkedRecordsAndShowsThemInTheApplicationList() throws Exception {
        String token = register("sample-load@example.com");

        mockMvc.perform(post("/api/v1/sample-data").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loaded", is(true)))
                .andExpect(jsonPath("$.applications", greaterThan(0)))
                .andExpect(jsonPath("$.interviews", greaterThan(0)))
                .andExpect(jsonPath("$.contacts", greaterThan(0)));

        mockMvc.perform(get("/api/v1/applications").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].job_title").value(org.hamcrest.Matchers.startsWith("[Sample] ")));
    }

    @Test
    void loadingTwiceDoesNotDuplicateRows() throws Exception {
        String token = register("sample-twice@example.com");
        UUID userId = userId("sample-twice@example.com");

        mockMvc.perform(post("/api/v1/sample-data").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        int first = count("applications", userId);
        mockMvc.perform(post("/api/v1/sample-data").header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        assertEquals(first, count("applications", userId));
        assertEquals(5, count("companies", userId));
    }

    @Test
    void removeDeletesOnlySampleRowsAndLeavesNoOrphans() throws Exception {
        String token = register("sample-remove@example.com");
        UUID userId = userId("sample-remove@example.com");

        mockMvc.perform(post("/api/v1/applications").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company_name": "My Own Company", "job_title": "Real Role", "date_applied": "%s"}
                                """.formatted(java.time.LocalDate.now().minusDays(1))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/sample-data").header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        mockMvc.perform(delete("/api/v1/sample-data").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loaded", is(false)))
                .andExpect(jsonPath("$.applications", is(0)));

        assertEquals(1, count("applications", userId));
        assertEquals(1, count("companies", userId));
        assertEquals(0, count("interviews", userId));
        assertEquals(0, count("interview_questions", userId));
        assertEquals(0, count("contacts", userId));
        assertEquals(0, count("offers", userId));
        assertEquals(0, count("company_notes", userId));
        assertEquals(0, jdbc.queryForObject("""
                SELECT count(*) FROM application_status_history h
                WHERE NOT EXISTS (SELECT 1 FROM applications a WHERE a.id = h.application_id)""", Integer.class));
    }

    @Test
    void removeIsIdempotent() throws Exception {
        String token = register("sample-idem@example.com");

        mockMvc.perform(delete("/api/v1/sample-data").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/v1/sample-data").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loaded", is(false)));
    }

    @Test
    void loadAndRemoveNeverTouchAnotherUsersRows() throws Exception {
        String owner = register("sample-owner@example.com");
        String other = register("sample-other@example.com");
        UUID ownerId = userId("sample-owner@example.com");

        mockMvc.perform(post("/api/v1/sample-data").header("Authorization", "Bearer " + owner)).andExpect(status().isOk());
        int ownerApplications = count("applications", ownerId);

        mockMvc.perform(get("/api/v1/applications").header("Authorization", "Bearer " + other))
                .andExpect(jsonPath("$.items", hasSize(0)));
        mockMvc.perform(delete("/api/v1/sample-data").header("Authorization", "Bearer " + other))
                .andExpect(status().isOk());

        assertEquals(ownerApplications, count("applications", ownerId));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/sample-data")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/sample-data")).andExpect(status().isUnauthorized());
    }

    private int count(String table, UUID userId) {
        String column = "companies".equals(table) ? "created_by_user_id" : "user_id";
        Integer value = jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE " + column + " = ?", Integer.class, userId);
        return value == null ? 0 : value;
    }

    private UUID userId(String email) {
        return jdbc.queryForObject("SELECT id FROM users WHERE email = ?", UUID.class, email);
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "a-genuinely-fine-password", "display_name": "Sample"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("access_token").asText();
    }
}
