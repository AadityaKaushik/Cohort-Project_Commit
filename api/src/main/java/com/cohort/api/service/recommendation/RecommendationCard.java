package com.cohort.api.service.recommendation;

import com.cohort.api.service.recommendation.tiering.EvidenceTier;
import com.cohort.api.service.recommendation.template.RenderedRecommendationCopy;
import java.util.Objects;
import java.util.UUID;

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
