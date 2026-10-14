package com.cohort.api.dto;

import com.cohort.api.entity.Offer;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateOfferRequest(
        @NotNull UUID applicationId,
        @NotNull @DecimalMin("0") BigDecimal baseSalary,
        @DecimalMin("0") BigDecimal bonus,
        @DecimalMin("0") BigDecimal equityValue,
        @DecimalMin("0") BigDecimal signingBonus,
        @Pattern(regexp = "[A-Z]{3}") String currency,
        @Size(max = 200) String location,
        Offer.WorkMode workMode,
        @Size(max = 2000) String benefits,
        @Size(max = 5000) String notes,
        LocalDate startDate,
        LocalDate responseDeadline,
        Offer.Status status) {
}
