package com.cohort.api.repository;

import com.cohort.api.entity.Question;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * Questions have mixed ownership (US-35/36): system rows are readable by
 * everyone, personal rows only by their owner. That visibility rule does
 * not fit {@link UserScopedRepository}'s "every finder takes userId and
 * matches only that user's rows" contract, so this extends plain {@link
 * JpaRepository} instead and {@code PreparationService} enforces
 * visibility explicitly (own-or-system for reads, own-only for writes) —
 * the same reasoning documented on {@code TopicRepository}. Filtering
 * (category/topic/difficulty/is_system/text search, US-35) is layered on
 * top via {@link JpaSpecificationExecutor}, the same technique {@code
 * ApplicationSpecifications} established (Specification pattern,
 * ARCHITECTURE_REVISION_JAVA.md §4.1), just not re-declared as a new named
 * pattern instance here.
 */
public interface QuestionRepository extends JpaRepository<Question, UUID>, JpaSpecificationExecutor<Question> {

    Optional<Question> findByIdAndUserId(UUID id, UUID userId);

    List<Question> findAllByTopicId(UUID topicId);
}
