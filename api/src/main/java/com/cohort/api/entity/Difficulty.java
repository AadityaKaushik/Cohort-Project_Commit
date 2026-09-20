package com.cohort.api.entity;

/**
 * Shared easy/medium/hard domain used by {@link Topic#getDefaultDifficulty()}
 * and {@link Question#getDifficulty()} (PROJECT_PLAN.md §17.2). Kept as a
 * standalone top-level enum, rather than nested inside one entity and
 * reused from the other, purely so both entities can reference the same
 * type without an awkward cross-entity import.
 */
public enum Difficulty {
    easy,
    medium,
    hard
}
