package com.cohort.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.cohort.api.service.report.ReportService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class ReportIntegrationTest extends AbstractIntegrationTest {
    @TempDir
    static Path storageDir;

    @DynamicPropertySource
    static void reportStorageDir(DynamicPropertyRegistry registry) {
        registry.add("app.report-storage.base-dir", storageDir::toString);
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ReportService reportService;

    @Test
    void ownerGeneratesListsDownloadsAndDeletesAReport() throws Exception {
        String token = register("report-owner@example.com");
        createApplication(token, "Acme");

        String csvId = createReport(token, "applications", "csv").get("id").asText();
        String pdfId = createReport(token, "progress_report", "pdf").get("id").asText();

        mockMvc.perform(get("/api/v1/reports").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id", is(pdfId)));

        byte[] csv = mockMvc.perform(get("/api/v1/reports/" + csvId + "/file").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(csv, StandardCharsets.UTF_8)).contains("Acme");

        byte[] pdf = mockMvc.perform(get("/api/v1/reports/" + pdfId + "/file").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");

        mockMvc.perform(delete("/api/v1/reports/" + csvId).header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/reports/" + csvId + "/file").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/reports").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void anotherUserGets404OnEveryReportEndpoint() throws Exception {
        String owner = register("report-isolation-owner@example.com");
        createApplication(owner, "Acme");
        String reportId = createReport(owner, "applications", "csv").get("id").asText();

        String intruder = register("report-isolation-intruder@example.com");

        mockMvc.perform(get("/api/v1/reports/" + reportId + "/file").header("Authorization", "Bearer " + intruder))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("REPORT_NOT_FOUND")));
        mockMvc.perform(delete("/api/v1/reports/" + reportId).header("Authorization", "Bearer " + intruder))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/reports").header("Authorization", "Bearer " + intruder))
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/v1/reports/" + reportId + "/file").header("Authorization", "Bearer " + owner))
                .andExpect(status().isOk());
    }

    @Test
    void anExpiredReportIs410AndIsRemovedByTheCleanup() throws Exception {
        String token = register("report-expiry@example.com");
        createApplication(token, "Acme");
        String reportId = createReport(token, "applications", "csv").get("id").asText();
        jdbcTemplate.update("UPDATE report_exports SET generated_at = ?, expires_at = ? WHERE id = ?::uuid",
                Timestamp.from(Instant.now().minus(8, ChronoUnit.DAYS)),
                Timestamp.from(Instant.now().minus(1, ChronoUnit.DAYS)), reportId);

        mockMvc.perform(get("/api/v1/reports/" + reportId + "/file").header("Authorization", "Bearer " + token))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.error.code", is("REPORT_EXPIRED")));
        mockMvc.perform(get("/api/v1/reports").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$", hasSize(0)));

        assertThat(reportService.deleteExpired(Instant.now())).isGreaterThanOrEqualTo(1);
        mockMvc.perform(get("/api/v1/reports/" + reportId + "/file").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void invalidRequestsAreRejectedAndEveryEndpointRequiresAuthentication() throws Exception {
        String token = register("report-validation@example.com");

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"report_type": "progress_report", "format": "csv"}
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"report_type": "applications", "format": "csv", "date_from": "2026-03-01", "date_to": "2026-02-01"}
                                """))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/v1/reports")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/reports").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/reports/" + UUID.randomUUID() + "/file")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/reports/" + UUID.randomUUID())).andExpect(status().isUnauthorized());
    }

    private JsonNode createReport(String token, String type, String format) throws Exception {
        String body = mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"report_type": "%s", "format": "%s"}
                                """.formatted(type, format)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private void createApplication(String token, String companyName) throws Exception {
        mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company_name": "%s", "job_title": "Engineer", "date_applied": "%s", "status": "APPLIED"}
                                """.formatted(companyName, LocalDate.now())))
                .andExpect(status().isCreated());
    }

    private String register(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "a-genuinely-fine-password", "display_name": "Tester"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("access_token").asText();
    }
}
