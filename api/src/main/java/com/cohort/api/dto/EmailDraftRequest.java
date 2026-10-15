package com.cohort.api.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record EmailDraftRequest(@NotNull Kind kind, UUID interviewId, UUID contactId) {
    public enum Kind { thank_you, follow_up, negotiation, referral_request, reconnect, referral_thanks }
}
