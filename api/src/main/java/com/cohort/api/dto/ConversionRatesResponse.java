package com.cohort.api.dto;

/** US-44: all five §20.7 conversion rates in one response, each independently sample-size-gated (FR-67). */
public record ConversionRatesResponse(
        ConversionRateView applicationToOa,
        ConversionRateView oaToInterview,
        ConversionRateView interviewToFinal,
        ConversionRateView finalToOffer,
        ConversionRateView overallSuccess) {
}
