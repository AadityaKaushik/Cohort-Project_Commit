package com.cohort.api.dto;

/** US-57/US-64: the current user's federated-learning participation state. */
public record FlConsentResponse(boolean optedIn) {
}
