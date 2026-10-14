package com.cohort.api.repository;

import com.cohort.api.entity.Recommendation;
import com.cohort.api.service.recommendation.RecommendationStatus;
import java.util.List;
import java.util.UUID;

public interface RecommendationRepository extends UserScopedRepository<Recommendation> {
    List<Recommendation> findAllByUserIdAndStatus(UUID userId, RecommendationStatus status);

    List<Recommendation> findAllByUserIdAndStatusIn(UUID userId, List<RecommendationStatus> statuses);

    void deleteAllByUserIdAndStatus(UUID userId, RecommendationStatus status);
}
