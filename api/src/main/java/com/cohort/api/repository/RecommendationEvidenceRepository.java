package com.cohort.api.repository;

import com.cohort.api.entity.RecommendationEvidence;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RecommendationEvidenceRepository extends JpaRepository<RecommendationEvidence, UUID> {
    List<RecommendationEvidence> findAllByRecommendationId(UUID recommendationId);

    List<RecommendationEvidence> findAllByRecommendationIdIn(List<UUID> recommendationIds);
}
