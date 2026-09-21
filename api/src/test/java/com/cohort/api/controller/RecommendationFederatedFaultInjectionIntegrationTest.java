package com.cohort.api.controller;

import static org.hamcrest.Matchers.is;
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
 * P53's headline test — the "we thought about failure modes" commit,
 * per {@code BUILD_PHASES_JAVA.md}'s own quick-reference callout. Proves,
 * against a real HTTP round trip (not a mocked gateway), that stopping
 * fl-service <em>mid-deployment</em> never turns {@code GET /recommendations?federated=true}
 * into a 500: the Resilience4j circuit breaker's fallback
 * ({@link com.cohort.api.service.fl.FlCircuitBreakerGateway}) degrades
 * silently to the exact same local-only recommendations a plain,
 * non-federated request would have produced.
 *
 * <p>This test must never be watered down (no catching an exception in the
 * test itself, no asserting a non-200 status is "acceptable") — the whole
 * point is that the API layer never sees the failure at all.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class RecommendationFederatedFaultInjectionIntegrationTest extends AbstractIntegrationTest {

    private static StubFlServiceServer stubFlService;

    @DynamicPropertySource
    static void flServiceProperties(DynamicPropertyRegistry registry) throws Exception {
        stubFlService = new StubFlServiceServer();
        registry.add("app.fl.service-base-url", stubFlService::baseUrl);
        // Fast timeouts so a genuinely-down fl-service (connection refused,
        // the scenario this test exercises) fails fast rather than the
        // request itself hanging near the default timeout budget.
        registry.add("app.fl.connect-timeout-ms", () -> "500");
        registry.add("app.fl.read-timeout-ms", () -> "500");
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
    void federatedRecommendationsGracefullyDegradeToLocalOnlyWhenFlServiceGoesDownMidRequest() throws Exception {
        String token = registerAndGetAccessToken("rec-fl-fault@example.com", "Owner");

        // Phase 1: fl-service is up -- federated=true actually reaches it,
        // and every returned recommendation is marked federated_applied.
        mockMvc.perform(get("/api/v1/recommendations?federated=true").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].federated_applied", is(true)));

        // Phase 2: fl-service goes down mid-deployment (stopped outright --
        // connection refused, not a slow response). The exact same request
        // must still return 200 with local-only results, never a 500.
        stubFlService.stop();

        MvcResult result = mockMvc.perform(get("/api/v1/recommendations?federated=true").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].federated_applied", is(false)))
                .andExpect(jsonPath("$[0].type", is("baseline")))
                .andReturn();

        // Belt-and-braces: the body really is the ordinary recommendation
        // payload, not an empty/error placeholder standing in for a crash.
        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(body).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(body.get(0).get("explanation").asText()).isNotBlank();
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
