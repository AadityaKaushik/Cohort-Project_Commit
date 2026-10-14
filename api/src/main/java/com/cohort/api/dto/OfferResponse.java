package com.cohort.api.dto;

import com.cohort.api.entity.Offer;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record OfferResponse(
        UUID id,
        UUID applicationId,
        String companyName,
        String jobTitle,
        BigDecimal baseSalary,
        BigDecimal bonus,
        BigDecimal equityValue,
        BigDecimal signingBonus,
        BigDecimal firstYearTotal,
        String currency,
        String location,
        Offer.WorkMode workMode,
        String benefits,
        String notes,
        LocalDate startDate,
        LocalDate responseDeadline,
        Long daysRemaining,
        Offer.Status status,
        Instant createdAt,
        Instant updatedAt) {
}
