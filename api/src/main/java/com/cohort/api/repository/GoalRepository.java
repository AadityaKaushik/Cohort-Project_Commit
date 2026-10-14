package com.cohort.api.repository;

import com.cohort.api.entity.Goal;
import java.util.Optional;
import java.util.UUID;

public interface GoalRepository extends UserScopedRepository<Goal> {

    Optional<Goal> findByUserIdAndMetric(UUID userId, Goal.Metric metric);
}
