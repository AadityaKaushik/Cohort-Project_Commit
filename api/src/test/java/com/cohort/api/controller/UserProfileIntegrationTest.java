package com.cohort.api.controller;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class UserProfileIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void meRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code", is("UNAUTHENTICATED")));
    }

    @Test
    void getAndPatchOwnProfile() throws Exception {
        String accessToken = registerAndGetAccessToken("profile-owner@example.com", "Profile Owner");

        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("profile-owner@example.com")))
                .andExpect(jsonPath("$.display_name", is("Profile Owner")))
                .andExpect(jsonPath("$.fl_opt_in", is(false)));

        String patchBody = """
                {"display_name": "Updated Name", "target_role": "Backend Engineer", "timezone": "Asia/Kolkata"}
                """;

        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.display_name", is("Updated Name")))
                .andExpect(jsonPath("$.target_role", is("Backend Engineer")))
                .andExpect(jsonPath("$.timezone", is("Asia/Kolkata")))
                .andExpect(jsonPath("$.email", is("profile-owner@example.com")));

        // Change persisted, not just echoed in the response.
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.display_name", is("Updated Name")));
    }

    @Test
    void patchRejectsEmailAlreadyTakenByAnotherAccount() throws Exception {
        registerAndGetAccessToken("existing@example.com", "Existing User");
        String secondUserToken = registerAndGetAccessToken("second@example.com", "Second User");

        String patchBody = """
                {"email": "existing@example.com"}
                """;

        mockMvc.perform(patch("/api/v1/users/me")
                        .header("Authorization", "Bearer " + secondUserToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patchBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("EMAIL_CONFLICT")));
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

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("access_token").asText();
    }
}
