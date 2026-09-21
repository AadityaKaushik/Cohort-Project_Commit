package com.cohort.api.service.recommendation.generator;

import com.cohort.api.service.recommendation.EvidenceItem;
import com.cohort.api.service.recommendation.RecommendationCandidate;
import com.cohort.api.service.recommendation.RecommendationContext;
import com.cohort.api.service.recommendation.TopicMasterySnapshot;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * US-51 / PROJECT_PLAN.md §21.3: "Mastery has fallen below 0.5 purely
 * through time decay." Deliberately disjoint from {@link WeakTopicGenerator}:
 * this fires only in the band {@code [0.4, 0.5)} where the <em>decayed</em>
 * score has dropped under the "comfortable" 0.5 mark while the
 * <em>undecayed</em> raw score ({@link TopicMasterySnapshot#rawScore()}) is
 * still &gt;= 0.5 -- i.e. the evidence itself was fine, it is staleness alone
 * that makes the topic look weak. A topic whose raw score has also dropped
 * already qualifies for {@link WeakTopicGenerator} instead (mastery &lt; 0.4)
 * or isn't reported by either generator, so no topic is ever emitted twice
 * between the two.
 */
@Component
public class DecayGenerator extends RecommendationGenerator {

    static final double DECAY_CEILING = 0.5;
    static final double RAW_SCORE_FLOOR = 0.5;

    @Override
    protected List<RecommendationCandidate> generate(RecommendationContext context) {
        List<RecommendationCandidate> candidates = new ArrayList<>();
        for (TopicMasterySnapshot snapshot : context.topicMasteries()) {
            boolean decayedBelowComfortable = snapshot.masteryScore() >= WeakTopicGenerator.WEAK_MASTERY_THRESHOLD
                    && snapshot.masteryScore() < DECAY_CEILING;
            boolean rawScoreWasFine = snapshot.rawScore() >= RAW_SCORE_FLOOR;
            if (snapshot.sufficientEvidence() && decayedBelowComfortable && rawScoreWasFine) {
                candidates.add(RecommendationCandidate.decay(snapshot, evidenceFor(snapshot)));
            }
        }
        return candidates;
    }

    private List<EvidenceItem> evidenceFor(TopicMasterySnapshot snapshot) {
        return List.of(new EvidenceItem(
                "Time decay",
                "Undecayed score %.0f%%, now %.0f%% after time decay (last practised %s)".formatted(
                        snapshot.rawScore() * 100, snapshot.masteryScore() * 100,
                        snapshot.lastEvidenceAt() == null ? "unknown" : snapshot.lastEvidenceAt().toString()),
                snapshot.lastEvidenceAt()));
    }
}
