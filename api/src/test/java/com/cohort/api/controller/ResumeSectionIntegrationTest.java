package com.cohort.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
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
class ResumeSectionIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void addEditReorderAndDeleteKeepSortOrderContiguous() throws Exception {
        String token = register("sections-owner@example.com");
        String resumeId = createResume(token, "Main");

        String first = addSection(token, resumeId, "experience", "Backend Engineer", "Acme");
        String second = addSection(token, resumeId, "skill", "Languages", null);
        String third = addSection(token, resumeId, "education", "BSc", "State University");

        mockMvc.perform(put(base(resumeId) + "/" + second)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"section_type\":\"skill\",\"title\":\"Languages\",\"description\":\"Java, SQL\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description", is("Java, SQL")));

        mockMvc.perform(put(base(resumeId) + "/order")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"section_ids\":[\"%s\",\"%s\",\"%s\"]}".formatted(third, first, second)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(third)))
                .andExpect(jsonPath("$[2].id", is(second)));

        mockMvc.perform(delete(base(resumeId) + "/" + third).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(base(resumeId)).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(first)))
                .andExpect(jsonPath("$[0].sort_order", is(0)))
                .andExpect(jsonPath("$[1].sort_order", is(1)));
    }

    @Test
    void anotherUsersResumeSectionsReturn404() throws Exception {
        String ownerToken = register("sections-private-owner@example.com");
        String intruderToken = register("sections-intruder@example.com");
        String resumeId = createResume(ownerToken, "Private");
        String sectionId = addSection(ownerToken, resumeId, "experience", "Secret role", null);
        String body = "{\"section_type\":\"experience\",\"title\":\"x\"}";

        mockMvc.perform(get(base(resumeId)).header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(base(resumeId))
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
        mockMvc.perform(put(base(resumeId) + "/" + sectionId)
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(base(resumeId) + "/" + sectionId).header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(put(base(resumeId) + "/order")
                        .header("Authorization", "Bearer " + intruderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"section_ids\":[\"%s\"]}".formatted(sectionId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/export").header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void sectionFromAnotherResumeOfTheSameUserIsNotReachable() throws Exception {
        String token = register("sections-cross-resume@example.com");
        String resumeA = createResume(token, "A");
        String resumeB = createResume(token, "B");
        String sectionInA = addSection(token, resumeA, "project", "Cohort", null);

        mockMvc.perform(delete(base(resumeB) + "/" + sectionInA).header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void cloneCarriesTheEditedSections() throws Exception {
        String token = register("sections-clone@example.com");
        String resumeId = createResume(token, "Source");
        String sectionId = addSection(token, resumeId, "experience", "Old title", "Acme");

        mockMvc.perform(put(base(resumeId) + "/" + sectionId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"section_type\":\"experience\",\"title\":\"Edited title\",\"organisation\":\"Acme\"}"))
                .andExpect(status().isOk());

        MvcResult clone = mockMvc.perform(post("/api/v1/resumes/" + resumeId + "/clone")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();
        String cloneId = idOf(clone);

        mockMvc.perform(get(base(cloneId)).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Edited title")));
    }

    @Test
    void exportReturnsANonEmptyPdf() throws Exception {
        String token = register("sections-export@example.com");
        String resumeId = createResume(token, "Export me");
        addSection(token, resumeId, "experience", "Backend Engineer", "Acme");
        addSection(token, resumeId, "skill", "Languages", null);

        MvcResult result = mockMvc.perform(get("/api/v1/resumes/" + resumeId + "/export")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        byte[] pdf = result.getResponse().getContentAsByteArray();
        assertThat(pdf).isNotEmpty();
        assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        assertThat(result.getResponse().getContentType()).isEqualTo("application/pdf");
    }

    private String base(String resumeId) {
        return "/api/v1/resumes/" + resumeId + "/sections";
    }

    private String addSection(String token, String resumeId, String type, String title, String organisation) throws Exception {
        String org = organisation == null ? "null" : "\"" + organisation + "\"";
        MvcResult result = mockMvc.perform(post(base(resumeId))
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"section_type\":\"%s\",\"title\":\"%s\",\"organisation\":%s}".formatted(type, title, org)))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private String createResume(String token, String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/resumes")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\"}".formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();
        return idOf(result);
    }

    private String idOf(MvcResult result) throws Exception {
        JsonNode node = objectMapper.readTree(result.getResponse().getContentAsString());
        return node.get("id").asText();
    }

    private String register(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "a-genuinely-fine-password", "display_name": "Owner"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("access_token").asText();
    }
}
