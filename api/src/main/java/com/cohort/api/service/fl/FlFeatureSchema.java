package com.cohort.api.service.fl;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Feature schema v1, Java's own copy of the single source of truth (US-58,
 * PROJECT_PLAN.md §22.2). <b>This list, its order, and its bounds must be
 * kept in sync by hand with {@code fl-service/app/common/schema.py}'s
 * {@code FEATURE_BOUNDS}</b> — that file is the canonical definition (it
 * has its own schema test asserting exactly 24 named floats); this class
 * is the allow-list the Java-side export job (P51) and the {@code
 * GET /fl/schema} endpoint (P54) are built against on this side of the
 * language boundary. There is deliberately no automated cross-language
 * sync check in this batch — a human keeping the two lists identical on
 * review is the control, exactly like {@code InterviewRoundType}
 * (Java enum) already has to be kept in sync with
 * {@code app/client/feature_extractor.py}'s {@code ROUND_TYPE_ORDER} by
 * hand.
 *
 * <p>What is provably absent by construction, same guarantee as the Python
 * source of truth: no strings, no user/record identifiers, no company
 * names, no job titles, no dates, no free text. {@link FlFeatureVector}'s
 * fields are all {@code double}, so there is no field type this allow-list
 * could accidentally admit a PII string through.
 */
public final class FlFeatureSchema {

    public static final int FEATURE_COUNT = 24;

    /** name -&gt; [lowerBound, upperBound], in the exact order the Python schema declares them. */
    public static final Map<String, double[]> FEATURE_BOUNDS = buildBounds();

    public static final List<String> FEATURE_NAMES = List.copyOf(FEATURE_BOUNDS.keySet());

    /** Shown on the opt-in consent screen (US-57) alongside the allow-list itself; mirrors fl-service's {@code /fl/schema} "not_collected" list. */
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
        // Collections.unmodifiableMap (not Map.copyOf) deliberately -- it wraps the
        // LinkedHashMap instead of re-keying into an order-agnostic immutable map,
        // which is required here: FEATURE_NAMES' order IS the wire-format order.
        return Collections.unmodifiableMap(bounds);
    }
}
