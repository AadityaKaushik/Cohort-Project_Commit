package com.cohort.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record EmailDraftRequest(@NotNull Kind kind, UUID interviewId) {
    public enum Kind { thank_you, follow_up, negotiation }
}
