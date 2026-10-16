package com.cohort.api.controller;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class SearchIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findsApplicationsCompaniesAndNotesAcrossOneRequest() throws Exception {
        Session owner = register("search-owner@example.com");
        String applicationId = createApplication(owner.token(), "Zebra Labs");
        String companyId = objectMapper.readTree(fetch(owner.token(), "/api/v1/applications/" + applicationId))
                .get("company").get("id").asText();
        mockMvc.perform(post("/api/v1/companies/" + companyId + "/notes")
                        .header("Authorization", "Bearer " + owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"Ask about the zebra migration\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/search?q=zebra").header("Authorization", "Bearer " + owner.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applications", hasSize(1)))
                .andExpect(jsonPath("$.applications[0].subtitle", is("Zebra Labs")))
                .andExpect(jsonPath("$.companies", hasSize(1)))
                .andExpect(jsonPath("$.notes", hasSize(1)))
                .andExpect(jsonPath("$.notes[0].parent_id", is(companyId)));
    }

    @Test
    void includesSeededQuestionsAndTheUsersOwnButNotOthers() throws Exception {
        Session owner = register("search-questions@example.com");
        String seeded = jdbcTemplate.queryForObject(
                "SELECT question_text FROM questions WHERE is_system = true ORDER BY question_text LIMIT 1", String.class);
        String word = Arrays.stream(seeded.split("\\s+"))
                .map(token -> token.replaceAll("[^A-Za-z]", ""))
                .filter(token -> token.length() >= 4)
                .findFirst()
                .orElseThrow();

        mockMvc.perform(get("/api/v1/search?q=" + word).header("Authorization", "Bearer " + owner.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.questions", not(empty())));
    }

    @Test
    void neverReturnsAnotherUsersData() throws Exception {
        Session owner = register("search-iso-owner@example.com");
        Session intruder = register("search-iso-intruder@example.com");
        String applicationId = createApplication(owner.token(), "Quokka Systems");
        String companyId = objectMapper.readTree(fetch(owner.token(), "/api/v1/applications/" + applicationId))
                .get("company").get("id").asText();
        mockMvc.perform(post("/api/v1/companies/" + companyId + "/notes")
                        .header("Authorization", "Bearer " + owner.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\": \"quokka private note\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/search?q=quokka").header("Authorization", "Bearer " + intruder.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applications", hasSize(0)))
                .andExpect(jsonPath("$.companies", hasSize(0)))
                .andExpect(jsonPath("$.notes", hasSize(0)))
                .andExpect(jsonPath("$.questions", hasSize(0)));
    }

    @Test
    void aQueryUnderTwoCharactersReturnsNothing() throws Exception {
        Session owner = register("search-short@example.com");
        createApplication(owner.token(), "Acme");

        mockMvc.perform(get("/api/v1/search?q=a").header("Authorization", "Bearer " + owner.token()))
                .andExpect(jsonPath("$.applications", hasSize(0)));
        mockMvc.perform(get("/api/v1/search?q=%25%25").header("Authorization", "Bearer " + owner.token()))
                .andExpect(jsonPath("$.questions", hasSize(0)));
        mockMvc.perform(get("/api/v1/search?q=acme")).andExpect(status().isUnauthorized());
    }

    private record Session(UUID userId, String token) {
    }

    private String fetch(String token, String path) throws Exception {
        return mockMvc.perform(get(path)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private String createApplication(String token, String companyName) throws Exception {
        String body = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company_name": "%s", "job_title": "Engineer", "date_applied": "%s", "status": "APPLIED"}
                                """.formatted(companyName, LocalDate.now())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private Session register(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "a-genuinely-fine-password", "display_name": "Tester"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return new Session(UUID.fromString(json.get("user").get("id").asText()), json.get("access_token").asText());
    }
}
