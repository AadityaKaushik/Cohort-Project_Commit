package com.cohort.api.dto;

public record FunnelResponse(long applied, long reachedOa, long reachedInterview, long reachedFinal, long reachedOffer, int sampleSize) {
}
