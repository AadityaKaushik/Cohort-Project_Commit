package com.cohort.api.mapper;

import com.cohort.api.dto.OfferResponse;
import com.cohort.api.entity.Offer;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class OfferMapper {

    public OfferResponse toResponse(Offer offer, String companyName, String jobTitle, LocalDate today) {
        return new OfferResponse(
                offer.getId(),
                offer.getApplicationId(),
                companyName,
                jobTitle,
                offer.getBaseSalary(),
                offer.getBonus(),
                offer.getEquityValue(),
                offer.getSigningBonus(),
                firstYearTotal(offer),
                offer.getCurrency(),
                offer.getLocation(),
                offer.getWorkMode(),
                offer.getBenefits(),
                offer.getNotes(),
                offer.getStartDate(),
                offer.getResponseDeadline(),
                daysRemaining(offer, today),
                offer.getStatus(),
                offer.getCreatedAt(),
                offer.getUpdatedAt());
    }

    private static BigDecimal firstYearTotal(Offer offer) {
        return offer.getBaseSalary()
                .add(orZero(offer.getBonus()))
                .add(orZero(offer.getEquityValue()))
                .add(orZero(offer.getSigningBonus()));
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static Long daysRemaining(Offer offer, LocalDate today) {
        boolean open = offer.getStatus() == Offer.Status.pending || offer.getStatus() == Offer.Status.negotiating;
        if (!open || offer.getResponseDeadline() == null) {
            return null;
        }
        return ChronoUnit.DAYS.between(today, offer.getResponseDeadline());
    }
}
