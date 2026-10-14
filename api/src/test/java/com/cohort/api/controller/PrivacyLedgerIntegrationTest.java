package com.cohort.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.cohort.api.service.fl.PrivacyLedgerService;
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

@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class PrivacyLedgerIntegrationTest extends AbstractIntegrationTest {
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

    @Autowired
    private PrivacyLedgerService privacyLedgerService;

    @Test
    void transmittedVectorIsStoredAndReadBack() throws Exception {
        String ownerToken = registerAndGetAccessToken("ledger-owner@example.com", "Owner");
        String ownerId = currentUserId(ownerToken);
        optIn(ownerToken);

        stubFlService.respondToRoundWith("""
                {"round_number": 1, "accepted": true, "reason": null, "cohort_size": 3, "entries": [
                    {"client_id": "%s", "schema_version": 1, "pre_clip_norm": 1.732, "post_clip_norm": 1.0,
                     "noise_multiplier": 1.0, "epsilon": 1.0,
                     "transmitted_vector": [0.123456, -0.987654, 0.5, -0.5, 0.111]}
                ]}
                """.formatted(ownerId));

        privacyLedgerService.runNextRoundAndPersist();

        mockMvc.perform(get("/api/v1/fl/privacy-ledger").header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].round_number", is(1)))
                .andExpect(jsonPath("$[0].pre_clip_norm", is(1.732)))
                .andExpect(jsonPath("$[0].post_clip_norm", is(1.0)))
                .andExpect(jsonPath("$[0].transmitted_vector", hasSize(5)))
                .andExpect(jsonPath("$[0].transmitted_vector[0]", is(0.123456)))
                .andExpect(jsonPath("$[0].transmitted_vector[1]", is(-0.987654)));

        org.assertj.core.api.Assertions.assertThat(stubFlService.lastRoundRequestJson()).contains(ownerId);
    }

    @Test
    void aUserWhoOptedOutIsNotSentAsAParticipantAndGetsNoEntry() throws Exception {
        String withdrawnToken = registerAndGetAccessToken("ledger-withdrawn@example.com", "Withdrawn");
        String withdrawnId = currentUserId(withdrawnToken);
        String participantToken = registerAndGetAccessToken("ledger-participant@example.com", "Participant");
        String participantId = currentUserId(participantToken);
        optIn(withdrawnToken);
        optIn(participantToken);
        mockMvc.perform(post("/api/v1/fl/opt-out").header("Authorization", "Bearer " + withdrawnToken)).andExpect(status().isOk());

        stubFlService.respondToRoundWith("""
                {"round_number": 1, "accepted": true, "reason": null, "cohort_size": 3, "entries": [
                    {"client_id": "%s", "schema_version": 1, "pre_clip_norm": 1.0, "post_clip_norm": 1.0,
                     "noise_multiplier": 1.0, "epsilon": 1.0, "transmitted_vector": [0.4]},
                    {"client_id": "%s", "schema_version": 1, "pre_clip_norm": 1.0, "post_clip_norm": 1.0,
                     "noise_multiplier": 1.0, "epsilon": 1.0, "transmitted_vector": [0.5]}
                ]}
                """.formatted(withdrawnId, participantId));
        privacyLedgerService.runNextRoundAndPersist();

        org.assertj.core.api.Assertions.assertThat(stubFlService.lastRoundRequestJson())
                .contains(participantId)
                .doesNotContain(withdrawnId);
        mockMvc.perform(get("/api/v1/fl/privacy-ledger").header("Authorization", "Bearer " + withdrawnToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/v1/fl/privacy-ledger").header("Authorization", "Bearer " + participantToken))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].transmitted_vector[0]", is(0.5)));
    }

    @Test
    void aUsersLedgerEntriesAreInvisibleToAnotherUser() throws Exception {
        String ownerToken = registerAndGetAccessToken("ledger-owner-2@example.com", "Owner");
        String ownerId = currentUserId(ownerToken);
        String intruderToken = registerAndGetAccessToken("ledger-intruder@example.com", "Intruder");
        optIn(ownerToken);

        stubFlService.respondToRoundWith("""
                {"round_number": 1, "accepted": true, "reason": null, "cohort_size": 3, "entries": [
                    {"client_id": "%s", "schema_version": 1, "pre_clip_norm": 1.0, "post_clip_norm": 1.0,
                     "noise_multiplier": 1.0, "epsilon": 1.0, "transmitted_vector": [0.1, 0.2]}
                ]}
                """.formatted(ownerId));
        privacyLedgerService.runNextRoundAndPersist();

        mockMvc.perform(get("/api/v1/fl/privacy-ledger").header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        MvcResult ownerList = mockMvc.perform(get("/api/v1/fl/privacy-ledger").header("Authorization", "Bearer " + ownerToken))
                .andExpect(jsonPath("$", hasSize(1)))
                .andReturn();
        String entryId = objectMapper.readTree(ownerList.getResponse().getContentAsString()).get(0).get("id").asText();

        mockMvc.perform(get("/api/v1/fl/privacy-ledger/" + entryId).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transmitted_vector", hasSize(2)));
        // FR-95: foreign access is 404, indistinguishable from a missing id -- never 403.
        mockMvc.perform(get("/api/v1/fl/privacy-ledger/" + entryId).header("Authorization", "Bearer " + intruderToken))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/fl/privacy-ledger/" + java.util.UUID.randomUUID()).header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isNotFound());
    }

    private void optIn(String token) throws Exception {
        mockMvc.perform(post("/api/v1/fl/opt-in").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
    }

    private String currentUserId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
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
