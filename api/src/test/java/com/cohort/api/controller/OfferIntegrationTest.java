package com.cohort.api.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class OfferIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void offerShowsFirstYearTotalAndDaysRemaining() throws Exception {
        Session owner = register("offer-crud@example.com");
        String applicationId = createApplication(owner.token(), "Acme");
        String deadline = LocalDate.now().plusDays(6).toString();

        String offerId = createOffer(owner.token(), """
                {"application_id": "%s", "base_salary": 100000, "bonus": 10000, "equity_value": 20000,
                 "signing_bonus": 5000, "currency": "EUR", "work_mode": "hybrid", "response_deadline": "%s"}
                """.formatted(applicationId, deadline));

        mockMvc.perform(get("/api/v1/offers/" + offerId).header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company_name", is("Acme")))
                .andExpect(jsonPath("$.job_title", is("Engineer")))
                .andExpect(jsonPath("$.first_year_total", is(135000)))
                .andExpect(jsonPath("$.days_remaining", is(6)))
                .andExpect(jsonPath("$.currency", is("EUR")))
                .andExpect(jsonPath("$.status", is("pending")));

        mockMvc.perform(patch("/api/v1/offers/" + offerId)
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"accepted\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("accepted")))
                .andExpect(jsonPath("$.days_remaining", nullValue()));

        mockMvc.perform(delete("/api/v1/offers/" + offerId).header("Authorization", bearer(owner)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/offers").header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void listIsOrderedByNearestDeadline() throws Exception {
        Session owner = register("offer-order@example.com");
        String later = createApplication(owner.token(), "Later Ltd");
        String sooner = createApplication(owner.token(), "Sooner Inc");
        createOffer(owner.token(), "{\"application_id\": \"%s\", \"base_salary\": 1, \"response_deadline\": \"%s\"}"
                .formatted(later, LocalDate.now().plusDays(20)));
        createOffer(owner.token(), "{\"application_id\": \"%s\", \"base_salary\": 1, \"response_deadline\": \"%s\"}"
                .formatted(sooner, LocalDate.now().plusDays(3)));

        mockMvc.perform(get("/api/v1/offers").header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$[0].company_name", is("Sooner Inc")))
                .andExpect(jsonPath("$[1].company_name", is("Later Ltd")));
    }

    @Test
    void anApplicationHasOneOffer() throws Exception {
        Session owner = register("offer-dup@example.com");
        String applicationId = createApplication(owner.token(), "Acme");
        String json = "{\"application_id\": \"%s\", \"base_salary\": 90000}".formatted(applicationId);
        createOffer(owner.token(), json);

        mockMvc.perform(post("/api/v1/offers")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code", is("DUPLICATE_OFFER")));
    }

    @Test
    void salaryMustNotBeNegative() throws Exception {
        Session owner = register("offer-validation@example.com");
        String applicationId = createApplication(owner.token(), "Acme");
        mockMvc.perform(post("/api/v1/offers")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"application_id\": \"%s\", \"base_salary\": -1}".formatted(applicationId)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void anotherUserGetsNotFoundForSomeoneElsesOffer() throws Exception {
        Session owner = register("offer-iso-owner@example.com");
        Session intruder = register("offer-iso-intruder@example.com");
        String applicationId = createApplication(owner.token(), "Initech");
        String offerId = createOffer(owner.token(),
                "{\"application_id\": \"%s\", \"base_salary\": 80000}".formatted(applicationId));

        mockMvc.perform(get("/api/v1/offers/" + offerId).header("Authorization", bearer(intruder)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("OFFER_NOT_FOUND")));
        mockMvc.perform(patch("/api/v1/offers/" + offerId)
                        .header("Authorization", bearer(intruder))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"base_salary\": 1}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/offers/" + offerId).header("Authorization", bearer(intruder)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/offers").header("Authorization", bearer(intruder)))
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(post("/api/v1/offers")
                        .header("Authorization", bearer(intruder))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"application_id\": \"%s\", \"base_salary\": 1}".formatted(applicationId)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/offers")).andExpect(status().isUnauthorized());
    }

    private record Session(UUID userId, String token) {
    }

    private String bearer(Session session) {
        return "Bearer " + session.token();
    }

    private String createOffer(String token, String json) throws Exception {
        String body = mockMvc.perform(post("/api/v1/offers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private String createApplication(String token, String companyName) throws Exception {
        String body = mockMvc.perform(post("/api/v1/applications")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"company_name": "%s", "job_title": "Engineer", "date_applied": "%s", "status": "APPLIED"}
                                """.formatted(companyName, LocalDate.now())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).get("id").asText();
    }

    private Session register(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "a-genuinely-fine-password", "display_name": "Tester"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return new Session(UUID.fromString(json.get("user").get("id").asText()), json.get("access_token").asText());
    }
}
