package com.cohort.api.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cohort.api.AbstractIntegrationTest;
import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.entity.InterviewRoundType;
import com.cohort.api.event.MissingDebriefEvent;
import com.cohort.api.event.StalledApplicationEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@AutoConfigureMockMvc
@Import(AbstractIntegrationTest.ContainerConfig.class)
class InAppNotificationDeliveryIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void aPublishedEventBecomesOneInAppNotificationAndRepeatsAreDeduplicated() throws Exception {
        UUID userId = registerUser("delivery-owner@example.com");
        UUID otherUserId = registerUser("delivery-bystander@example.com");
        UUID applicationId = UUID.randomUUID();
        Instant enteredAt = Instant.parse("2026-08-01T09:00:00Z");
        StalledApplicationEvent event = new StalledApplicationEvent(
                userId, applicationId, "Acme", "Backend Engineer", ApplicationStatus.APPLIED, enteredAt, 25, 14);

        eventPublisher.publishEvent(event);
        eventPublisher.publishEvent(event);

        Map<String, Object> row = jdbcTemplate.queryForMap("SELECT * FROM notifications WHERE user_id = ?", userId);
        assertThat(row.get("type")).isEqualTo("stalled_application");
        assertThat(row.get("channel")).isEqualTo("in_app");
        assertThat(row.get("related_entity_type")).isEqualTo("application");
        assertThat(row.get("related_entity_id")).isEqualTo(applicationId);
        assertThat(row.get("is_read")).isEqualTo(false);
        assertThat(row.get("title")).isEqualTo("Your Acme application has stalled");
        assertThat(countFor(otherUserId)).isZero();

        eventPublisher.publishEvent(new StalledApplicationEvent(
                userId, applicationId, "Acme", "Backend Engineer", ApplicationStatus.OA, enteredAt.plusSeconds(3_000_000), 12, 10));
        assertThat(countFor(userId)).isEqualTo(2);
    }

    @Test
    void anEventPublishedInsideAReadOnlyTransactionIsStillPersisted() throws Exception {
        UUID userId = registerUser("delivery-readonly@example.com");
        MissingDebriefEvent event = new MissingDebriefEvent(
                userId, UUID.randomUUID(), UUID.randomUUID(), "Globex", "SRE", (short) 1, InterviewRoundType.TECHNICAL,
                Instant.parse("2026-09-20T10:00:00Z"), false);

        TransactionTemplate readOnly = new TransactionTemplate(transactionTemplate.getTransactionManager());
        readOnly.setReadOnly(true);
        readOnly.executeWithoutResult(status -> eventPublisher.publishEvent(event));

        assertThat(countFor(userId)).isEqualTo(1);
    }

    private long countFor(UUID userId) {
        return jdbcTemplate.queryForObject("SELECT count(*) FROM notifications WHERE user_id = ?", Long.class, userId);
    }

    private UUID registerUser(String email) throws Exception {
        String body = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "a-genuinely-fine-password", "display_name": "Tester"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode json = objectMapper.readTree(body);
        return UUID.fromString(json.get("user").get("id").asText());
    }
}
