package com.cohort.api.service.recommendation.generator;

import com.cohort.api.service.recommendation.EvidenceItem;
import com.cohort.api.service.recommendation.RecommendationCandidate;
import com.cohort.api.service.recommendation.RecommendationContext;
import com.cohort.api.service.recommendation.TopicMasterySnapshot;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

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
