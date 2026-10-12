package com.cohort.api.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.cohort.api.service.detection.NotificationDetectionJob;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * US-76..79 end to end against real Postgres: the scheduled detection job's
 * work (called directly rather than waiting for the timer) turns a stalled
 * application, an interview due in a few hours, and an interview that
 * happened without a debrief into three notifications -- exactly three, no
 * matter how many times detection runs -- which the bell's endpoints list,
 * count, and mark read, all strictly scoped to their owner (FR-95).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class NotificationIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private NotificationDetectionJob detectionJob;

    @Test
    void detectionProducesOneNotificationPerOccurrenceAndTheBellCanReadAndClearThem() throws Exception {
        Session owner = register("notif-owner@example.com");
        String stalledApplicationId = createApplication(owner.token(), "Acme");
        backdateStageEnteredAt(stalledApplicationId, 20);
        String interviewApplicationId = createApplication(owner.token(), "Globex");
        String upcomingInterviewId = createInterview(owner.token(), interviewApplicationId, 1, Instant.now().plus(Duration.ofHours(3)));
        String pastInterviewId = createInterview(owner.token(), interviewApplicationId, 2, Instant.now().minus(Duration.ofHours(6)));

        detectionJob.runFor(owner.userId(), Instant.now());
        detectionJob.runFor(owner.userId(), Instant.now());
        detectionJob.runDetection(Instant.now());

        JsonNode list = getJson(owner.token(), "/api/v1/notifications");
        assertThat(list.get("total").asLong()).isEqualTo(3);
        Map<String, JsonNode> byType = new HashMap<>();
        list.get("items").forEach(item -> byType.put(item.get("type").asText(), item));
        assertThat(byType.get("stalled_application").get("related_entity_type").asText()).isEqualTo("application");
        assertThat(byType.get("stalled_application").get("related_entity_id").asText()).isEqualTo(stalledApplicationId);
        assertThat(byType.get("stalled_application").get("title").asText()).isEqualTo("Your Acme application has stalled");
        assertThat(byType.get("upcoming_interview").get("related_entity_id").asText()).isEqualTo(upcomingInterviewId);
        assertThat(byType.get("missing_debrief").get("related_entity_id").asText()).isEqualTo(pastInterviewId);
        list.get("items").forEach(item -> assertThat(item.get("is_read").asBoolean()).isFalse());

        mockMvc.perform(get("/api/v1/notifications/unread-count").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unread_count", is(3)));

        String stalledNotificationId = byType.get("stalled_application").get("id").asText();
        mockMvc.perform(post("/api/v1/notifications/" + stalledNotificationId + "/read").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.is_read", is(true)));
        // Marking an already-read notification read again is harmless.
        mockMvc.perform(post("/api/v1/notifications/" + stalledNotificationId + "/read").header("Authorization", bearer(owner)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/notifications?status=unread").header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.total", is(2)));
        mockMvc.perform(get("/api/v1/notifications?status=read").header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].id", is(stalledNotificationId)))
                .andExpect(jsonPath("$.items[0].read_at").isNotEmpty());
        // "all" lists unread before read.
        mockMvc.perform(get("/api/v1/notifications").header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$.items[2].id", is(stalledNotificationId)));

        mockMvc.perform(post("/api/v1/notifications/read-all").header("Authorization", bearer(owner)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.marked_read", is(2)));
        mockMvc.perform(get("/api/v1/notifications/unread-count").header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$.unread_count", is(0)));

        // Still deduplicated after being read: a later detection run doesn't resurrect them.
        detectionJob.runFor(owner.userId(), Instant.now());
        mockMvc.perform(get("/api/v1/notifications/unread-count").header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$.unread_count", is(0)));
    }

    @Test
    void anotherUserCanNeitherSeeNorMarkSomeoneElsesNotifications() throws Exception {
        Session owner = register("notif-isolation-owner@example.com");
        String applicationId = createApplication(owner.token(), "Acme");
        backdateStageEnteredAt(applicationId, 30);
        detectionJob.runFor(owner.userId(), Instant.now());
        String notificationId = getJson(owner.token(), "/api/v1/notifications").get("items").get(0).get("id").asText();

        Session intruder = register("notif-isolation-intruder@example.com");

        mockMvc.perform(post("/api/v1/notifications/" + notificationId + "/read").header("Authorization", bearer(intruder)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code", is("NOTIFICATION_NOT_FOUND")));
        mockMvc.perform(get("/api/v1/notifications").header("Authorization", bearer(intruder)))
                .andExpect(jsonPath("$.total", is(0)));
        mockMvc.perform(post("/api/v1/notifications/read-all").header("Authorization", bearer(intruder)))
                .andExpect(jsonPath("$.marked_read", is(0)));
        mockMvc.perform(get("/api/v1/notifications/unread-count").header("Authorization", bearer(intruder)))
                .andExpect(jsonPath("$.unread_count", is(0)));

        // The owner's notification is untouched by any of the above.
        mockMvc.perform(get("/api/v1/notifications/unread-count").header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$.unread_count", is(1)));
    }

    @Test
    void anUnknownNotificationIsA404AndEveryEndpointRequiresAuthentication() throws Exception {
        Session owner = register("notif-misc@example.com");
        mockMvc.perform(post("/api/v1/notifications/" + UUID.randomUUID() + "/read").header("Authorization", bearer(owner)))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/notifications")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/notifications/unread-count")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/notifications/read-all")).andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/notifications/" + UUID.randomUUID() + "/read")).andExpect(status().isUnauthorized());
    }

    @Test
    void aFreshApplicationAndAFarOffInterviewProduceNothing() throws Exception {
        Session owner = register("notif-quiet@example.com");
        String applicationId = createApplication(owner.token(), "Initech");
        createInterview(owner.token(), applicationId, 1, Instant.now().plus(Duration.ofDays(5)));

        detectionJob.runFor(owner.userId(), Instant.now());

        mockMvc.perform(get("/api/v1/notifications").header("Authorization", bearer(owner)))
                .andExpect(jsonPath("$.total", is(0)))
                .andExpect(jsonPath("$.items", hasSize(0)));
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

    private void backdateStageEnteredAt(String applicationId, int daysAgo) {
        jdbcTemplate.update("UPDATE applications SET stage_entered_at = ? WHERE id = ?::uuid",
                Timestamp.from(Instant.now().minus(daysAgo, ChronoUnit.DAYS)), applicationId);
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

    private String createInterview(String token, String applicationId, int round, Instant scheduledAt) throws Exception {
        String body = mockMvc.perform(post("/api/v1/interviews")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"application_id": "%s", "round_number": %d, "round_type": "TECHNICAL", "scheduled_at": "%s"}
                                """.formatted(applicationId, round, scheduledAt)))
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
