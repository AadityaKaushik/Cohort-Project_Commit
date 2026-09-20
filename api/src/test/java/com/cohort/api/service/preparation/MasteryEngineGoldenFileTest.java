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

/**
 * P30's mandatory verify (BUILD_PHASES_JAVA.md): golden-file tests against
 * fixed fixtures, proving {@link MasteryEngine} implements PROJECT_PLAN.md
 * §21.4 exactly and stays correct under refactoring (NFR-51). Each fixture
 * pair under {@code src/test/resources/fixtures/mastery/} is a fixed set of
 * practice attempts/interview ratings with fixed timestamps
 * ({@code caseN-input.json}) and the formula's expected output, independently
 * hand-computed from the documented formula
 * ({@code caseN-expected.json}) — not derived from running the engine
 * itself, which would make the test circular.
 *
 * <ul>
 *   <li>case1: both practice and interview evidence present -> blended 0.4/0.6.</li>
 *   <li>case2: practice-only evidence -> §21.4's fallback rule (raw = practice_component alone).</li>
 *   <li>case3: a single observation -> insufficient evidence, but the score is still computed, not suppressed.</li>
 * </ul>
 */
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
