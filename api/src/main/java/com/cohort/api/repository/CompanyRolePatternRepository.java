package com.cohort.api.repository;

import com.cohort.api.entity.CompanyRolePattern;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRolePatternRepository extends JpaRepository<CompanyRolePattern, UUID> {

    List<CompanyRolePattern> findByCompanyIdIn(Collection<UUID> companyIds);

    List<CompanyRolePattern> findByCompanyIdOrderByRoleFamilyAscLevelAsc(UUID companyId);
}
