package com.cohort.api.service.recommendation.template;

import java.util.List;
import java.util.Locale;

/**
 * US-54 / PROJECT_PLAN.md §21.7: "The test {@code test_no_causal_language}
 * iterates every template and fails on any match against the prohibited
 * list." This is the checker that test calls; kept as its own standalone,
 * dependency-free utility (rather than inlined into the test) specifically
 * so a second test can prove the checker itself actually catches a bad
 * phrase, rather than the real "all templates pass" test being vacuously
 * true because the checker never flags anything.
 *
 * <p>The phrase list is §21.7's own list verbatim, plus the near-duplicate
 * phrasing named in BUILD_PHASES_JAVA.md's P41 verify column ("this
 * caused", "because you", "led to") -- kept as separate explicit entries
 * even where they overlap with a broader one ({@code "caused"} already
 * matches {@code "this caused"}) because the phrase list is meant to read
 * as a literal, auditable checklist against both source documents, not a
 * minimal regex.
 */
public final class CausalLanguageLinter {

    static final List<String> BANNED_PHRASES = List.of(
            "guarantees",
            "will get you",
            "ensures",
            "because of this you were rejected",
            "caused",
            "proven to",
            "always leads to",
            "you failed because",
            "this caused",
            "because you",
            "led to");

    private CausalLanguageLinter() {
    }

    /** Every banned phrase found in {@code text} (case-insensitive substring match), or an empty list if it reads clean. */
    public static List<String> findViolations(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalised = text.toLowerCase(Locale.ROOT);
        return BANNED_PHRASES.stream().filter(normalised::contains).toList();
    }
}
