package com.cohort.api.service.report;

import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.entity.ApplicationStatusHistory;
import com.cohort.api.service.analytics.ApplicationSnapshot;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.test.util.ReflectionTestUtils;

/** Loads {@code fixtures/reports/*} -- the builder's inputs and its expected, hand-checked outputs. */
final class ReportFixtures {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ReportFixtures() {
    }

    record Case(String owner, Instant generatedAt, List<ReportApplicationInput> applications) {
    }

    static Case load(String name) {
        JsonNode root = readJson(name + "-input.json");
        List<ReportApplicationInput> applications = new ArrayList<>();
        for (JsonNode app : root.get("applications")) {
            UUID applicationId = UUID.randomUUID();
            List<ApplicationStatusHistory> history = new ArrayList<>();
            ApplicationStatus previous = null;
            ApplicationStatus currentStage = ApplicationStatus.valueOf(app.get("currentStage").asText());
            Instant stageEnteredAt = null;
            for (JsonNode step : app.get("history")) {
                ApplicationStatus to = ApplicationStatus.valueOf(step.get(0).asText());
                Instant at = Instant.parse(step.get(1).asText() + "T10:00:00Z");
                ApplicationStatusHistory entry = new ApplicationStatusHistory(applicationId, previous, to, null);
                ReflectionTestUtils.setField(entry, "changedAt", at);
                history.add(entry);
                if (to == currentStage) {
                    stageEnteredAt = at;
                }
                previous = to;
            }
            ApplicationSnapshot snapshot = new ApplicationSnapshot(
                    applicationId, ApplicationStatus.valueOf(app.get("status").asText()), currentStage, stageEnteredAt, history);
            applications.add(new ReportApplicationInput(
                    text(app, "company"), text(app, "title"), LocalDate.parse(app.get("dateApplied").asText()),
                    text(app, "location"), text(app, "workMode"), text(app, "priority"), text(app, "source"),
                    app.get("referral").asBoolean(), app.get("interviews").asInt(), snapshot));
        }
        return new Case(root.get("owner").asText(), Instant.parse(root.get("generatedAt").asText()), applications);
    }

    static String expected(String fileName) {
        try (InputStream in = ReportFixtures.class.getResourceAsStream("/fixtures/reports/" + fileName)) {
            if (in == null) {
                throw new IllegalStateException("Missing fixture /fixtures/reports/" + fileName);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static JsonNode readJson(String fileName) {
        try (InputStream in = ReportFixtures.class.getResourceAsStream("/fixtures/reports/" + fileName)) {
            return MAPPER.readTree(in);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : value.asText();
    }
}
