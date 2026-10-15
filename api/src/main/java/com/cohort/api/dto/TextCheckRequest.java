package com.cohort.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TextCheckRequest(@NotNull @Size(max = 10000) String text, Kind kind) {
    public enum Kind {
        EXPERIENCE,
        GENERAL
    }

    public boolean experience() {
        return kind != Kind.GENERAL;
    }
}
