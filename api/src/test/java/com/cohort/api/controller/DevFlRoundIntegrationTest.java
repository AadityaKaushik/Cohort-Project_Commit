package com.cohort.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.cohort.api.service.fl.StubFlServiceServer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@Import(AbstractIntegrationTest.ContainerConfig.class)
class DevFlRoundIntegrationTest extends AbstractIntegrationTest {
    private static StubFlServiceServer stubFlService;

    @DynamicPropertySource
    static void flServiceProperties(DynamicPropertyRegistry registry) throws Exception {
        stubFlService = new StubFlServiceServer();
        registry.add("app.fl.service-base-url", stubFlService::baseUrl);
    }

    @AfterAll
    static void tearDownStub() {
        stubFlService.close();
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void devRoundExportsRunsTheRoundAndKeepsOnlyTheRealUsersLedgerEntry() throws Exception {
        String token = registerAndGetAccessToken("dev-round-owner@example.com");
        String userId = currentUserId(token);
        mockMvc.perform(post("/api/v1/fl/opt-in").header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        stubFlService.respondToRoundWith("""
                {"round_number": 1, "accepted": true, "reason": null, "cohort_size": 4, "entries": [
                    {"client_id": "%s", "schema_version": 1, "pre_clip_norm": 2.0, "post_clip_norm": 1.0,
                     "noise_multiplier": 1.0, "epsilon": 1.0, "transmitted_vector": [0.25, -0.5]},
                    {"client_id": "synthetic-client-01", "schema_version": 1, "pre_clip_norm": 1.5, "post_clip_norm": 1.0,
                     "noise_multiplier": 1.0, "epsilon": 1.0, "transmitted_vector": [0.1, 0.2]}
                ]}
                """.formatted(userId));

        mockMvc.perform(post("/api/v1/dev/fl/round").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.exported_users", is(1)))
                .andExpect(jsonPath("$.export_failures", is(0)))
                .andExpect(jsonPath("$.round_ran", is(true)))
                .andExpect(jsonPath("$.accepted", is(true)))
                .andExpect(jsonPath("$.cohort_size", is(4)));

        org.assertj.core.api.Assertions.assertThat(stubFlService.lastRoundRequestJson()).contains(userId);
        mockMvc.perform(get("/api/v1/fl/privacy-ledger").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].cohort_size", is(4)))
                .andExpect(jsonPath("$[0].transmitted_vector[0]", is(0.25)));
    }

    @Test
    void statusShowsTheCurrentModelWithNamedWeightsTheHistoryAndYourPrediction() throws Exception {
        String token = registerAndGetAccessToken("dev-status@example.com");

        mockMvc.perform(get("/api/v1/dev/fl/status").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available", is(true)))
                .andExpect(jsonPath("$.model.version", is(1)))
                .andExpect(jsonPath("$.model.weights", hasSize(24)))
                .andExpect(jsonPath("$.model.weights[0].feature", is("mastery_mean")))
                .andExpect(jsonPath("$.model.weights[0].value", is(1.5)))
                .andExpect(jsonPath("$.model.intercept", is(-1.0)))
                .andExpect(jsonPath("$.versions", hasSize(2)))
                .andExpect(jsonPath("$.versions[1].check_accuracy", is(0.58)))
                .andExpect(jsonPath("$.central_ceiling", is(0.74)))
                .andExpect(jsonPath("$.synthetic_clients", is(20)))
                .andExpect(jsonPath("$.your_rated_rounds", is(0)))
                .andExpect(jsonPath("$.your_predicted_confidence").isNumber());
    }

    @Test
    void demoActionsAreForwardedToFlService() throws Exception {
        String token = registerAndGetAccessToken("dev-demo-actions@example.com");

        mockMvc.perform(put("/api/v1/dev/fl/synthetic-clients").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"count\": 20}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/dev/fl/reset").header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
        mockMvc.perform(put("/api/v1/dev/fl/synthetic-clients").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"count\": 999}"))
                .andExpect(status().isBadRequest());

        org.assertj.core.api.Assertions.assertThat(stubFlService.demoCalls())
                .contains("PUT /fl/demo/synthetic-clients", "POST /fl/demo/reset");
    }

    @Test
    void aRoundCanAskForTheDemoNoiseLevel() throws Exception {
        String token = registerAndGetAccessToken("dev-round-noise@example.com");
        mockMvc.perform(post("/api/v1/fl/opt-in").header("Authorization", "Bearer " + token)).andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/dev/fl/round").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"noise_multiplier\": 0.5}"))
                .andExpect(status().isOk());

        org.assertj.core.api.Assertions.assertThat(stubFlService.lastRoundRequestJson()).contains("\"noise_multiplier\":0.5");
        mockMvc.perform(post("/api/v1/fl/opt-out").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }

    @Test
    void devRoundNeedsALogin() throws Exception {
        mockMvc.perform(post("/api/v1/dev/fl/round")).andExpect(status().isUnauthorized());
    }

    private String currentUserId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private String registerAndGetAccessToken(String email) throws Exception {
        String registerBody = """
                {"email": "%s", "password": "a-genuinely-fine-password", "display_name": "Dev Round"}
                """.formatted(email);
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("access_token").asText();
    }
}
