package com.cohort.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** US-96: everything the user holds about one company, aggregated in one response. */
public record CompanyDetailResponse(
        UUID id,
        String name,
        String website,
        String industry,
        Stats stats,
        List<ApplicationSummary> applications,
        List<ContactResponse> contacts,
        List<CompanyNoteResponse> notes) {

    public record Stats(int applications, int active, int interviews, int offers, Instant lastActivityAt) {
    }
}
