package com.cohort.api.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.assertj.core.api.Assertions;
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

@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class ResumeIntegrationTest extends AbstractIntegrationTest {
    private static final byte[] REAL_PDF_BYTES = "%PDF-1.4\n%fake but valid resume content".getBytes(StandardCharsets.UTF_8);
    private static final byte[] WINDOWS_EXECUTABLE_BYTES =
            new byte[] {0x4D, 0x5A, (byte) 0x90, 0x00, 0x03, 0x00, 0x00, 0x00};

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

    @Test
    void createListGetUpdateDeleteHappyPath() throws Exception {
        String token = registerAndGetAccessToken("resume-owner-crud@example.com", "Owner");

        MvcResult createResult = mockMvc.perform(post("/api/v1/resumes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Backend Resume", "version_label": "v1", "target_role": "Backend Engineer"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name", is("Backend Resume")))
                .andExpect(jsonPath("$.version_label", is("v1")))
                .andExpect(jsonPath("$.is_default", is(false)))
                .andExpect(jsonPath("$.has_file", is(false)))
                .andReturn();

        String resumeId = idOf(createResult);

        mockMvc.perform(get("/api/v1/resumes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(1)))
                .andExpect(jsonPath("$.items[0].name", is("Backend Resume")));

        mockMvc.perform(get("/api/v1/resumes/" + resumeId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Backend Resume")));

        mockMvc.perform(patch("/api/v1/resumes/" + resumeId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Backend Resume (Updated)"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Backend Resume (Updated)")));

        mockMvc.perform(delete("/api/v1/resumes/" + resumeId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/resumes/" + resumeId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("RESUME_NOT_FOUND")));
    }

    @Test
    void settingADefaultResumeUnsetsAnyPreviousDefaultForThatUser() throws Exception {
        String token = registerAndGetAccessToken("resume-default@example.com", "Default Tester");

        String firstId = createResume(token, "Resume A", true);
        String secondId = createResume(token, "Resume B", true);

        mockMvc.perform(get("/api/v1/resumes/" + firstId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_default", is(false)));

        mockMvc.perform(get("/api/v1/resumes/" + secondId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_default", is(true)));

        mockMvc.perform(patch("/api/v1/resumes/" + firstId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"is_default": true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_default", is(true)));

        mockMvc.perform(get("/api/v1/resumes/" + secondId).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_default", is(false)));
    }

    @Test
    void uploadThenDownloadRoundTripsTheExactFileWithAttachmentHeaders() throws Exception {
        String token = registerAndGetAccessToken("resume-upload@example.com", "Uploader");
        String resumeId = createResume(token, "Resume With File", false);

        MockMultipartFile filePart = new MockMultipartFile("file", "my-resume.pdf", "application/pdf", REAL_PDF_BYTES);

        mockMvc.perform(multipart("/api/v1/resumes/" + resumeId + "/file")
                        .file(filePart)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.has_file", is(true)))
                .andExpect(jsonPath("$.file_original_name", is("my-resume.pdf")));

        MvcResult downloadResult = mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/file")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("my-resume.pdf")))
                .andReturn();

        Assertions.assertThat(downloadResult.getResponse().getContentAsByteArray()).isEqualTo(REAL_PDF_BYTES);
    }

    @Test
    void downloadingAResumeWithNoFileUploadedYetReturns404() throws Exception {
        String token = registerAndGetAccessToken("resume-no-file@example.com", "No File");
        String resumeId = createResume(token, "Empty Resume", false);

        mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/file").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("RESUME_FILE_NOT_FOUND")));
    }

    @Test
    void aRenamedExecutableUploadIsRejectedThroughTheFullValidationChain() throws Exception {
        String token = registerAndGetAccessToken("resume-malicious@example.com", "Attacker");
        String resumeId = createResume(token, "Target Resume", false);

        MockMultipartFile disguisedExecutable =
                new MockMultipartFile("file", "resume.pdf", "application/pdf", WINDOWS_EXECUTABLE_BYTES);

        mockMvc.perform(multipart("/api/v1/resumes/" + resumeId + "/file")
                        .file(disguisedExecutable)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.error.code", is("INVALID_FILE")));

        mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/file").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("RESUME_FILE_NOT_FOUND")));
    }

    @Test
    void crossUserAccessReturns404NeverExposingAnotherUsersResumeOrItsFile() throws Exception {
        String ownerToken = registerAndGetAccessToken("resume-owner-isolation@example.com", "Owner");
        String intruderToken = registerAndGetAccessToken("resume-intruder-isolation@example.com", "Intruder");
        String resumeId = createResume(ownerToken, "Private Resume", false);

        MockMultipartFile filePart = new MockMultipartFile("file", "secret.pdf", "application/pdf", REAL_PDF_BYTES);
        mockMvc.perform(multipart("/api/v1/resumes/" + resumeId + "/file")
                        .file(filePart)
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/resumes/" + resumeId).header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("RESUME_NOT_FOUND")));

        mockMvc.perform(patch("/api/v1/resumes/" + resumeId)
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "Hijacked"}
                                """))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/v1/resumes/" + resumeId).header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());

        // The critical one: a foreign user must not be able to download the real stored file, ever.
        mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/file").header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("RESUME_NOT_FOUND")));

        MockMultipartFile intruderUpload = new MockMultipartFile("file", "replace.pdf", "application/pdf", REAL_PDF_BYTES);
        mockMvc.perform(multipart("/api/v1/resumes/" + resumeId + "/file")
                        .file(intruderUpload)
                        .header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/file").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/resumes").header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(0)));
    }

    private String createResume(String token, String name, boolean isDefault) throws Exception {
        String body = """
                {"name": "%s", "is_default": %s}
                """.formatted(name, isDefault);

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
