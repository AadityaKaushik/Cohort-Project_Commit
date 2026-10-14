package com.cohort.api.service.preparation;

import com.cohort.api.service.preparation.analytics.AnalyticsMetric;
import java.util.List;
import java.util.UUID;

public class ReadinessMetric extends AnalyticsMetric<ReadinessMetric.Input, Double> {
    public static final int MINIMUM_TOPICS_WITH_EVIDENCE = 2;

    public record WeightedTopic(UUID topicId, String topicName, int weight, double masteryScore, boolean hasEvidence) {
    }

    public record Input(List<WeightedTopic> topics) {
    }

    @Override
    protected int sampleSize(Input input) {
        return (int) input.topics().stream().filter(WeightedTopic::hasEvidence).count();
    }

    @Override
    protected Double compute(Input input) {
        double weightedSum = 0.0;
        double weightTotal = 0.0;
        for (WeightedTopic topic : input.topics()) {
            if (topic.hasEvidence()) {
                weightedSum += topic.weight() * topic.masteryScore();
                weightTotal += topic.weight();
            }
        }
        return weightTotal == 0.0 ? 0.0 : weightedSum / weightTotal;
    }

    @Override
    protected int minimumSampleSize() {
        return MINIMUM_TOPICS_WITH_EVIDENCE;
    }

    @Override
    protected String format(Double value, boolean sufficientData) {
        if (!sufficientData) {
            return "insufficient data";
        }
        return Math.round(value * 100) + "%";
    }
}
