package com.cohort.api.service.analytics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.cohort.api.service.preparation.analytics.AnalyticsResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

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
