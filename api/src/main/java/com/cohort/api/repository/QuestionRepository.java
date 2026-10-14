package com.cohort.api.repository;

import com.cohort.api.entity.Question;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface QuestionRepository extends JpaRepository<Question, UUID>, JpaSpecificationExecutor<Question> {
    Optional<Question> findByIdAndUserId(UUID id, UUID userId);

    List<Question> findAllByTopicId(UUID topicId);

    @Query("""
            select q from Question q
            where (q.isSystem = true or q.userId = :userId)
              and lower(q.questionText) like lower(concat('%', :pattern, '%')) escape '\\'
            order by q.questionText
            """)
    List<Question> search(@Param("userId") UUID userId, @Param("pattern") String pattern, Pageable pageable);
}
