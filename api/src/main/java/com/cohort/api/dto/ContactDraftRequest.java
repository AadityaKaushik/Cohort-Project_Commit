package com.cohort.api.dto;

import jakarta.validation.constraints.NotNull;

public record ContactDraftRequest(@NotNull Kind kind) {
    public enum Kind { referral_request, reconnect, referral_thanks }
}
