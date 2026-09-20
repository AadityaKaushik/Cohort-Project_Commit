package com.cohort.api.repository;

import com.cohort.api.entity.PreparationTask;
import java.util.List;
import java.util.UUID;

/** Strictly user-owned (US-40, FR-95), like every other per-user table in this codebase. */
public interface PreparationTaskRepository extends UserScopedRepository<PreparationTask> {

    List<PreparationTask> findAllByUserIdOrderByDueDateAscCreatedAtDesc(UUID userId);
}
