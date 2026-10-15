package com.cohort.api.repository;

import com.cohort.api.entity.OfferFactor;
import com.cohort.api.entity.OfferFactorWeight;
import java.util.Optional;
import java.util.UUID;

public interface OfferFactorWeightRepository extends UserScopedRepository<OfferFactorWeight> {

    Optional<OfferFactorWeight> findByUserIdAndFactor(UUID userId, OfferFactor factor);
}
