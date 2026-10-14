package com.cohort.api.repository;

import com.cohort.api.entity.ResumeSection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeSectionRepository extends JpaRepository<ResumeSection, UUID> {
    List<ResumeSection> findByResumeIdOrderBySortOrder(UUID resumeId);
}
