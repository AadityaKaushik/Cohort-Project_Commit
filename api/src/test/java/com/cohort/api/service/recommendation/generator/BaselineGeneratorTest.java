package com.cohort.api.service.recommendation.generator;

import static org.assertj.core.api.Assertions.assertThat;

import com.cohort.api.service.recommendation.RecommendationCandidate;
import com.cohort.api.service.recommendation.RecommendationContext;
import com.cohort.api.service.recommendation.RecommendationType;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BaselineGeneratorTest {

    private final BaselineGenerator generator = new BaselineGenerator();

    @Test
    void firesWhenFewerThanThreeInterviewsAreLogged() {
        List<RecommendationCandidate> result = generator.handle(contextWithInterviewCount(0), List.of());

        assertThat(result).hasSize(1);
        RecommendationCandidate candidate = result.get(0);
        assertThat(candidate.type()).isEqualTo(RecommendationType.baseline);
        assertThat(candidate.evidence()).isEmpty();
    }

    @Test
    void doesNotFireOnceThreeInterviewsAreLogged() {
        assertThat(generator.handle(contextWithInterviewCount(3), List.of())).isEmpty();
    }

    @Test
    void doesNotFireWithMoreThanThreeInterviews() {
        assertThat(generator.handle(contextWithInterviewCount(10), List.of())).isEmpty();
    }

    private RecommendationContext contextWithInterviewCount(int interviewCount) {
        return new RecommendationContext(UUID.randomUUID(), Instant.now(), List.of(), Map.of(), Map.of(), List.of(), interviewCount);
    }
}
