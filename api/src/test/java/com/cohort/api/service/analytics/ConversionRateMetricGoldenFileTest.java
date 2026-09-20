package com.cohort.api.service.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.cohort.api.service.preparation.analytics.AnalyticsResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * P33's mandatory verify: golden-file test pinning {@link ConversionRateMetric}
 * against PROJECT_PLAN.md §20.7 ("reached next stage / reached this stage,
 * over resolved applications only") and FR-67 (denominators under 5 marked
 * low confidence).
 *
 * <ul>
 *   <li>case1: an adequate sample (n=10) -&gt; a confident, formatted rate.</li>
 *   <li>case2: a below-threshold sample (n=3) -&gt; the raw value is still
 *       computed (never hidden), but marked insufficient, per §21.4's
 *       "confidence gating" philosophy applied here to rates instead of mastery.</li>
 *   <li>case3: zero observations -&gt; no division by zero, reported as
 *       insufficient rather than crashing or silently showing 0%.</li>
 * </ul>
 */
class ConversionRateMetricGoldenFileTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final double TOLERANCE = 1e-9;

    private final ConversionRateMetric metric = new ConversionRateMetric();

    @ParameterizedTest
    @ValueSource(strings = {"case1", "case2", "case3"})
    void rateMatchesTheGoldenFixtureExactly(String caseName) throws Exception {
        JsonNode input = readFixture(caseName + "-input.json");
        JsonNode expected = readFixture(caseName + "-expected.json");

        ConversionRateMetric.Input metricInput =
                new ConversionRateMetric.Input(input.get("numerator").asLong(), input.get("denominator").asLong());

        AnalyticsResult<Double> result = metric.evaluate(metricInput);

        assertThat(result.value()).isCloseTo(expected.get("value").asDouble(), within(TOLERANCE));
        assertThat(result.formatted()).isEqualTo(expected.get("formatted").asText());
        assertThat(result.sampleSize()).isEqualTo(expected.get("sampleSize").asInt());
        assertThat(result.sufficientData()).isEqualTo(expected.get("sufficientData").asBoolean());
    }

    private JsonNode readFixture(String fileName) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/fixtures/analytics/conversion/" + fileName)) {
            return MAPPER.readTree(in);
        }
    }
}
