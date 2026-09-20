package com.cohort.api.service.analytics;

import static org.assertj.core.api.Assertions.assertThat;

import com.cohort.api.service.preparation.analytics.AnalyticsResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * P33's mandatory verify: golden-file test pinning {@link ActivityTimelineMetric}
 * (US-45) against a hand-computed fixture.
 *
 * <ul>
 *   <li>case1: a two-week range with applications and interviews landing in
 *       both weeks -&gt; each week's counts pinned exactly.</li>
 *   <li>case2: no activity at all -&gt; US-45's required empty state: a real
 *       bucket list (one empty week), sample size 0, still {@code sufficientData}
 *       (a raw count is never "insufficient," only a rate can be).</li>
 * </ul>
 */
class ActivityTimelineMetricGoldenFileTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ActivityTimelineMetric metric = new ActivityTimelineMetric();

    @ParameterizedTest
    @ValueSource(strings = {"case1", "case2"})
    void bucketsMatchTheGoldenFixtureExactly(String caseName) throws Exception {
        JsonNode input = readFixture(caseName + "-input.json");
        JsonNode expected = readFixture(caseName + "-expected.json");

        ActivityTimelineMetric.Input metricInput = new ActivityTimelineMetric.Input(
                toDates(input.get("applicationDates")),
                toDates(input.get("interviewDates")),
                LocalDate.parse(input.get("rangeStart").asText()),
                LocalDate.parse(input.get("rangeEnd").asText()));

        AnalyticsResult<List<ActivityTimelineMetric.WeekBucket>> result = metric.evaluate(metricInput);

        assertThat(result.sampleSize()).isEqualTo(expected.get("sampleSize").asInt());
        assertThat(result.sufficientData()).isEqualTo(expected.get("sufficientData").asBoolean());

        JsonNode expectedBuckets = expected.get("buckets");
        assertThat(result.value()).hasSize(expectedBuckets.size());
        for (int i = 0; i < expectedBuckets.size(); i++) {
            JsonNode expectedBucket = expectedBuckets.get(i);
            ActivityTimelineMetric.WeekBucket actual = result.value().get(i);
            assertThat(actual.weekStart()).isEqualTo(LocalDate.parse(expectedBucket.get("weekStart").asText()));
            assertThat(actual.applications()).isEqualTo(expectedBucket.get("applications").asLong());
            assertThat(actual.interviews()).isEqualTo(expectedBucket.get("interviews").asLong());
        }
    }

    private List<LocalDate> toDates(JsonNode arrayNode) {
        List<LocalDate> dates = new ArrayList<>();
        for (JsonNode node : arrayNode) {
            dates.add(LocalDate.parse(node.asText()));
        }
        return dates;
    }

    private JsonNode readFixture(String fileName) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/fixtures/analytics/timeline/" + fileName)) {
            return MAPPER.readTree(in);
        }
    }
}
