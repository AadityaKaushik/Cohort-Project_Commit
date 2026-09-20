package com.cohort.api.service.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.cohort.api.service.preparation.analytics.AnalyticsResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * P34's mandatory verify: golden-file test pinning {@link StageDwellTimeMetric}
 * against a hand-computed fixture, using the Tukey's-hinges method documented
 * on the class itself.
 *
 * <ul>
 *   <li>case1: n=7 (odd, &gt;= 5) -&gt; confident median/IQR.</li>
 *   <li>case2: n=4 (even, &lt; 5) -&gt; still computed, marked insufficient (§20.7's
 *       "suppressed below 5 completed observations" means hidden by the UI,
 *       not un-computed by this class -- same confidence-gating philosophy
 *       as every other metric here).</li>
 *   <li>case3: n=1 -&gt; degenerate case, median/p25/p75 all equal the one value.</li>
 * </ul>
 */
class StageDwellTimeMetricGoldenFileTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final double TOLERANCE = 1e-9;

    private final StageDwellTimeMetric metric = new StageDwellTimeMetric();

    @ParameterizedTest
    @ValueSource(strings = {"case1", "case2", "case3"})
    void dwellStatsMatchTheGoldenFixtureExactly(String caseName) throws Exception {
        JsonNode input = readFixture(caseName + "-input.json");
        JsonNode expected = readFixture(caseName + "-expected.json");

        List<Double> durations = new ArrayList<>();
        for (JsonNode node : input.get("durationsDays")) {
            durations.add(node.asDouble());
        }

        AnalyticsResult<StageDwellTimeMetric.DwellStats> result = metric.evaluate(durations);

        assertThat(result.value().medianDays()).isCloseTo(expected.get("medianDays").asDouble(), within(TOLERANCE));
        assertThat(result.value().p25Days()).isCloseTo(expected.get("p25Days").asDouble(), within(TOLERANCE));
        assertThat(result.value().p75Days()).isCloseTo(expected.get("p75Days").asDouble(), within(TOLERANCE));
        assertThat(result.sampleSize()).isEqualTo(expected.get("sampleSize").asInt());
        assertThat(result.sufficientData()).isEqualTo(expected.get("sufficientData").asBoolean());
    }

    private JsonNode readFixture(String fileName) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/fixtures/analytics/dwell/" + fileName)) {
            return MAPPER.readTree(in);
        }
    }
}
