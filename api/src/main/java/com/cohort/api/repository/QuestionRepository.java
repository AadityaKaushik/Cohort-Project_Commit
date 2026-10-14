package com.cohort.api.repository;

import com.cohort.api.entity.Question;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface QuestionRepository extends JpaRepository<Question, UUID>, JpaSpecificationExecutor<Question> {
    Optional<Question> findByIdAndUserId(UUID id, UUID userId);

    List<Question> findAllByTopicId(UUID topicId);
}
