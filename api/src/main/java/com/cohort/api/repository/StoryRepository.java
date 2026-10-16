package com.cohort.api.repository;

import com.cohort.api.entity.Story;
import java.util.List;
import java.util.UUID;

public interface StoryRepository extends UserScopedRepository<Story> {

    List<Story> findAllByUserIdOrderByCreatedAtDesc(UUID userId);
}
