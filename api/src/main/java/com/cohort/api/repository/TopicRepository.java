package com.cohort.api.repository;

import com.cohort.api.entity.Topic;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Topics are system-owned reference data (US-38), not per-user content, so
 * this deliberately extends plain {@link JpaRepository} rather than
 * {@link UserScopedRepository} — there is no {@code userId} to scope by,
 * and every user reads the same shared taxonomy.
 */
public interface TopicRepository extends JpaRepository<Topic, UUID> {

    Optional<Topic> findBySlug(String slug);

    List<Topic> findAllByOrderByCategoryAscNameAsc();

    /**
     * The prerequisite DAG's edges (US-38, {@code topic_prerequisites}),
     * exposed as a flat projection since the join table has no surrogate
     * key of its own and no entity is warranted for it.
     */
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
