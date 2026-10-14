package com.cohort.api.service.fl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class FlFeatureSchema {
    public static final int FEATURE_COUNT = 24;

    public static final Map<String, double[]> FEATURE_BOUNDS = buildBounds();

    public static final List<String> FEATURE_NAMES = List.copyOf(FEATURE_BOUNDS.keySet());

    public static final List<String> NOT_COLLECTED = List.of(
            "free text", "names", "company names", "job titles", "dates/timestamps",
            "interviewer names", "resume content", "question text", "salary", "location");

    private FlFeatureSchema() {
    }

    private static Map<String, double[]> buildBounds() {
        Map<String, double[]> bounds = new LinkedHashMap<>();
        bounds.put("mastery_mean", new double[] {0.0, 1.0});
        bounds.put("mastery_min", new double[] {0.0, 1.0});
        bounds.put("mastery_std", new double[] {0.0, 1.0});
        bounds.put("practice_volume", new double[] {0.0, 1.0});
        bounds.put("topic_breadth", new double[] {0.0, 1.0});
        bounds.put("practice_recency", new double[] {0.0, 1.0});
        bounds.put("practice_consistency", new double[] {0.0, 1.0});
        bounds.put("confidence_mean", new double[] {0.0, 1.0});
        bounds.put("confidence_trend", new double[] {-1.0, 1.0});
        bounds.put("hard_ratio", new double[] {0.0, 1.0});
        bounds.put("share_dsa", new double[] {0.0, 1.0});
        bounds.put("share_technical", new double[] {0.0, 1.0});
        bounds.put("share_behavioural", new double[] {0.0, 1.0});
        bounds.put("share_system_design", new double[] {0.0, 1.0});
        bounds.put("task_completion_rate", new double[] {0.0, 1.0});
        bounds.put("prep_lead_time", new double[] {0.0, 1.0});
        bounds.put("interview_experience", new double[] {0.0, 1.0});
        bounds.put("rounds_per_application", new double[] {0.0, 1.0});
        bounds.put("recent_self_rating", new double[] {0.0, 1.0});
        bounds.put("coverage_ratio", new double[] {0.0, 1.0});
        bounds.put("repeat_topic_ratio", new double[] {0.0, 1.0});
        bounds.put("application_volume", new double[] {0.0, 1.0});
        bounds.put("resume_variants", new double[] {0.0, 1.0});
        bounds.put("round_type_ordinal", new double[] {0.0, 1.0});
        if (bounds.size() != FEATURE_COUNT) {
            throw new IllegalStateException("FlFeatureSchema must declare exactly " + FEATURE_COUNT + " features");
        }
        return Collections.unmodifiableMap(bounds);
    }
}
