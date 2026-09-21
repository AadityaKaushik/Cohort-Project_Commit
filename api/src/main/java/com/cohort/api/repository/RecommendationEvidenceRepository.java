package com.cohort.api.repository;

import com.cohort.api.entity.RecommendationEvidence;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Evidence rows are reached only through their owning {@link com.cohort.api.entity.Recommendation}
 * (always loaded first via {@link RecommendationRepository}, which does
 * enforce ownership), so this deliberately extends plain
 * {@link JpaRepository} rather than {@link UserScopedRepository} -- same
 * rationale as {@link TopicRepository} not extending it: there is no direct
 * {@code user_id} column to scope by here.
 */
public interface RecommendationEvidenceRepository extends JpaRepository<RecommendationEvidence, UUID> {

    List<RecommendationEvidence> findAllByRecommendationId(UUID recommendationId);

    List<RecommendationEvidence> findAllByRecommendationIdIn(List<UUID> recommendationIds);
}
