package com.cohort.api.service.recommendation;

import com.cohort.api.service.recommendation.tiering.EvidenceTier;
import com.cohort.api.service.recommendation.template.RenderedRecommendationCopy;
import java.util.Objects;
import java.util.UUID;

/**
 * Builder pattern (ARCHITECTURE_REVISION_JAVA.md §4.1): the fully-assembled
 * recommendation, ready to persist and return -- assembled from the three
 * prior pipeline stages (§21.2: generate -&gt; route -&gt; score -&gt; template)
 * via a fluent builder rather than a single telescoping constructor.
 * {@code RecommendationFacade} is the only caller, and it sets each part as
 * that stage of the pipeline finishes:
 * {@code RecommendationCard.builder().forUser(userId).withCandidate(routed)
 * .withTierAndScore(tier, score).withCopy(renderedCopy).build()} -- reading
 * as a sentence in pipeline order is the point; a constructor taking six
 * positional arguments (several of them {@code double}/{@code UUID}, easy
 * to transpose by accident) would not.
 */
public final class RecommendationCard {

    private final UUID userId;
    private final RecommendationCandidate candidate;
    private final EvidenceTier tier;
    private final double score;
    private final String headline;
    private final String explanation;

    private RecommendationCard(Builder builder) {
        this.userId = Objects.requireNonNull(builder.userId, "userId");
        this.candidate = Objects.requireNonNull(builder.candidate, "candidate");
        this.tier = Objects.requireNonNull(builder.tier, "tier");
        this.score = builder.score;
        this.headline = Objects.requireNonNull(builder.headline, "headline");
        this.explanation = Objects.requireNonNull(builder.explanation, "explanation");
    }

    public static Builder builder() {
        return new Builder();
    }

    public UUID userId() {
        return userId;
    }

    public RecommendationCandidate candidate() {
        return candidate;
    }

    public EvidenceTier tier() {
        return tier;
    }

    public double score() {
        return score;
    }

    public String headline() {
        return headline;
    }

    public String explanation() {
        return explanation;
    }

    public static final class Builder {

        private UUID userId;
        private RecommendationCandidate candidate;
        private EvidenceTier tier;
        private double score;
        private String headline;
        private String explanation;

        private Builder() {
        }

        public Builder forUser(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder withCandidate(RecommendationCandidate candidate) {
            this.candidate = candidate;
            return this;
        }

        public Builder withTierAndScore(EvidenceTier tier, double score) {
            this.tier = tier;
            this.score = score;
            return this;
        }

        public Builder withCopy(RenderedRecommendationCopy copy) {
            this.headline = copy.headline();
            this.explanation = copy.explanation();
            return this;
        }

        public RecommendationCard build() {
            return new RecommendationCard(this);
        }
    }
}
