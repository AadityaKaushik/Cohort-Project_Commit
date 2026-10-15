package com.cohort.api.dto;

import com.cohort.api.entity.OfferFactor;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.EnumMap;
import java.util.Map;

/** A 1 to 5 value per factor, used for both weights and ratings. Only non-null fields are applied. */
public record OfferFactorValues(
        @Min(1) @Max(5) Integer pay,
        @Min(1) @Max(5) Integer growth,
        @Min(1) @Max(5) Integer learning,
        @Min(1) @Max(5) Integer workLifeBalance,
        @Min(1) @Max(5) Integer location,
        @Min(1) @Max(5) Integer brand) {

    public Map<OfferFactor, Integer> toMap() {
        Map<OfferFactor, Integer> values = new EnumMap<>(OfferFactor.class);
        put(values, OfferFactor.pay, pay);
        put(values, OfferFactor.growth, growth);
        put(values, OfferFactor.learning, learning);
        put(values, OfferFactor.work_life_balance, workLifeBalance);
        put(values, OfferFactor.location, location);
        put(values, OfferFactor.brand, brand);
        return values;
    }

    private static void put(Map<OfferFactor, Integer> values, OfferFactor factor, Integer value) {
        if (value != null) {
            values.put(factor, value);
        }
    }
}
