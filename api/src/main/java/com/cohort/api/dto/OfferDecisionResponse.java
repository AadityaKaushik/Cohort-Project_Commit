package com.cohort.api.dto;

import com.cohort.api.entity.OfferFactor;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record OfferDecisionResponse(Map<OfferFactor, Integer> weights, List<Entry> offers, String leaderSummary) {

    public record Entry(
            UUID offerId,
            String companyName,
            String jobTitle,
            Map<OfferFactor, Integer> ratings,
            Integer score,
            Integer rank) {
    }
}
