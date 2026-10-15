package com.cohort.api.repository;

import com.cohort.api.entity.ExperienceContribution;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExperienceContributionRepository extends JpaRepository<ExperienceContribution, UUID> {

    List<ExperienceContribution> findByCompanyId(UUID companyId);

    List<ExperienceContribution> findByContributorHashIn(Collection<String> hashes);

    boolean existsByCompanyIdAndRoleFamilyAndContributorHashAndYear(
            UUID companyId, String roleFamily, String contributorHash, int year);
}
