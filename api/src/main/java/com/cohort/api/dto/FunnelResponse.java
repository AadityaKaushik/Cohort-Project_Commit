package com.cohort.api.dto;

/** US-43: application funnel counts, all "ever reached" per §20.7/DR-01. Every stage links to its filtered application list client-side via {@code current_stage}/{@code status} query params, not via anything this response carries. */
public record FunnelResponse(long applied, long reachedOa, long reachedInterview, long reachedFinal, long reachedOffer, int sampleSize) {
}
