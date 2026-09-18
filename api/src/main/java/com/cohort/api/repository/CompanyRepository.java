package com.cohort.api.repository;

import com.cohort.api.entity.Company;
import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends UserScopedRepository<Company> {

    Optional<Company> findByUserIdAndNormalizedName(UUID userId, String normalizedName);
}
