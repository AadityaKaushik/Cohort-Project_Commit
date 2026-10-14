package com.cohort.api.entity;

public enum ApplicationStatus {
    SAVED,
    APPLIED,
    OA,
    INTERVIEW,
    FINAL_ROUND,
    OFFER,
    REJECTED,
    WITHDRAWN,
    ACCEPTED,
    DECLINED;

    private static final java.util.Set<ApplicationStatus> TERMINAL = java.util.Set.of(
            REJECTED, WITHDRAWN, ACCEPTED, DECLINED);

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }
}
