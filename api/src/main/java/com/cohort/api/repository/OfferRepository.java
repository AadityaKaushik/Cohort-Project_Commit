package com.cohort.api.repository;

import com.cohort.api.entity.Offer;
import java.util.List;
import java.util.UUID;

public interface OfferRepository extends UserScopedRepository<Offer> {

    List<Offer> findAllByUserIdOrderByCreatedAtDesc(UUID userId);

    boolean existsByApplicationIdAndUserId(UUID applicationId, UUID userId);
}
