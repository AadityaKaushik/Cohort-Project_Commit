package com.cohort.api.service.fl;

import java.util.List;

/**
 * Exactly the 24 named, bounded floats {@link FlFeatureSchema} defines, in
 * schema order (US-58). Every field is a primitive {@code double} — by
 * construction there is no field type here that could admit a string, an
 * identifier, or free text; that is what P51's allow-list test checks.
 */
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

    /** In {@link FlFeatureSchema#FEATURE_NAMES} order -- the exact wire-format array. */
    public double[] toOrderedArray() {
        return new double[] {
            masteryMean, masteryMin, masteryStd, practiceVolume, topicBreadth, practiceRecency,
            practiceConsistency, confidenceMean, confidenceTrend, hardRatio, shareDsa, shareTechnical,
            shareBehavioural, shareSystemDesign, taskCompletionRate, prepLeadTime, interviewExperience,
            roundsPerApplication, recentSelfRating, coverageRatio, repeatTopicRatio, applicationVolume,
            resumeVariants, roundTypeOrdinal
        };
    }

    /** name -&gt; value, in schema order — what the export job actually serialises (P51's "matches the allow-list exactly" test). */
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
