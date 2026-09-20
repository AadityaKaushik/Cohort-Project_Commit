package com.cohort.api.service.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.service.preparation.analytics.AnalyticsResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * P34's mandatory verify: golden-file test pinning {@link RejectionDistributionMetric}
 * (US-46) against a hand-computed fixture -- case1 has an adequate sample
 * (n=6), case2 is deliberately below FR-67's threshold, proving the
 * distribution is still computed (never hidden) but marked insufficient.
 */
class RejectionDistributionMetricGoldenFileTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RejectionDistributionMetric metric = new RejectionDistributionMetric();

    @ParameterizedTest
    @ValueSource(strings = {"case1", "case2"})
    void distributionMatchesTheGoldenFixtureExactly(String caseName) throws Exception {
        JsonNode input = readFixture(caseName + "-input.json");
        JsonNode expected = readFixture(caseName + "-expected.json");

        List<ApplicationStatus> lastStages = new ArrayList<>();
        for (JsonNode node : input.get("lastStages")) {
            lastStages.add(ApplicationStatus.valueOf(node.asText()));
        }

        AnalyticsResult<Map<ApplicationStatus, Long>> result = metric.evaluate(lastStages);

        JsonNode expectedCounts = expected.get("counts");
        expectedCounts.fieldNames().forEachRemaining(stageName -> {
            ApplicationStatus stage = ApplicationStatus.valueOf(stageName);
            assertThat(result.value().get(stage)).isEqualTo(expectedCounts.get(stageName).asLong());
        });
        assertThat(result.value().keySet()).hasSize(expectedCounts.size());

        assertThat(result.sampleSize()).isEqualTo(expected.get("sampleSize").asInt());
        assertThat(result.sufficientData()).isEqualTo(expected.get("sufficientData").asBoolean());
    }

    private JsonNode readFixture(String fileName) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/fixtures/analytics/rejection/" + fileName)) {
            return MAPPER.readTree(in);
        }
    }
}
