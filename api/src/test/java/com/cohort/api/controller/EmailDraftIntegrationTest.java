package com.cohort.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.cohort.api.service.ai.AiTestConfig;
import com.cohort.api.service.ai.StubAiProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import({AbstractIntegrationTest.ContainerConfig.class, AiTestConfig.class})
class EmailDraftIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private StubAiProvider provider;

    @BeforeEach
    void reset() {
        provider.reset();
    }

    @Test
    void negotiationTemplateUsesTheOfferData() throws Exception {
        String token = register("draft-offer@example.com");
        String applicationId = createApplication(token, "Acme", "Engineer");
        send("/api/v1/offers", token, Map.of("application_id", applicationId, "base_salary", 95000, "signing_bonus", 5000));
        provider.goDown();

        JsonNode draft = draft(token, applicationId, "negotiation", 200);

        assertThat(draft.get("source").asText()).isEqualTo("template");
        assertThat(draft.get("ai_available").asBoolean()).isFalse();
        assertThat(draft.get("body").asText()).contains("95000 USD").contains("signing bonus of 5000").contains("Acme");
    }

    @Test
    void negotiationWithoutAnOfferStillProducesADraft() throws Exception {
        String token = register("draft-nooffer@example.com");
        String applicationId = createApplication(token, "Acme", "Engineer");

        JsonNode draft = draft(token, applicationId, "negotiation", 200);

        assertThat(draft.get("body").asText()).contains("[your target]").doesNotContain("base salary of 0");
    }

    @Test
    void thankYouAddressesTheInterviewer() throws Exception {
        String token = register("draft-thanks@example.com");
        String applicationId = createApplication(token, "Acme", "Engineer");
        Map<String, Object> interview = new HashMap<>();
        interview.put("application_id", applicationId);
        interview.put("round_number", 1);
        interview.put("round_type", "TECHNICAL");
        interview.put("interviewer_name", "Priya Natarajan");
        send("/api/v1/interviews", token, interview);

        JsonNode draft = draft(token, applicationId, "thank_you", 200);

        assertThat(draft.get("body").asText()).startsWith("Hi Priya,");
    }

    @Test
    void modelReplyIsUsedWhenItAddsNoNewFigures() throws Exception {
        String token = register("draft-model@example.com");
        String applicationId = createApplication(token, "Acme", "Engineer");
        String reply = "Hi there, I wanted to follow up on my application for the Engineer role at Acme and ask whether "
                + "there is an update on the next step. Thank you for your time. Best regards, [Your name]";
        provider.replyWith(reply);

        JsonNode draft = draft(token, applicationId, "follow_up", 200);

        assertThat(draft.get("source").asText()).isEqualTo("model");
        assertThat(draft.get("body").asText()).isEqualTo(reply);
    }

    @Test
    void replyThatInventsAFigureIsRejected() throws Exception {
        String token = register("draft-invent@example.com");
        String applicationId = createApplication(token, "Acme", "Engineer");
        send("/api/v1/offers", token, Map.of("application_id", applicationId, "base_salary", 95000));
        provider.replyWith("Hi, thanks for the offer. I would like a base salary of 130000 USD for this Engineer role at Acme. "
                + "Best regards, [Your name]");

        JsonNode draft = draft(token, applicationId, "negotiation", 200);

        assertThat(draft.get("source").asText()).isEqualTo("template");
        assertThat(draft.get("body").asText()).doesNotContain("130000");
    }

    @Test
    void anotherUsersApplicationIsNotFound() throws Exception {
        String owner = register("draft-iso-owner@example.com");
        String other = register("draft-iso-other@example.com");
        String applicationId = createApplication(owner, "Acme", "Engineer");

        draft(other, applicationId, "follow_up", 404);
    }

    @Test
    void anotherUsersInterviewIsNotFound() throws Exception {
        String owner = register("draft-int-owner@example.com");
        String other = register("draft-int-other@example.com");
        String ownerApplication = createApplication(owner, "Acme", "Engineer");
        String otherApplication = createApplication(other, "Acme", "Engineer");
        Map<String, Object> interview = new HashMap<>();
        interview.put("application_id", ownerApplication);
        interview.put("round_number", 1);
        interview.put("round_type", "TECHNICAL");
        String interviewId = send("/api/v1/interviews", owner, interview).get("id").asText();

        mockMvc.perform(post("/api/v1/applications/" + otherApplication + "/email-drafts")
                        .header("Authorization", "Bearer " + other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\": \"thank_you\", \"interview_id\": \"" + interviewId + "\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void promptNeverContainsAnotherUsersRecords() throws Exception {
        String other = register("draft-ground-other@example.com");
        String otherApplication = createApplication(other, "Zorbex Industries", "Staff Haskell Wizard");
        send("/api/v1/offers", other, Map.of("application_id", otherApplication, "base_salary", 777777));
        String token = register("draft-ground-owner@example.com");
        String applicationId = createApplication(token, "Acme", "Engineer");
        send("/api/v1/offers", token, Map.of("application_id", applicationId, "base_salary", 95000));
        provider.replyWith("Hi, thank you for the offer. I would like to talk about compensation for the Engineer role at Acme. "
                + "Best regards, [Your name]");

        draft(token, applicationId, "negotiation", 200);

        assertThat(provider.everythingSent()).contains("Acme", "95000")
                .doesNotContain("Zorbex").doesNotContain("777777").doesNotContain("Haskell");
    }

    @Test
    void referralRequestTemplateUsesTheContactAndRoleAndLeavesAnEasyExit() throws Exception {
        String token = register("draft-referral@example.com");
        String applicationId = createApplication(token, "Acme", "Engineer");
        String contactId = createContact(token, "Meera Iyer", applicationId, "Staff Engineer");
        provider.goDown();

        JsonNode draft = contactDraft(token, contactId, "referral_request", 200);

        assertThat(draft.get("source").asText()).isEqualTo("template");
        assertThat(draft.get("ai_available").asBoolean()).isFalse();
        assertThat(draft.get("subject").asText()).isEqualTo("Referral for the Engineer role at Acme");
        assertThat(draft.get("body").asText()).startsWith("Hi Meera,")
                .contains("the Engineer role at Acme").contains("Staff Engineer").contains("completely fine");
    }

    @Test
    void reconnectAndThankYouDraftsAreAvailableFromTheApplicationToo() throws Exception {
        String token = register("draft-reconnect@example.com");
        String applicationId = createApplication(token, "Acme", "Engineer");
        createContact(token, "Meera Iyer", applicationId, null);
        provider.goDown();

        assertThat(draft(token, applicationId, "reconnect", 200).get("body").asText())
                .startsWith("Hi Meera,").contains("It has been a while");
        assertThat(draft(token, applicationId, "referral_thanks", 200).get("body").asText())
                .contains("Thank you for referring me");
    }

    @Test
    void referralReplyWithAnInventedNumberFallsBackToTheTemplate() throws Exception {
        String token = register("draft-referral-number@example.com");
        String applicationId = createApplication(token, "Acme", "Engineer");
        String contactId = createContact(token, "Meera Iyer", applicationId, null);
        provider.replyWith("Hi Meera, I have 7 years of experience and I am applying for the Engineer role at Acme. "
                + "Would you refer me? Thanks, [Your name]");

        JsonNode draft = contactDraft(token, contactId, "referral_request", 200);

        assertThat(draft.get("source").asText()).isEqualTo("template");
        assertThat(draft.get("body").asText()).doesNotContain("7 years");
    }

    @Test
    void anotherUsersContactIsNotFoundAndPromptsStayGrounded() throws Exception {
        String owner = register("draft-contact-owner@example.com");
        String other = register("draft-contact-other@example.com");
        String ownerApplication = createApplication(owner, "Zorbex Industries", "Staff Haskell Wizard");
        String ownerContact = createContact(owner, "Quentin Zorbex", ownerApplication, null);
        String otherApplication = createApplication(other, "Acme", "Engineer");
        String otherContact = createContact(other, "Meera Iyer", otherApplication, null);

        contactDraft(other, ownerContact, "referral_request", 404);

        mockMvc.perform(post("/api/v1/applications/" + otherApplication + "/email-drafts")
                        .header("Authorization", "Bearer " + other)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\": \"reconnect\", \"contact_id\": \"" + ownerContact + "\"}"))
                .andExpect(status().isNotFound());

        provider.replyWith("Hi Meera, I am applying for the Engineer role at Acme. Would you be open to referring me? "
                + "No problem if not. Thanks, [Your name]");
        contactDraft(other, otherContact, "referral_request", 200);

        assertThat(provider.everythingSent()).contains("Meera", "Acme")
                .doesNotContain("Zorbex").doesNotContain("Quentin").doesNotContain("Haskell");
    }

    private JsonNode contactDraft(String token, String contactId, String kind, int expected) throws Exception {
        String body = mockMvc.perform(post("/api/v1/contacts/" + contactId + "/message-drafts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\": \"" + kind + "\"}"))
                .andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private String createContact(String token, String name, String applicationId, String roleTitle) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("name", name);
        body.put("application_id", applicationId);
        if (roleTitle != null) {
            body.put("role_title", roleTitle);
        }
        return send("/api/v1/contacts", token, body).get("id").asText();
    }

    private JsonNode draft(String token, String applicationId, String kind, int expected) throws Exception {
        String body = mockMvc.perform(post("/api/v1/applications/" + applicationId + "/email-drafts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"kind\": \"" + kind + "\"}"))
                .andExpect(status().is(expected)).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private String createApplication(String token, String company, String title) throws Exception {
        return send("/api/v1/applications", token, Map.of(
                "company_name", company, "job_title", title,
                "date_applied", LocalDate.now().toString(), "status", "APPLIED")).get("id").asText();
    }

    private JsonNode send(String path, String token, Map<String, Object> body) throws Exception {
        String response = mockMvc.perform(post(path)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(result -> assertThat(result.getResponse().getStatus()).isIn(200, 201))
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response);
    }

    private String register(String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "a-genuinely-fine-password", "display_name": "Tester"}
                                """.formatted(email)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(response).get("access_token").asText();
    }
}
