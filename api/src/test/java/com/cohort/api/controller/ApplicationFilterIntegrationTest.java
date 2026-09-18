package com.cohort.api.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.cohort.api.entity.ApplicationTag;
import com.cohort.api.entity.Tag;
import com.cohort.api.repository.ApplicationTagRepository;
import com.cohort.api.repository.TagRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Proves {@code ApplicationSpecifications} (Specification pattern,
 * ARCHITECTURE_REVISION_JAVA.md §4.1) compose correctly through the real
 * {@code GET /applications} endpoint — every filter individually, and
 * several combined with AND semantics (US-17), which is exactly what a
 * hand-rolled dynamic-query builder tends to get wrong at the boundary
 * between filters.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class ApplicationFilterIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TagRepository tagRepository;

    @Autowired
    private ApplicationTagRepository applicationTagRepository;

    @Test
    void combinesStatusPriorityWorkModeAndDateRangeWithAndSemantics() throws Exception {
        String token = registerAndGetAccessToken("filter-combo@example.com", "Filterer");

        String matching = createApplication(
                token, "Acme Corp", "Backend Engineer", "APPLIED", "high", "remote", "2026-02-01");
        createApplication(token, "Acme Corp", "Frontend Engineer", "APPLIED", "low", "remote", "2026-02-02");
        createApplication(token, "Acme Corp", "Backend Engineer", "OA", "high", "remote", "2026-02-01");
        createApplication(token, "Acme Corp", "Backend Engineer", "APPLIED", "high", "onsite", "2026-02-01");
        createApplication(token, "Acme Corp", "Backend Engineer", "APPLIED", "high", "remote", "2025-01-01");

        mockMvc.perform(get("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .param("status", "APPLIED")
                        .param("priority", "high")
                        .param("work_mode", "remote")
                        .param("date_from", "2026-01-01")
                        .param("date_to", "2026-12-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(1)))
                .andExpect(jsonPath("$.items[0].id", is(matching)));
    }

    @Test
    void searchIsCaseInsensitiveAcrossCompanyNameAndJobTitle() throws Exception {
        String token = registerAndGetAccessToken("filter-search@example.com", "Searcher");

        String byTitle = createApplication(token, "Initech", "Senior Platform Engineer", "APPLIED", null, null, "2026-02-01");
        String byCompany = createApplication(token, "PlatformWorks", "Data Analyst", "APPLIED", null, null, "2026-02-01");
        createApplication(token, "Umbrella Corp", "Sales Rep", "APPLIED", null, null, "2026-02-01");

        MvcResult result = mockMvc.perform(get("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .param("q", "PLATFORM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(2)))
                .andReturn();

        JsonNode items = objectMapper.readTree(result.getResponse().getContentAsString()).get("items");
        java.util.Set<String> ids = new java.util.HashSet<>();
        items.forEach(item -> ids.add(item.get("id").asText()));
        org.assertj.core.api.Assertions.assertThat(ids).containsExactlyInAnyOrder(byTitle, byCompany);
    }

    @Test
    void filtersByTagCombinedWithStatus() throws Exception {
        String token = registerAndGetAccessToken("filter-tag@example.com", "Tagger");
        UUID userId = userIdFromToken(token);

        String tagged = createApplication(token, "Acme", "Backend Engineer", "APPLIED", null, null, "2026-02-01");
        createApplication(token, "Acme", "Backend Engineer", "APPLIED", null, null, "2026-02-01");
        String taggedButWrongStatus = createApplication(token, "Acme", "Backend Engineer", "OA", null, null, "2026-02-01");

        Tag dreamJob = tagRepository.save(new Tag(userId, "dream-job"));
        applicationTagRepository.save(new ApplicationTag(UUID.fromString(tagged), dreamJob.getId()));
        applicationTagRepository.save(new ApplicationTag(UUID.fromString(taggedButWrongStatus), dreamJob.getId()));

        mockMvc.perform(get("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .param("tag", "dream-job")
                        .param("status", "APPLIED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(1)))
                .andExpect(jsonPath("$.items[0].id", is(tagged)));
    }

    @Test
    void noFiltersReturnsEverythingForThatUserOnly() throws Exception {
        String ownerToken = registerAndGetAccessToken("filter-none-owner@example.com", "Owner");
        String otherToken = registerAndGetAccessToken("filter-none-other@example.com", "Other");
        createApplication(ownerToken, "Acme", "Engineer", "APPLIED", null, null, "2026-02-01");
        createApplication(otherToken, "Acme", "Engineer", "APPLIED", null, null, "2026-02-01");

        mockMvc.perform(get("/api/v1/applications").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(1)));
    }

    private UUID userIdFromToken(String token) {
        String[] parts = token.split("\\.");
        byte[] payload = java.util.Base64.getUrlDecoder().decode(parts[1]);
        try {
            JsonNode node = objectMapper.readTree(payload);
            return UUID.fromString(node.get("sub").asText());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String createApplication(
            String token, String companyName, String jobTitle, String status, String priority, String workMode, String dateApplied)
            throws Exception {
        StringBuilder body = new StringBuilder("{")
                .append("\"company_name\": \"").append(companyName).append("\", ")
                .append("\"job_title\": \"").append(jobTitle).append("\", ")
                .append("\"date_applied\": \"").append(dateApplied).append("\", ")
                .append("\"status\": \"").append(status).append("\"");
        if (priority != null) {
            body.append(", \"priority\": \"").append(priority).append("\"");
        }
        if (workMode != null) {
            body.append(", \"work_mode\": \"").append(workMode).append("\"");
        }
        body.append("}");

        MvcResult result = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.toString()))
                .andExpect(status().isCreated())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
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

        return objectMapper.readTree(result.getResponse().getContentAsString()).get("access_token").asText();
    }
}
