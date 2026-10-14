package com.cohort.api.service.recommendation.template;

import java.util.List;
import java.util.Locale;

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

    public static List<String> findViolations(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalised = text.toLowerCase(Locale.ROOT);
        return BANNED_PHRASES.stream().filter(normalised::contains).toList();
    }
}
