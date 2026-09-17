package com.cohort.api.controller;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
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
 * Exercises the real /auth/* endpoints against a real Postgres 16
 * instance — the phase's verification target: happy path, bad password,
 * 401s, and no distinguishable email enumeration.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class AuthIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registerLoginRefreshLogoutHappyPath() throws Exception {
        String email = "happy-path@example.com";

        String registerBody = """
                {"email": "%s", "password": "a-genuinely-fine-password", "display_name": "Happy Path"}
                """.formatted(email);

        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.email", is(email)))
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.refresh_token", notNullValue()))
                .andReturn();

        String firstRefreshToken = objectMapper.readTree(registerResult.getResponse().getContentAsString())
                .get("refresh_token").asText();

        String loginBody = """
                {"email": "%s", "password": "a-genuinely-fine-password"}
                """.formatted(email);

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginBody))
                .andExpect(status().isOk())
                .andReturn();

        String loginRefreshToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("refresh_token").asText();

        String refreshBody = """
                {"refresh_token": "%s"}
                """.formatted(loginRefreshToken);

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.access_token", notNullValue()))
                .andExpect(jsonPath("$.refresh_token", notNullValue()));

        // The just-consumed refresh token is single-use: reusing it now fails.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshBody))
                .andExpect(status().isUnauthorized());

        // A different, still-live token (from register) can still log out cleanly.
        String logoutBody = """
                {"refresh_token": "%s"}
                """.formatted(firstRefreshToken);

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutBody))
                .andExpect(status().isNoContent());

        // Logged-out token no longer works for a refresh.
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutBody))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void registerRejectsDuplicateEmailWith409() throws Exception {
        String body = """
                {"email": "duplicate@example.com", "password": "a-genuinely-fine-password", "display_name": "First"}
                """;

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("REGISTRATION_CONFLICT")));
    }

    @Test
    void registerRejectsWeakPasswordWith400() throws Exception {
        String body = """
                {"email": "weak@example.com", "password": "password123", "display_name": "Weak"}
                """;

        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code", is("WEAK_PASSWORD")));
    }

    @Test
    void loginWithWrongPasswordAndUnknownEmailReturnIdenticalResponses() throws Exception {
        String registerBody = """
                {"email": "enum-check@example.com", "password": "a-genuinely-fine-password", "display_name": "Enum Check"}
                """;
        mockMvc.perform(post("/api/v1/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerBody))
                .andExpect(status().isCreated());

        String wrongPasswordBody = """
                {"email": "enum-check@example.com", "password": "totally-wrong-password"}
                """;
        MvcResult wrongPasswordResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(wrongPasswordBody))
                .andExpect(status().isUnauthorized())
                .andReturn();

        String unknownEmailBody = """
                {"email": "no-such-account@example.com", "password": "totally-wrong-password"}
                """;
        MvcResult unknownEmailResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unknownEmailBody))
                .andExpect(status().isUnauthorized())
                .andReturn();

        org.assertj.core.api.Assertions.assertThat(wrongPasswordResult.getResponse().getContentAsString())
                .isEqualTo(unknownEmailResult.getResponse().getContentAsString());
    }

    @Test
    void protectedEndpointRejectsUnauthenticatedRequestWithJsonEnvelope() throws Exception {
        mockMvc.perform(post("/api/v1/does-not-exist-yet"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code", is("UNAUTHENTICATED")));
    }
}
