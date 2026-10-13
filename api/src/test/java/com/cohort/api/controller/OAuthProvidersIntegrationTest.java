package com.cohort.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class OAuthProvidersIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void noProvidersAreListedWithoutCredentialsAndTheEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/api/v1/auth/oauth/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providers", hasSize(0)));
    }

    @Test
    void startingAFlowForAnUnconfiguredProviderDoesNotRedirectAnywhere() throws Exception {
        mockMvc.perform(get("/oauth2/authorization/google"))
                .andExpect(status().isNotFound());
    }
}
