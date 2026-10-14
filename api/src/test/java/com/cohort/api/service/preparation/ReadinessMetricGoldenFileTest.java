package com.cohort.api.service.preparation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.cohort.api.service.preparation.ReadinessMetric.WeightedTopic;
import com.cohort.api.service.preparation.analytics.AnalyticsResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReadinessMetricGoldenFileTest {
    private final ReadinessMetric metric = new ReadinessMetric();

    @Test
    void everyFixtureCaseMatchesItsExpectedResult() throws Exception {
        JsonNode cases;
        try (InputStream in = getClass().getResourceAsStream("/fixtures/readiness/cases.json")) {
            cases = new ObjectMapper().readTree(in);
        }
        for (JsonNode fixture : cases) {
            List<WeightedTopic> topics = new ArrayList<>();
            for (JsonNode t : fixture.get("topics")) {
                topics.add(new WeightedTopic(
                        UUID.randomUUID(), t.get("name").asText(), t.get("weight").asInt(),
                        t.get("mastery").asDouble(), t.get("evidence").asBoolean()));
            }
            JsonNode expected = fixture.get("expected");

            AnalyticsResult<Double> result = metric.evaluate(new ReadinessMetric.Input(topics));

            String name = fixture.get("name").asText();
            assertThat(result.value()).as(name).isCloseTo(expected.get("value").asDouble(), within(1e-9));
            assertThat(result.formatted()).as(name).isEqualTo(expected.get("formatted").asText());
            assertThat(result.sampleSize()).as(name).isEqualTo(expected.get("sampleSize").asInt());
            assertThat(result.sufficientData()).as(name).isEqualTo(expected.get("sufficientData").asBoolean());
        }
    }
}
