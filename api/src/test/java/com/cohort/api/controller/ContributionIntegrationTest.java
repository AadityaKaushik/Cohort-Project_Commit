package com.cohort.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.ResultActions;

/** US-96: anonymous contributions, one per user, hidden below five contributors. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class ContributionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void aggregatesStayHiddenUntilFiveDistinctContributors() throws Exception {
        for (int i = 1; i <= 4; i++) {
            contribute(register("min-" + i + "@example.com"), "freshworks", 2025, "Practise aggregation questions " + i);
        }
        String token = register("min-viewer@example.com");

        mockMvc.perform(get("/api/v1/directory/companies/freshworks").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.community_rating", nullValue()))
                .andExpect(jsonPath("$.community_difficulty", nullValue()))
                .andExpect(jsonPath("$.community_contributors", is(4)))
                .andExpect(jsonPath("$.community_min_contributors", is(5)))
                .andExpect(jsonPath("$.community_summaries[0].tips", hasSize(0)));

        contribute(register("min-5@example.com"), "freshworks", 2025, "Revise joins before the technical round");

        mockMvc.perform(get("/api/v1/directory/companies/freshworks").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.community_contributors", is(5)))
                .andExpect(jsonPath("$.community_rating", is(4.0)))
                .andExpect(jsonPath("$.community_difficulty", is(3.0)))
                .andExpect(jsonPath("$.community_summaries[0].common_rounds[0].percent", is(100)))
                .andExpect(jsonPath("$.community_summaries[0].outcomes.offer", is(100)))
                .andExpect(jsonPath("$.community_summaries[0].tips", hasSize(5)))
                .andExpect(jsonPath("$.community_summaries[0].tips[0]", is("Practise aggregation questions 1")));
    }

    @Test
    void sameUserCannotSubmitTwiceForTheSameCompanyRoleAndYear() throws Exception {
        String token = register("dedupe@example.com");
        contribute(token, "tcs", 2025, null);

        submit(token, body("tcs", 2025, null)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("DUPLICATE_CONTRIBUTION")));
        mockMvc.perform(post("/api/v1/directory/contributions").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(body("tcs", 2024, null)))
                .andExpect(status().isCreated());
    }

    @Test
    void mineAndWithdrawAreScopedToTheContributor() throws Exception {
        String owner = register("owner@example.com");
        String other = register("other@example.com");
        String id = objectMapper.readTree(contribute(owner, "infosys", 2025, null)).get("id").asText();

        mockMvc.perform(get("/api/v1/directory/contributions/mine").header("Authorization", "Bearer " + other))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(delete("/api/v1/directory/contributions/" + id).header("Authorization", "Bearer " + other))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/directory/contributions/mine").header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].company_slug", is("infosys")));
        mockMvc.perform(delete("/api/v1/directory/contributions/" + id).header("Authorization", "Bearer " + owner))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/directory/contributions/mine").header("Authorization", "Bearer " + owner))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void freeCompanyNameCreatesAStubProfileWithoutPatterns() throws Exception {
        String token = register("stub@example.com");
        String request = body(null, 2025, null).replace("\"company_slug\": null", "\"company_name\": \"Brand New Labs\"");

        mockMvc.perform(post("/api/v1/directory/contributions").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.company_slug", is("brand-new-labs")));
        mockMvc.perform(get("/api/v1/directory/companies/brand-new-labs").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role_patterns", hasSize(0)))
                .andExpect(jsonPath("$.community_contributors", is(1)));
    }

    @Test
    void tipWithContactDetailsIsRejected() throws Exception {
        String token = register("contact@example.com");

        submit(token, body("tcs", 2025, "Email the recruiter at hr@example.com for help"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresSignIn() throws Exception {
        mockMvc.perform(get("/api/v1/directory/contributions/mine")).andExpect(status().isUnauthorized());
    }

    private ResultActions submit(String token, String json) throws Exception {
        return mockMvc.perform(post("/api/v1/directory/contributions").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private String contribute(String token, String slug, int year, String tip) throws Exception {
        return submit(token, body(slug, year, tip)).andExpect(status().isCreated()).andReturn()
                .getResponse().getContentAsString();
    }

    private static String body(String slug, int year, String tip) {
        return """
                {"company_slug": %s, "role_family": "software_engineer", "level": "fresher", "year": %d,
                 "rounds": [{"round_type": "technical", "difficulty": 3}],
                 "overall_difficulty": 3, "overall_experience": 4, "outcome": "offer", "tip": %s}
                """.formatted(slug == null ? "null" : "\"" + slug + "\"", year, tip == null ? "null" : "\"" + tip + "\"");
    }

    private String register(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "a-genuinely-fine-password", "display_name": "Tester"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("access_token").asText();
    }
}
