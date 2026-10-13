package com.cohort.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.cohort.api.service.detection.NotificationDetectionJob;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/** US-95: contact CRUD, referral status, cold-contact nudges through the Observer events, and per-user isolation. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class ContactIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NotificationDetectionJob detectionJob;

    @Test
    void contactLifecycleWithReferralStatusAndFilters() throws Exception {
        Session owner = register("contact-crud@example.com");
        String applicationId = createApplication(owner.token(), "Acme");
        String companyId = getJson(owner.token(), "/api/v1/applications/" + applicationId).get("company").get("id").asText();

        String contactId = createContact(owner.token(), """
                {"name": "Priya Nair", "role_title": "Staff Engineer", "relationship": "referrer",
                 "referral_status": "requested", "company_id": "%s", "application_id": "%s"}
                """.formatted(companyId, applicationId));

        mockMvc.perform(get("/api/v1/contacts/" + contactId).header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.company_name", is("Acme")))
                .andExpect(jsonPath("$.referral_status", is("requested")))
                .andExpect(jsonPath("$.cold", is(false)));

        mockMvc.perform(patch("/api/v1/contacts/" + contactId)
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"referral_status\": \"submitted\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.referral_status", is("submitted")))
                .andExpect(jsonPath("$.name", is("Priya Nair")));

        mockMvc.perform(get("/api/v1/contacts?referral_status=submitted").header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/api/v1/contacts?referral_status=declined").header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/api/v1/contacts?company_id=" + companyId).header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(post("/api/v1/contacts/" + contactId + "/contacted").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.last_contacted_at").isNotEmpty());

        mockMvc.perform(delete("/api/v1/contacts/" + contactId).header("Authorization", bearer(owner)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/contacts").header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void aNameIsRequired() throws Exception {
        Session owner = register("contact-validation@example.com");
        mockMvc.perform(post("/api/v1/contacts")
                        .header("Authorization", bearer(owner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \" \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void aColdContactProducesOneNudgeUntilItIsContactedAgain() throws Exception {
        Session owner = register("contact-cold@example.com");
        String warm = createContact(owner.token(), "{\"name\": \"Warm Wendy\"}");
        String cold = createContact(owner.token(), "{\"name\": \"Cold Carl\", \"referral_status\": \"requested\"}");
        jdbcTemplate.update("UPDATE contacts SET last_contacted_at = ? WHERE id = ?::uuid",
                Timestamp.from(Instant.now().minus(45, ChronoUnit.DAYS)), cold);

        detectionJob.runFor(owner.userId(), Instant.now());
        detectionJob.runFor(owner.userId(), Instant.now());

        JsonNode items = getJson(owner.token(), "/api/v1/notifications").get("items");
        assertThat(items).hasSize(1);
        assertThat(items.get(0).get("type").asText()).isEqualTo("cold_contact");
        assertThat(items.get(0).get("related_entity_type").asText()).isEqualTo("contact");
        assertThat(items.get(0).get("related_entity_id").asText()).isEqualTo(cold);
        assertThat(items.get(0).get("title").asText()).isEqualTo("Reconnect with Cold Carl");
        assertThat(warm).isNotEqualTo(cold);

        mockMvc.perform(get("/api/v1/contacts/" + cold).header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$.cold", is(true)));
    }

    @Test
    void anotherUserCanNeitherReadChangeNorLinkSomeoneElsesContact() throws Exception {
        Session owner = register("contact-iso-owner@example.com");
        Session intruder = register("contact-iso-intruder@example.com");
        String contactId = createContact(owner.token(), "{\"name\": \"Private Pat\"}");
        String ownerApplication = createApplication(owner.token(), "Initech");

        mockMvc.perform(get("/api/v1/contacts/" + contactId).header("Authorization", bearer(intruder)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("CONTACT_NOT_FOUND")));
        mockMvc.perform(patch("/api/v1/contacts/" + contactId)
                        .header("Authorization", bearer(intruder))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Hijacked\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/contacts/" + contactId + "/contacted").header("Authorization", bearer(intruder)))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete("/api/v1/contacts/" + contactId).header("Authorization", bearer(intruder)))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/api/v1/contacts").header("Authorization", bearer(intruder)))
                .andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(post("/api/v1/contacts")
                        .header("Authorization", bearer(intruder))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Sneaky\", \"application_id\": \"%s\"}".formatted(ownerApplication)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/v1/contacts")
                        .header("Authorization", bearer(intruder))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\": \"Sneaky\", \"company_id\": \"%s\"}".formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/contacts/" + contactId).header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$.name", is("Private Pat")));
        mockMvc.perform(get("/api/v1/contacts")).andExpect(status().isUnauthorized());
    }

    private record Session(UUID userId, String token) {
    }

    private String bearer(Session session) {
        return "Bearer " + session.token();
    }

    private JsonNode getJson(String token, String path) throws Exception {
        String body = mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private String createContact(String token, String json) throws Exception {
        String body = mockMvc.perform(post("/api/v1/contacts")
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
