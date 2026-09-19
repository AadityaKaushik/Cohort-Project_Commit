package com.cohort.api.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.cohort.api.entity.ResumeSection;
import com.cohort.api.repository.ResumeSectionRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * US-25/FR-35: cloning a resume copies structured content (resume_sections)
 * and records lineage via {@code parent_resume_id}, but never the uploaded
 * file and never the {@code is_default} flag — plus the mandatory
 * cross-user isolation check (FR-95) on the clone endpoint itself.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class ResumeCloneIntegrationTest extends AbstractIntegrationTest {

    private static final byte[] REAL_PDF_BYTES = "%PDF-1.4\n%fake resume content".getBytes(StandardCharsets.UTF_8);

    @TempDir
    static Path storageDir;

    @DynamicPropertySource
    static void resumeStorageDir(DynamicPropertyRegistry registry) {
        registry.add("app.resume-storage.base-dir", storageDir::toString);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ResumeSectionRepository resumeSectionRepository;

    @Test
    void cloneCopiesStructuredContentAndLineageButNotTheFileOrDefaultFlag() throws Exception {
        String token = registerAndGetAccessToken("resume-clone-owner@example.com", "Owner");
        String resumeId = createResume(token, "Source Resume", "v1", true);

        MockMultipartFile filePart = new MockMultipartFile("file", "source.pdf", "application/pdf", REAL_PDF_BYTES);
        mockMvc.perform(multipart("/api/v1/resumes/" + resumeId + "/file")
                        .file(filePart)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        ResumeSection section = new ResumeSection(UUID.fromString(resumeId), ResumeSection.SectionType.experience);
        section.setTitle("Backend Engineer");
        section.setOrganisation("Acme Corp");
        section.setSortOrder(0);
        resumeSectionRepository.save(section);

        MvcResult cloneResult = mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/clone")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Source Resume")))
                .andExpect(jsonPath("$.version_label", is("v2")))
                .andExpect(jsonPath("$.parent_resume_id", is(resumeId)))
                .andExpect(jsonPath("$.is_default", is(false)))
                .andExpect(jsonPath("$.has_file", is(false)))
                .andReturn();

        String cloneId = idOf(cloneResult);

        List<ResumeSection> clonedSections = resumeSectionRepository.findByResumeIdOrderBySortOrder(UUID.fromString(cloneId));
        org.assertj.core.api.Assertions.assertThat(clonedSections).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(clonedSections.get(0).getTitle()).isEqualTo("Backend Engineer");
        org.assertj.core.api.Assertions.assertThat(clonedSections.get(0).getOrganisation()).isEqualTo("Acme Corp");

        // The original is untouched and still has its file and default flag.
        mockMvc.perform(get("/api/v1/resumes/" + resumeId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_default", is(true)))
                .andExpect(jsonPath("$.has_file", is(true)));
    }

    @Test
    void cloningAResumeThatBelongsToAnotherUserReturns404() throws Exception {
        String ownerToken = registerAndGetAccessToken("resume-clone-target-owner@example.com", "Owner");
        String intruderToken = registerAndGetAccessToken("resume-clone-intruder@example.com", "Intruder");
        String resumeId = createResume(ownerToken, "Private Resume", "v1", false);

        mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/clone").header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("RESUME_NOT_FOUND")));
    }

    private String createResume(String token, String name, String versionLabel, boolean isDefault) throws Exception {
        String body = """
                {"name": "%s", "version_label": "%s", "is_default": %s}
                """.formatted(name, versionLabel, isDefault);

        MvcResult result = mockMvc.perform(post("/api/v1/resumes")
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
