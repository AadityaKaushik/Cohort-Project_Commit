package com.cohort.api.service.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.entity.ApplicationStatusHistory;
import com.cohort.api.service.preparation.analytics.AnalyticsResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FunnelMetricGoldenFileTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final FunnelMetric metric = new FunnelMetric();

    @ParameterizedTest
    @ValueSource(strings = {"case1"})
    void funnelCountsMatchTheGoldenFixtureExactly(String caseName) throws Exception {
        JsonNode input = readFixture("funnel/" + caseName + "-input.json");
        JsonNode expected = readFixture("funnel/" + caseName + "-expected.json");

        List<ApplicationSnapshot> snapshots = new ArrayList<>();
        for (JsonNode appNode : input.get("applications")) {
            ApplicationStatus status = ApplicationStatus.valueOf(appNode.get("status").asText());
            ApplicationStatus currentStage = ApplicationStatus.valueOf(appNode.get("currentStage").asText());
            UUID applicationId = UUID.randomUUID();

            List<ApplicationStatusHistory> history = new ArrayList<>();
            ApplicationStatus previous = null;
            for (JsonNode statusNode : appNode.get("history")) {
                ApplicationStatus toStatus = ApplicationStatus.valueOf(statusNode.asText());
                history.add(new ApplicationStatusHistory(applicationId, previous, toStatus, null));
                previous = toStatus;
            }
            snapshots.add(new ApplicationSnapshot(applicationId, status, currentStage, java.time.Instant.now(), history));
        }

        AnalyticsResult<FunnelMetric.FunnelCounts> result = metric.evaluate(snapshots);

        assertThat(result.value().applied()).isEqualTo(expected.get("applied").asLong());
        assertThat(result.value().reachedOa()).isEqualTo(expected.get("reachedOa").asLong());
        assertThat(result.value().reachedInterview()).isEqualTo(expected.get("reachedInterview").asLong());
        assertThat(result.value().reachedFinal()).isEqualTo(expected.get("reachedFinal").asLong());
        assertThat(result.value().reachedOffer()).isEqualTo(expected.get("reachedOffer").asLong());
        assertThat(result.sampleSize()).isEqualTo(expected.get("sampleSize").asInt());
        assertThat(result.sufficientData()).isEqualTo(expected.get("sufficientData").asBoolean());
    }

    private JsonNode readFixture(String fileName) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/fixtures/analytics/" + fileName)) {
            return MAPPER.readTree(in);
        }
    }
}
