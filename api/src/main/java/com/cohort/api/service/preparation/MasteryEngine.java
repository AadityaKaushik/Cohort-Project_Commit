package com.cohort.api.service.preparation;

import com.cohort.api.entity.Difficulty;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.OptionalDouble;
import org.springframework.stereotype.Component;

/**
 * US-39 / PROJECT_PLAN.md §21.4: the topic mastery formula, implemented
 * exactly as documented there — pure, seeded, and pinned by golden-file
 * tests ({@code MasteryEngineGoldenFileTest}) so a refactor that changes
 * the numbers by accident fails CI (NFR-51). This class has no
 * dependencies on any repository or entity beyond {@link Difficulty}; it
 * is handed evidence and an {@code asOf} instant and returns a score,
 * which is what makes golden-file testing meaningful in the first place.
 *
 * <p>Formula recap (§21.4):
 * <pre>
 * practice_component = Σ w_a·(confidence_a-1)/4 / Σ w_a,   w_a = exp(-ln2·days_ago/90)
 * interview_component = Σ w_q·(rating_q-1)/4 / Σ w_q,      w_q = exp(-ln2·days_ago/120)
 * raw = 0.4·practice_component + 0.6·interview_component (falls back to whichever exists if only one does)
 * mastery = raw · exp(-ln2·Δ/H_diff),  Δ = days since the most recent evidence of any kind
 * H_diff = 60 (hard) | 90 (medium) | 120 (easy)
 * </pre>
 * Confidence gating: with fewer than 2 total observations, the score is
 * still computed and returned, but {@code sufficientEvidence} is false —
 * "the score is stored but reported as insufficient evidence" (§21.4), so
 * the caller (not this class) decides whether to show it.
 */
@Component
public class MasteryEngine {

    private static final double PRACTICE_HALF_LIFE_DAYS = 90.0;
    private static final double INTERVIEW_HALF_LIFE_DAYS = 120.0;
    private static final double PRACTICE_WEIGHT = 0.4;
    private static final double INTERVIEW_WEIGHT = 0.6;
    private static final int MINIMUM_OBSERVATIONS = 2;
    private static final double LN2 = Math.log(2);

    public MasteryResult computeMastery(
            List<PracticeEvidence> practiceEvidence, List<InterviewEvidence> interviewEvidence,
            Difficulty topicDifficulty, Instant asOf) {

        int observationCount = practiceEvidence.size() + interviewEvidence.size();

        OptionalDouble practiceComponent = weightedComponent(
                practiceEvidence.stream().map(e -> new Evidence(e.confidence(), e.occurredAt())).toList(),
                asOf, PRACTICE_HALF_LIFE_DAYS);
        OptionalDouble interviewComponent = weightedComponent(
                interviewEvidence.stream().map(e -> new Evidence(e.selfRating(), e.occurredAt())).toList(),
                asOf, INTERVIEW_HALF_LIFE_DAYS);

        double raw = blend(practiceComponent, interviewComponent);

        Instant mostRecent = mostRecentEvidence(practiceEvidence, interviewEvidence);
        double masteryScore;
        if (mostRecent == null) {
            masteryScore = 0.0;
        } else {
            double deltaDays = daysBetween(mostRecent, asOf);
            double halfLife = halfLifeFor(topicDifficulty);
            masteryScore = raw * Math.exp(-LN2 * deltaDays / halfLife);
        }

        return new MasteryResult(raw, masteryScore, observationCount, observationCount >= MINIMUM_OBSERVATIONS);
    }

    /** §21.4's fallback rule: blend both components if both exist, else use whichever one does. */
    private double blend(OptionalDouble practiceComponent, OptionalDouble interviewComponent) {
        if (practiceComponent.isPresent() && interviewComponent.isPresent()) {
            return PRACTICE_WEIGHT * practiceComponent.getAsDouble() + INTERVIEW_WEIGHT * interviewComponent.getAsDouble();
        }
        if (practiceComponent.isPresent()) {
            return practiceComponent.getAsDouble();
        }
        if (interviewComponent.isPresent()) {
            return interviewComponent.getAsDouble();
        }
        return 0.0;
    }

    private OptionalDouble weightedComponent(List<Evidence> evidence, Instant asOf, double halfLifeDays) {
        if (evidence.isEmpty()) {
            return OptionalDouble.empty();
        }
        double weightedSum = 0.0;
        double weightTotal = 0.0;
        for (Evidence e : evidence) {
            double daysAgo = daysBetween(e.occurredAt(), asOf);
            double weight = Math.exp(-LN2 * daysAgo / halfLifeDays);
            double normalisedRating = (e.rating() - 1) / 4.0;
            weightedSum += weight * normalisedRating;
            weightTotal += weight;
        }
        return weightTotal == 0.0 ? OptionalDouble.empty() : OptionalDouble.of(weightedSum / weightTotal);
    }

    private Instant mostRecentEvidence(List<PracticeEvidence> practice, List<InterviewEvidence> interview) {
        Instant latest = null;
        for (PracticeEvidence e : practice) {
            latest = latest == null || e.occurredAt().isAfter(latest) ? e.occurredAt() : latest;
        }
        for (InterviewEvidence e : interview) {
            latest = latest == null || e.occurredAt().isAfter(latest) ? e.occurredAt() : latest;
        }
        return latest;
    }

    private double halfLifeFor(Difficulty difficulty) {
        return switch (difficulty) {
            case hard -> 60.0;
            case medium -> 90.0;
            case easy -> 120.0;
        };
    }

    /** Fractional days, not truncated to whole days, so the exponential decay is continuous rather than step-wise. */
    private double daysBetween(Instant from, Instant to) {
        return Duration.between(from, to).toMillis() / 86_400_000.0;
    }

    private record Evidence(short rating, Instant occurredAt) {
    }
}
