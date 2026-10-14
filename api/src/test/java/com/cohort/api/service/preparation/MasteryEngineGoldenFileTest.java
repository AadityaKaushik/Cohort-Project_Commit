package com.cohort.api.service.preparation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.cohort.api.entity.Difficulty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MasteryEngineGoldenFileTest {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final double TOLERANCE = 1e-9;

    private final MasteryEngine engine = new MasteryEngine();

    static Stream<String> cases() {
        return Stream.of("case1", "case2", "case3");
    }

    @ParameterizedTest
    @ValueSource(strings = {"case1", "case2", "case3"})
    void engineOutputMatchesTheGoldenFixtureExactly(String caseName) throws Exception {
        JsonNode input = readFixture(caseName + "-input.json");
        JsonNode expected = readFixture(caseName + "-expected.json");

        Instant asOf = Instant.parse(input.get("asOf").asText());
        Difficulty difficulty = Difficulty.valueOf(input.get("difficulty").asText());

        List<PracticeEvidence> practice = new ArrayList<>();
        for (JsonNode node : input.get("practiceAttempts")) {
            practice.add(new PracticeEvidence(
                    (short) node.get("confidence").asInt(), Instant.parse(node.get("attemptedAt").asText())));
        }

        List<InterviewEvidence> interview = new ArrayList<>();
        for (JsonNode node : input.get("interviewRatings")) {
            interview.add(new InterviewEvidence(
                    (short) node.get("selfRating").asInt(), Instant.parse(node.get("occurredAt").asText())));
        }

        MasteryResult result = engine.computeMastery(practice, interview, difficulty, asOf);

        assertThat(result.rawScore()).isCloseTo(expected.get("rawScore").asDouble(), within(TOLERANCE));
        assertThat(result.masteryScore()).isCloseTo(expected.get("masteryScore").asDouble(), within(TOLERANCE));
        assertThat(result.observationCount()).isEqualTo(expected.get("observationCount").asInt());
        assertThat(result.sufficientEvidence()).isEqualTo(expected.get("sufficientEvidence").asBoolean());
    }

    private JsonNode readFixture(String fileName) throws Exception {
        try (InputStream in = getClass().getResourceAsStream("/fixtures/mastery/" + fileName)) {
            return MAPPER.readTree(in);
        }
    }
}
