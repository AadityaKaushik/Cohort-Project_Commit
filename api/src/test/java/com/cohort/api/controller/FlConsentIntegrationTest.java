package com.cohort.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.cohort.api.service.fl.FlExportClient;
import com.cohort.api.service.fl.FlFeatureExportJob;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class FlConsentIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private FlFeatureExportJob exportJob;

    @MockitoBean
    private FlExportClient exportClient;

    @Test
    void aNewUserIsOptedOutByDefault() throws Exception {
        String token = registerAndGetAccessToken("fl-default@example.com", "Owner");

        mockMvc.perform(get("/api/v1/fl/consent").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.opted_in", is(false)));
    }

    @Test
    void optInThenOptOutRoundTrips() throws Exception {
        String token = registerAndGetAccessToken("fl-roundtrip@example.com", "Owner");

        mockMvc.perform(post("/api/v1/fl/opt-in").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.opted_in", is(true)));
        mockMvc.perform(get("/api/v1/fl/consent").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.opted_in", is(true)));

        mockMvc.perform(post("/api/v1/fl/opt-out").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.opted_in", is(false)));
        mockMvc.perform(get("/api/v1/fl/consent").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.opted_in", is(false)));
    }

    @Test
    void anOptedOutUserProducesNoExportWhileAnOptedInUserDoes() throws Exception {
        String withdrawnToken = registerAndGetAccessToken("fl-withdrawn@example.com", "Withdrawn");
        String neverToken = registerAndGetAccessToken("fl-never@example.com", "Never");
        String participantToken = registerAndGetAccessToken("fl-participant@example.com", "Participant");

        mockMvc.perform(post("/api/v1/fl/opt-in").header("Authorization", "Bearer " + withdrawnToken)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/fl/opt-out").header("Authorization", "Bearer " + withdrawnToken)).andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/fl/opt-in").header("Authorization", "Bearer " + participantToken)).andExpect(status().isOk());

        exportJob.runExport();

        verify(exportClient, never()).export(eq(currentUserId(withdrawnToken)), any(), any());
        verify(exportClient, never()).export(eq(currentUserId(neverToken)), any(), any());
        verify(exportClient).export(eq(currentUserId(participantToken)), any(), any());
    }

    @Test
    void oneUsersConsentNeverChangesAnotherUsers() throws Exception {
        String ownerToken = registerAndGetAccessToken("fl-iso-owner@example.com", "Owner");
        String otherToken = registerAndGetAccessToken("fl-iso-other@example.com", "Other");

        mockMvc.perform(post("/api/v1/fl/opt-in").header("Authorization", "Bearer " + otherToken))
                .andExpect(jsonPath("$.opted_in", is(true)));

        // Consent is looked up by JWT subject, so there is no foreign id to try.
        mockMvc.perform(get("/api/v1/fl/consent").header("Authorization", "Bearer " + ownerToken))
                .andExpect(jsonPath("$.opted_in", is(false)));
    }

    @Test
    void unauthenticatedRequestsAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/fl/consent")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/fl/opt-in")).andExpect(status().isUnauthorized());
    }

    @Test
    void schemaPublishesExactlyTwentyFourAllowListedFeatures() throws Exception {
        String token = registerAndGetAccessToken("fl-schema@example.com", "Owner");

        MvcResult result = mockMvc.perform(get("/api/v1/fl/schema").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feature_count", is(24)))
                .andExpect(jsonPath("$.features", hasSize(24)))
                .andReturn();

        var body = objectMapper.readTree(result.getResponse().getContentAsString());
        for (var feature : body.get("features")) {
            org.assertj.core.api.Assertions.assertThat(feature.has("name")).isTrue();
            org.assertj.core.api.Assertions.assertThat(feature.get("name").isTextual()).isTrue();
            org.assertj.core.api.Assertions.assertThat(feature.has("lower_bound")).isTrue();
            org.assertj.core.api.Assertions.assertThat(feature.has("upper_bound")).isTrue();
        }
        org.assertj.core.api.Assertions.assertThat(body.get("not_collected").isArray()).isTrue();
    }

    private UUID currentUserId(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
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
