package com.cohort.api.entity;

/**
 * Round-type domain for interviews (FR-39, PROJECT_PLAN.md §17.2). Mirrors
 * {@link ApplicationStatus}'s split between a plain top-level enum and its
 * own {@link InterviewRoundTypeConverter}: {@code FINAL} cannot be a
 * lowercase Java identifier ({@code final} is a reserved keyword), so this
 * enum stays uppercase and the converter maps it to the lowercase
 * snake_case text the {@code interviews.round_type} CHECK constraint
 * expects, the same way {@code FINAL_ROUND} maps to {@code "final_round"}.
 */
public enum InterviewRoundType {
    OA,
    PHONE_SCREEN,
    TECHNICAL,
    SYSTEM_DESIGN,
    BEHAVIOURAL,
    HR,
    FINAL
}
