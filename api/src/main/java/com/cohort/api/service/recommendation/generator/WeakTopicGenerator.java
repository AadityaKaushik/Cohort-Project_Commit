package com.cohort.api.service.recommendation.generator;

import com.cohort.api.service.recommendation.EvidenceItem;
import com.cohort.api.service.recommendation.RecommendationCandidate;
import com.cohort.api.service.recommendation.RecommendationContext;
import com.cohort.api.service.recommendation.TopicMasterySnapshot;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * US-51 / PROJECT_PLAN.md §21.3: "A topic's decayed mastery &lt; 0.4 with
 * &gt;=2 observations." The &gt;=2 threshold is exactly
 * {@link TopicMasterySnapshot#sufficientEvidence()} -- the same confidence
 * gate {@link com.cohort.api.service.preparation.MasteryEngine} already
 * enforces (§21.4) -- so a topic reported as insufficient evidence is never
 * turned into a recommendation (US-51: "topics with no supporting evidence
 * are never recommended").
 */
@Component
public class WeakTopicGenerator extends RecommendationGenerator {

    /** §21.3's own number, not this codebase's invention. */
    static final double WEAK_MASTERY_THRESHOLD = 0.4;

    @Override
    protected List<RecommendationCandidate> generate(RecommendationContext context) {
        List<RecommendationCandidate> candidates = new ArrayList<>();
        for (TopicMasterySnapshot snapshot : context.topicMasteries()) {
            if (snapshot.sufficientEvidence() && snapshot.masteryScore() < WEAK_MASTERY_THRESHOLD) {
                candidates.add(RecommendationCandidate.weakTopic(snapshot, evidenceFor(snapshot)));
            }
        }
        return candidates;
    }

    private List<EvidenceItem> evidenceFor(TopicMasterySnapshot snapshot) {
        return List.of(new EvidenceItem(
                "Mastery score",
                "%.0f%% mastery from %d observation%s".formatted(
                        snapshot.masteryScore() * 100, snapshot.observationCount(), snapshot.observationCount() == 1 ? "" : "s"),
                snapshot.lastEvidenceAt()));
    }
}
