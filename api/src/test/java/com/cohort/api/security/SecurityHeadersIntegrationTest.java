package com.cohort.api.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class SecurityHeadersIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicResponsesCarryTheSecurityHeaders() throws Exception {
        expectSecurityHeaders(mockMvc.perform(get("/health").secure(true)).andExpect(status().isOk()));
    }

    @Test
    void unauthenticatedErrorsCarryTheSecurityHeadersToo() throws Exception {
        expectSecurityHeaders(mockMvc.perform(get("/api/v1/applications").secure(true))
                .andExpect(status().isUnauthorized()));
    }

    @Test
    void oauthChainCarriesTheSecurityHeaders() throws Exception {
        expectSecurityHeaders(mockMvc.perform(get("/oauth2/authorization/unknown-provider").secure(true)));
    }

    @Test
    void hstsIsOnlySentOverHttps() throws Exception {
        mockMvc.perform(get("/health")).andExpect(header().doesNotExist("Strict-Transport-Security"));
    }

    private static void expectSecurityHeaders(ResultActions result) throws Exception {
        result.andExpect(header().string("Content-Security-Policy", SecurityHeaders.CONTENT_SECURITY_POLICY))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Strict-Transport-Security",
                        "max-age=" + SecurityHeaders.HSTS_MAX_AGE_SECONDS + " ; includeSubDomains"));
    }
}
