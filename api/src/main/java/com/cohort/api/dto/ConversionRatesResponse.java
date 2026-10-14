package com.cohort.api.dto;

public record ConversionRatesResponse(
        ConversionRateView applicationToOa,
        ConversionRateView oaToInterview,
        ConversionRateView interviewToFinal,
        ConversionRateView finalToOffer,
        ConversionRateView overallSuccess) {
}
