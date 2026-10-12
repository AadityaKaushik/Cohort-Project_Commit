package com.cohort.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.cohort.api.service.fl.FlExportClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * US-06/07/08 end to end against real Postgres: password change (and the
 * session revocation that comes with it), a data export that contains the
 * caller's data and nobody else's, and account deletion followed by an
 * orphan check that scans every UUID column of every table for the deleted
 * user's ID (NFR-14).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class AccountIntegrationTest extends AbstractIntegrationTest {

    private static final byte[] PDF_BYTES = "%PDF-1.4\n%account export resume".getBytes(StandardCharsets.UTF_8);

    @TempDir
    static Path resumeDir;

    @TempDir
    static Path reportDir;

    @DynamicPropertySource
    static void storageDirs(DynamicPropertyRegistry registry) {
        registry.add("app.resume-storage.base-dir", resumeDir::toString);
        registry.add("app.report-storage.base-dir", reportDir::toString);
    }

    @MockitoBean
    private FlExportClient flExportClient;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void changingThePasswordRequiresTheCurrentOneAndRevokesEverySession() throws Exception {
        Account account = register("pw-change@example.com");

        postJson(account.token(), "/api/v1/users/me/password", """
                {"current_password": "not-the-password", "new_password": "a-brand-new-passphrase"}
                """).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field_errors.current_password").exists());
        postJson(account.token(), "/api/v1/users/me/password", """
                {"current_password": "a-genuinely-fine-password", "new_password": "password1234"}
                """).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("WEAK_PASSWORD")));

        postJson(account.token(), "/api/v1/users/me/password", """
                {"current_password": "a-genuinely-fine-password", "new_password": "a-brand-new-passphrase"}
                """).andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refresh_token\": \"%s\"}".formatted(account.refreshToken())))
                .andExpect(status().isUnauthorized());
        login(account.email(), "a-genuinely-fine-password").andExpect(status().isUnauthorized());
        login(account.email(), "a-brand-new-passphrase").andExpect(status().isOk());
    }

    @Test
    void theExportHoldsTheCallersDataAndNoOneElses() throws Exception {
        Account owner = register("export-owner@example.com");
        seed(owner, "Acme Export Co");
        Account other = register("export-other@example.com");
        seed(other, "Globex Secret Co");

        String body = mockMvc.perform(get("/api/v1/users/me/export").header("Authorization", "Bearer " + owner.token()))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(header().string("Content-Disposition", org.hamcrest.Matchers.containsString("attachment")))
                .andReturn().getResponse().getContentAsString();
        JsonNode export = objectMapper.readTree(body);

        assertThat(export.get("profile").get("email").asText()).isEqualTo("export-owner@example.com");
        assertThat(export.get("applications")).hasSize(1);
        assertThat(export.get("companies").get(0).get("name").asText()).isEqualTo("Acme Export Co");
        assertThat(export.get("resumes")).hasSize(1);
        assertThat(export.get("interviews")).hasSize(1);
        assertThat(export.get("notifications")).hasSize(1);
        assertThat(export.get("privacy_ledger_entries")).hasSize(1);
        assertThat(export.get("resume_files")).hasSize(1);
        assertThat(Base64.getDecoder().decode(export.get("resume_files").get(0).get("content_base64").asText()))
                .isEqualTo(PDF_BYTES);

        assertThat(body).doesNotContain("Globex Secret Co", "export-other@example.com", "password_hash", "file_path");
        assertThat(body).doesNotContain(other.userId().toString());
    }

    @Test
    void deletionRequiresTypedConfirmationAndLeavesNoRowReferencingTheUser() throws Exception {
        Account doomed = register("doomed@example.com");
        seed(doomed, "Doomed Co");
        Account survivor = register("survivor@example.com");
        seed(survivor, "Survivor Co");
        String doomedId = doomed.userId().toString();

        List<String> populatedBefore = tablesReferencing(doomedId);
        assertThat(populatedBefore).contains(
                "applications", "companies", "resumes", "interviews", "notifications",
                "report_exports", "refresh_tokens", "privacy_ledger_entries");
        assertThat(filesUnder(resumeDir, doomedId)).isNotEmpty();
        assertThat(filesUnder(reportDir, doomedId)).isNotEmpty();

        deleteAccount(doomed.token(), "delete").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.field_errors.confirmation").exists());
        assertThat(tablesReferencing(doomedId)).isNotEmpty();

        deleteAccount(doomed.token(), "DELETE").andExpect(status().isNoContent());

        assertThat(tablesReferencing(doomedId)).as("tables still referencing the deleted user").isEmpty();
        assertThat(jdbcTemplate.queryForObject("SELECT count(*) FROM users WHERE id = ?::uuid", Integer.class, doomedId)).isZero();
        assertThat(filesUnder(resumeDir, doomedId)).isEmpty();
        assertThat(filesUnder(reportDir, doomedId)).isEmpty();
        verify(flExportClient).delete(doomed.userId());
        login(doomed.email(), "a-genuinely-fine-password").andExpect(status().isUnauthorized());

        assertThat(tablesReferencing(survivor.userId().toString())).containsAll(populatedBefore);
        mockMvc.perform(get("/api/v1/users/me/export").header("Authorization", "Bearer " + survivor.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applications.length()", is(1)));
    }

    @Test
    void everyAccountEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/v1/users/me/password").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/users/me/export")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/api/v1/users/me").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    private List<String> tablesReferencing(String userId) {
        List<Map<String, Object>> uuidColumns = jdbcTemplate.queryForList("""
                SELECT c.table_schema, c.table_name, c.column_name
                FROM information_schema.columns c
                JOIN information_schema.tables t
                  ON t.table_schema = c.table_schema AND t.table_name = c.table_name AND t.table_type = 'BASE TABLE'
                WHERE c.data_type = 'uuid' AND c.table_schema IN ('public', 'fl')
                  AND NOT (c.table_name = 'users' AND c.column_name = 'id')
                """);
        List<String> tables = new ArrayList<>();
        for (Map<String, Object> column : uuidColumns) {
            String qualified = "\"%s\".\"%s\"".formatted(column.get("table_schema"), column.get("table_name"));
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT count(*) FROM %s WHERE \"%s\" = ?::uuid".formatted(qualified, column.get("column_name")),
                    Integer.class, userId);
            if (count != null && count > 0) {
                tables.add((String) column.get("table_name"));
            }
        }
        return tables;
    }

    private List<Path> filesUnder(Path root, String userId) throws Exception {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile).filter(path -> path.toString().contains(userId)).toList();
        }
    }

    private void seed(Account account, String companyName) throws Exception {
        String applicationId = objectMapper.readTree(mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + account.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company_name": "%s", "job_title": "Engineer", "date_applied": "%s", "status": "APPLIED"}
                                """.formatted(companyName, LocalDate.now())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/interviews")
                        .header("Authorization", "Bearer " + account.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"application_id": "%s", "round_number": 1, "round_type": "TECHNICAL", "scheduled_at": "%s"}
                                """.formatted(applicationId, Instant.now().plusSeconds(86_400 * 5))))
                .andExpect(status().isCreated());

        String resumeId = objectMapper.readTree(mockMvc.perform(post("/api/v1/resumes")
                        .header("Authorization", "Bearer " + account.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Main resume\", \"is_default\": true}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(multipart("/api/v1/resumes/" + resumeId + "/file")
                        .file(new MockMultipartFile("file", "resume.pdf", "application/pdf", PDF_BYTES))
                        .header("Authorization", "Bearer " + account.token()))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/reports")
                        .header("Authorization", "Bearer " + account.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"report_type\": \"applications\", \"format\": \"csv\"}"))
                .andExpect(status().isCreated());

        jdbcTemplate.update(
                "INSERT INTO notifications (user_id, type, title, body, channel) VALUES (?::uuid, 'stalled_application', 't', 'b', 'in_app')",
                account.userId().toString());
        UUID roundId = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO fl.fl_rounds (id, round_number, accepted, cohort_size) VALUES (?, 1, true, 5)", roundId);
        jdbcTemplate.update("""
                INSERT INTO fl.privacy_ledger_entries
                    (round_id, user_id, schema_version, pre_clip_norm, post_clip_norm, noise_multiplier, cohort_size, epsilon, transmitted_vector)
                VALUES (?, ?::uuid, 1, 1.0, 1.0, 1.0, 5, 0.5, '[0.1]'::jsonb)
                """, roundId, account.userId().toString());
    }

    private org.springframework.test.web.servlet.ResultActions postJson(String token, String path, String json) throws Exception {
        return mockMvc.perform(post(path)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json));
    }

    private org.springframework.test.web.servlet.ResultActions deleteAccount(String token, String confirmation) throws Exception {
        return mockMvc.perform(delete("/api/v1/users/me")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"confirmation\": \"%s\"}".formatted(confirmation)));
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\": \"%s\", \"password\": \"%s\"}".formatted(email, password)));
    }

    private record Account(UUID userId, String email, String token, String refreshToken) {
    }

    private Account register(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "a-genuinely-fine-password", "display_name": "Tester"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return new Account(UUID.fromString(json.get("user").get("id").asText()), email,
                json.get("access_token").asText(), json.get("refresh_token").asText());
    }
}
