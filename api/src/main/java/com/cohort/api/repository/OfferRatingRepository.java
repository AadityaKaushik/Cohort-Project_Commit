package com.cohort.api.repository;

import com.cohort.api.entity.OfferFactor;
import com.cohort.api.entity.OfferRating;
import java.util.Optional;
import java.util.UUID;

public interface OfferRatingRepository extends UserScopedRepository<OfferRating> {

    Optional<OfferRating> findByUserIdAndOfferIdAndFactor(UUID userId, UUID offerId, OfferFactor factor);
}
