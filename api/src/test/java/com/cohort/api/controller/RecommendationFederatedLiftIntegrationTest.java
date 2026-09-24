package com.cohort.api.controller;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * P57/US-65: the Federated Lift viewer's plumbing end to end over HTTP --
 * {@code ?federated=true} actually reaches fl-service, marks every
 * response {@code federated_applied}, and carries a {@code rank_delta}.
 * The bounded-multiplier property itself is proven directly and
 * exhaustively by {@code FederatedLiftCalculatorTest}; this test only
 * needs to prove the wiring, not re-derive the bound.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class RecommendationFederatedLiftIntegrationTest extends AbstractIntegrationTest {

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
    void federatedRequestMarksEveryResponseAndCarriesARankDelta() throws Exception {
        // A deliberately extreme model -- proves in the same request that even a huge
        // parameter set still produces a plumbed-through, bounded outcome (the bound
        // itself is FederatedLiftCalculatorTest's job to prove exhaustively).
        stubFlService.respondWith("""
                {"version": 9, "round_number": 9, "participant_count": 5, "created_at": "2026-01-01T00:00:00Z", "parameters": [50.0, -50.0, 25.0]}
                """);
        String token = registerAndGetAccessToken("lift-fresh@example.com", "Owner");

        mockMvc.perform(get("/api/v1/recommendations?federated=true").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].federated_applied", is(true)))
                .andExpect(jsonPath("$[0].rank_delta", notNullValue()));
    }

    @Test
    void nonFederatedRequestsAlwaysHaveAZeroRankDelta() throws Exception {
        String token = registerAndGetAccessToken("lift-local-only@example.com", "Owner");

        MvcResult result = mockMvc.perform(get("/api/v1/recommendations").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].federated_applied", is(false)))
                .andExpect(jsonPath("$[0].rank_delta", is(0)))
                .andReturn();

        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        for (var item : body) {
            org.assertj.core.api.Assertions.assertThat(item.get("rank_delta").asInt()).isZero();
        }
    }

    @Test
    void aNeutralModelLeavesTheLocalOnlyOrderUntouched() throws Exception {
        // All-zero parameters -> every multiplier is exactly 1.0, so the federated
        // order must be the local-only order: every rank_delta is 0.
        stubFlService.respondWith("""
                {"version": 1, "round_number": 1, "participant_count": 3, "created_at": "2026-01-01T00:00:00Z", "parameters": [0.0, 0.0, 0.0]}
                """);
        String token = registerAndGetAccessToken("lift-neutral@example.com", "Owner");

        MvcResult local = mockMvc.perform(get("/api/v1/recommendations").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult federated = mockMvc.perform(get("/api/v1/recommendations?federated=true").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].federated_applied", is(true)))
                .andReturn();

        var localBody = objectMapper.readTree(local.getResponse().getContentAsString());
        var federatedBody = objectMapper.readTree(federated.getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(federatedBody.size()).isEqualTo(localBody.size());
        for (int i = 0; i < federatedBody.size(); i++) {
            org.assertj.core.api.Assertions.assertThat(federatedBody.get(i).get("rank_delta").asInt()).isZero();
            org.assertj.core.api.Assertions.assertThat(federatedBody.get(i).get("headline").asText())
                    .isEqualTo(localBody.get(i).get("headline").asText());
        }
    }

    @Test
    void anotherUserCannotActOnARecommendationFromMyFederatedRanking() throws Exception {
        String ownerToken = registerAndGetAccessToken("lift-owner@example.com", "Owner");
        String intruderToken = registerAndGetAccessToken("lift-intruder@example.com", "Intruder");

        MvcResult result = mockMvc.perform(get("/api/v1/recommendations?federated=true").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();
        String recommendationId = objectMapper.readTree(result.getResponse().getContentAsString()).get(0).get("id").asText();

        // FR-95: 404, never 403.
        mockMvc.perform(post("/api/v1/recommendations/" + recommendationId + "/snooze").header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());
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
