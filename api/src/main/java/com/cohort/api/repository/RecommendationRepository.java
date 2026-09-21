package com.cohort.api.repository;

import com.cohort.api.entity.Recommendation;
import com.cohort.api.service.recommendation.RecommendationStatus;
import java.util.List;
import java.util.UUID;

/**
 * Recommendations are strictly user-owned (US-51, FR-95), so this extends
 * {@link UserScopedRepository} like every other owned entity.
 * {@code findAllByUserIdAndStatusIn} backs {@code RecommendationFacade}'s
 * (P42) suppression check -- reading currently snoozed/dismissed rows
 * before regenerating the {@code active} set, so a snoozed or
 * type-dismissed candidate is never silently regenerated.
 */
public interface RecommendationRepository extends UserScopedRepository<Recommendation> {

    List<Recommendation> findAllByUserIdAndStatus(UUID userId, RecommendationStatus status);

    List<Recommendation> findAllByUserIdAndStatusIn(UUID userId, List<RecommendationStatus> statuses);

    void deleteAllByUserIdAndStatus(UUID userId, RecommendationStatus status);
}
