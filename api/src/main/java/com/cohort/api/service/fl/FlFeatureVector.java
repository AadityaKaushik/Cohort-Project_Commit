package com.cohort.api.service.fl;

import java.util.List;

public record FlFeatureVector(
        double masteryMean,
        double masteryMin,
        double masteryStd,
        double practiceVolume,
        double topicBreadth,
        double practiceRecency,
        double practiceConsistency,
        double confidenceMean,
        double confidenceTrend,
        double hardRatio,
        double shareDsa,
        double shareTechnical,
        double shareBehavioural,
        double shareSystemDesign,
        double taskCompletionRate,
        double prepLeadTime,
        double interviewExperience,
        double roundsPerApplication,
        double recentSelfRating,
        double coverageRatio,
        double repeatTopicRatio,
        double applicationVolume,
        double resumeVariants,
        double roundTypeOrdinal) {
    public double[] toOrderedArray() {
        return new double[] {
            masteryMean, masteryMin, masteryStd, practiceVolume, topicBreadth, practiceRecency,
            practiceConsistency, confidenceMean, confidenceTrend, hardRatio, shareDsa, shareTechnical,
            shareBehavioural, shareSystemDesign, taskCompletionRate, prepLeadTime, interviewExperience,
            roundsPerApplication, recentSelfRating, coverageRatio, repeatTopicRatio, applicationVolume,
            resumeVariants, roundTypeOrdinal
        };
    }

    public java.util.Map<String, Double> toNamedMap() {
        double[] values = toOrderedArray();
        List<String> names = FlFeatureSchema.FEATURE_NAMES;
        java.util.Map<String, Double> map = new java.util.LinkedHashMap<>();
        for (int i = 0; i < names.size(); i++) {
            map.put(names.get(i), values[i]);
        }
        return map;
    }
}
