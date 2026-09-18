package com.cohort.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Sprint 1 end-to-end journey (US-73 partial): the same story a demo would
 * walk through, exercised as one continuous flow through the real HTTP
 * endpoints against a real Postgres 16 instance — register, log in (as a
 * separate step from registration, proving the credentials really were
 * persisted and hashed correctly), create an application, move it through
 * two status transitions (one legal forward step, one illegal attempt that
 * must be rejected), and read back the resulting history timeline.
 *
 * <p>This complements, rather than replaces, the narrower per-endpoint
 * tests in AuthIntegrationTest / UserProfileIntegrationTest /
 * ApplicationIntegrationTest (including their cross-user isolation checks,
 * which together are this sprint's isolation suite) — this test's job is to
 * prove the pieces actually compose into one working journey.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class SprintOneJourneyIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerLoginCreateApplicationTransitionStatusAndReadHistory() throws Exception {
        String email = "journey@example.com";
        String password = "a-genuinely-fine-password";

        // 1. Register.
        String registerBody = """
                {"email": "%s", "password": "%s", "display_name": "Journey Tester"}
                """.formatted(email, password);

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email", is(email)));

        // 2. Log in as a fresh step (not reusing the register response's
        // token) — proves the password was actually persisted and hashed
        // correctly, not just accepted at registration time.
        String loginBody = """
                {"email": "%s", "password": "%s"}
                """.formatted(email, password);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andReturn();

        String accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("access_token").asText();

        // A newly logged-in session can immediately reach a protected endpoint.
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.display_name", is("Journey Tester")))
                .andExpect(jsonPath("$.fl_opt_in", is(false)));

        // 3. Create an application.
        String createBody = """
                {"company_name": "Journey Co", "job_title": "Platform Engineer",
                 "date_applied": "2026-01-05", "status": "SAVED"}
                """;

        MvcResult createResult = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("SAVED")))
                .andExpect(jsonPath("$.current_stage", is("SAVED")))
                .andReturn();

        JsonNode created = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String applicationId = created.get("id").asText();

        // 4. Legal forward transition, through the State pattern.
        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/status")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"to_status": "APPLIED"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPLIED")))
                .andExpect(jsonPath("$.current_stage", is("APPLIED")));

        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/status")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"to_status": "INTERVIEW"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("INTERVIEW")));

        // 5. Illegal transition — skipping straight to a status not reachable
        // from Interview without going via Offer — is rejected with 409 and
        // writes no history row.
        mockMvc.perform(post("/api/v1/applications/" + applicationId + "/status")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"to_status": "ACCEPTED"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("INVALID_STATUS_TRANSITION")));

        // 6. The application's own view still reflects the last *legal* status.
        mockMvc.perform(get("/api/v1/applications/" + applicationId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("INTERVIEW")))
                .andExpect(jsonPath("$.history", org.hamcrest.Matchers.hasSize(3)));

        // 7. History reads newest-first and matches the journey exactly —
        // the rejected illegal attempt left no trace.
        MvcResult historyResult = mockMvc.perform(get("/api/v1/applications/" + applicationId + "/history")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(3)))
                .andExpect(jsonPath("$[0].to_status", is("INTERVIEW")))
                .andExpect(jsonPath("$[0].from_status", is("APPLIED")))
                .andExpect(jsonPath("$[1].to_status", is("APPLIED")))
                .andExpect(jsonPath("$[1].from_status", is("SAVED")))
                .andExpect(jsonPath("$[2].to_status", is("SAVED")))
                .andExpect(jsonPath("$[2].from_status", org.hamcrest.Matchers.nullValue()))
                .andReturn();

        JsonNode history = objectMapper.readTree(historyResult.getResponse().getContentAsString());
        assertThat(history).hasSize(3);
    }
}
