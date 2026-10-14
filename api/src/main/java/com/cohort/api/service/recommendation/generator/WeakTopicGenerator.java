package com.cohort.api.service.recommendation.generator;

import com.cohort.api.service.recommendation.EvidenceItem;
import com.cohort.api.service.recommendation.RecommendationCandidate;
import com.cohort.api.service.recommendation.RecommendationContext;
import com.cohort.api.service.recommendation.TopicMasterySnapshot;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class WeakTopicGenerator extends RecommendationGenerator {
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
