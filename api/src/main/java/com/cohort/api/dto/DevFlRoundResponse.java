package com.cohort.api.dto;

public record DevFlRoundResponse(
        int exportedUsers,
        int exportFailures,
        boolean roundRan,
        Integer roundNumber,
        Boolean accepted,
        String reason,
        Integer cohortSize) {
}
