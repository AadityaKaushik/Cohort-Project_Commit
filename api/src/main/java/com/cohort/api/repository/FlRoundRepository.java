package com.cohort.api.repository;

import com.cohort.api.entity.FlRound;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Round metadata is not user-owned content (US-61) -- plain {@link JpaRepository}, same reasoning as {@code TopicRepository}. */
public interface FlRoundRepository extends JpaRepository<FlRound, UUID> {
}
