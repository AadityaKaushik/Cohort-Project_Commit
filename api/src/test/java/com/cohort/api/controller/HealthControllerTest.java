package com.cohort.api.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.config.CorsConfig;
import com.cohort.api.security.JwtConfig;
import com.cohort.api.security.RestAccessDeniedHandler;
import com.cohort.api.security.RestAuthenticationEntryPoint;
import com.cohort.api.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/**
 * A web-layer slice test — no datasource, no Flyway, no full application
 * context. The real SecurityConfig is imported (rather than relying on
 * Spring Boot's test-time security defaults) so this proves /health is
 * genuinely reachable under the actual security rules, not just in
 * isolation from them.
 */
@WebMvcTest(HealthController.class)
@Import({SecurityConfig.class, CorsConfig.class, JwtConfig.class, RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class})
class HealthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void healthEndpointReturns200WithUpStatus() throws Exception {
        mockMvc.perform(get("/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")));
    }
}
