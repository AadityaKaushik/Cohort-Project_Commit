package com.cohort.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class ApplicationImportIntegrationTest extends AbstractIntegrationTest {
    private static final String CSV = """
            Company,Role,Status,Applied Date,Location,URL,Notes
            Acme,Engineer,applied,2026-01-05,Pune,https://acme.com/1,first
            Globex,Analyst,ghosted,2026-01-06,,,
            ,Intern,applied,2026-01-07,,,
            Acme,Engineer,applied,2026-01-05,,,
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private String registerAndGetAccessToken(String email, String displayName) throws Exception {
        String body = """
                {"email": "%s", "password": "a-genuinely-fine-password", "display_name": "%s"}
                """.formatted(email, displayName);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("access_token").asText();
    }

    private ResultActions upload(String token, byte[] content, boolean dryRun) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "apps.csv", "text/csv", content);
        return mockMvc.perform(multipart("/api/v1/applications/import")
                .file(file)
                .param("dryRun", String.valueOf(dryRun))
                .header("Authorization", "Bearer " + token));
    }

    private ResultActions upload(String token, String csv, boolean dryRun) throws Exception {
        return upload(token, csv.getBytes(StandardCharsets.UTF_8), dryRun);
    }

    @Test
    void dryRunPreviewsWithoutCreatingAnything() throws Exception {
        String token = registerAndGetAccessToken("import-dry@example.com", "Importer");

        upload(token, CSV, true)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dry_run", is(true)))
                .andExpect(jsonPath("$.total", is(4)))
                .andExpect(jsonPath("$.importable", is(2)))
                .andExpect(jsonPath("$.invalid", is(1)))
                .andExpect(jsonPath("$.duplicates", is(1)))
                .andExpect(jsonPath("$.created", is(0)))
                .andExpect(jsonPath("$.rows[1].status", is("SAVED")))
                .andExpect(jsonPath("$.rows[1].warnings", hasSize(1)));

        mockMvc.perform(get("/api/v1/applications").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.total", is(0)));
    }

    @Test
    void importCreatesApplicationsAndSkipsDuplicatesOnASecondRun() throws Exception {
        String token = registerAndGetAccessToken("import-run@example.com", "Importer");

        upload(token, CSV, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created", is(2)));

        mockMvc.perform(get("/api/v1/applications").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.total", is(2)));
        mockMvc.perform(get("/api/v1/companies").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(2)));

        upload(token, CSV, false)
                .andExpect(jsonPath("$.created", is(0)))
                .andExpect(jsonPath("$.duplicates", is(3)));
    }

    @Test
    void anotherUsersApplicationsAreNeverCountedAsDuplicatesOrTouched() throws Exception {
        String owner = registerAndGetAccessToken("import-owner@example.com", "Owner");
        String other = registerAndGetAccessToken("import-other@example.com", "Other");
        upload(owner, CSV, false).andExpect(status().isOk());

        upload(other, CSV, false)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.created", is(2)))
                .andExpect(jsonPath("$.duplicates", is(1)));

        mockMvc.perform(get("/api/v1/applications").header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$.total", is(2)));
    }

    @Test
    void rejectsFilesOverOneMegabyte() throws Exception {
        String token = registerAndGetAccessToken("import-big@example.com", "Importer");
        byte[] big = ("company,role\n" + "a".repeat(1024 * 1024 + 1)).getBytes(StandardCharsets.UTF_8);

        upload(token, big, true).andExpect(status().isPayloadTooLarge());
    }

    @Test
    void rejectsFilesWithoutRequiredColumns() throws Exception {
        String token = registerAndGetAccessToken("import-cols@example.com", "Importer");

        upload(token, "name,status\nAcme,applied\n", true)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("INVALID_IMPORT")));
    }

    @Test
    void requiresAuthentication() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "apps.csv", "text/csv", "company,role\n".getBytes());
        mockMvc.perform(multipart("/api/v1/applications/import").file(file)).andExpect(status().isUnauthorized());
    }
}
