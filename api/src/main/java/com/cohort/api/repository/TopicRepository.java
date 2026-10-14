package com.cohort.api.repository;

import com.cohort.api.entity.Topic;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TopicRepository extends JpaRepository<Topic, UUID> {
    Optional<Topic> findBySlug(String slug);

    List<Topic> findAllByOrderByCategoryAscNameAsc();

    @Query(
            value = "SELECT topic_id AS topicId, prerequisite_topic_id AS prerequisiteTopicId FROM topic_prerequisites",
            nativeQuery = true)
    List<TopicPrerequisiteEdge> findAllPrerequisiteEdges();

    @Query(
            value = "SELECT prerequisite_topic_id FROM topic_prerequisites WHERE topic_id = :topicId",
            nativeQuery = true)
    List<UUID> findPrerequisiteIds(@Param("topicId") UUID topicId);

    interface TopicPrerequisiteEdge {
        UUID getTopicId();

        UUID getPrerequisiteTopicId();
    }
}
