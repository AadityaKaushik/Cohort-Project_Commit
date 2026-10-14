package com.cohort.api.repository;

import com.cohort.api.entity.PreparationTask;
import java.util.List;
import java.util.UUID;

public interface PreparationTaskRepository extends UserScopedRepository<PreparationTask> {
    List<PreparationTask> findAllByUserIdOrderByDueDateAscCreatedAtDesc(UUID userId);
}
