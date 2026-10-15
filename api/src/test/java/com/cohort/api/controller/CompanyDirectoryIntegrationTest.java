package com.cohort.api.controller;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
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

/** US-96: the company directory is shared reference data that still needs a signed-in user. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class CompanyDirectoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void searchFindsSeededCompaniesIgnoringCase() throws Exception {
        String token = register("directory-search@example.com");

        mockMvc.perform(get("/api/v1/directory/companies?query=payu").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].slug", is("payu")))
                .andExpect(jsonPath("$[0].role_families", hasItem("software_engineer")));

        mockMvc.perform(get("/api/v1/directory/companies").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThan(10)));
    }

    @Test
    void profileCarriesRoundsSourcesAndNoCommunityRatingYet() throws Exception {
        String token = register("directory-detail@example.com");

        mockMvc.perform(get("/api/v1/directory/companies/payu").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("PayU")))
                .andExpect(jsonPath("$.community_rating", nullValue()))
                .andExpect(jsonPath("$.community_min_contributors", is(5)))
                .andExpect(jsonPath("$.role_patterns[0].rounds.length()", greaterThan(0)))
                .andExpect(jsonPath("$.role_patterns[0].sources[0].url").isNotEmpty())
                .andExpect(jsonPath("$.role_patterns[0].last_checked").isNotEmpty());
    }

    @Test
    void difficultyIsExposedOnlyWhereASourceStatesIt() throws Exception {
        String token = register("directory-difficulty@example.com");

        mockMvc.perform(get("/api/v1/directory/companies?query=cognizant").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$[0].difficulty_level", is(2)))
                .andExpect(jsonPath("$[0].difficulty_basis").isNotEmpty());
        mockMvc.perform(get("/api/v1/directory/companies/payu").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.role_patterns[0].difficulty_level", nullValue()))
                .andExpect(jsonPath("$.role_patterns[0].difficulty_basis", nullValue()));
    }

    @Test
    void unknownSlugIsNotFoundAndAnonymousCallersAreRejected() throws Exception {
        String token = register("directory-missing@example.com");

        mockMvc.perform(get("/api/v1/directory/companies/no-such-company").header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/directory/companies")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/directory/companies/payu")).andExpect(status().isUnauthorized());
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
