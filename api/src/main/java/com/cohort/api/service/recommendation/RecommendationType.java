package com.cohort.api.service.recommendation;

/**
 * Mirrors the {@code recommendations.type} CHECK constraint (V17). Lowercase
 * constant names, matching this codebase's existing convention for enums
 * that map 1:1 onto a text column ({@code Topic.Category},
 * {@code PreparationTask.Status}) rather than going through a converter.
 *
 * <p>Only the generators actually built in P39/P40 are represented here --
 * {@code coverage}, {@code missing_debrief}, {@code cadence}, and
 * {@code resume_pattern} from PROJECT_PLAN.md §21.3's full generator table
 * are out of scope for this sprint slice (see the P39 commit body).
 * {@code prerequisite_gap} is never produced by a generator directly: it is
 * what {@link RecommendationType#weak_topic} becomes after P40's root-cause
 * router redirects it to a weaker prerequisite (US-52).
 */
public enum RecommendationType {
    weak_topic,
    prerequisite_gap,
    decay,
    stalled_application,
    baseline
}
