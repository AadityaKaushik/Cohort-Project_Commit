package com.cohort.api.repository;

import com.cohort.api.entity.ResumeSection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Deliberately NOT a {@link UserScopedRepository} — this table has no
 * {@code user_id} column of its own (PROJECT_PLAN.md §17.2); isolation is
 * enforced one level up, by always resolving the owning {@code Resume} via
 * {@code ResumeRepository.findByIdAndUserId} first, in {@code ResumeService},
 * before ever touching section rows (same convention as
 * {@code ApplicationStatusHistoryRepository}).
 */
public interface ResumeSectionRepository extends JpaRepository<ResumeSection, UUID> {

    List<ResumeSection> findByResumeIdOrderBySortOrder(UUID resumeId);
}
