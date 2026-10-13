package com.cohort.api.repository;

import com.cohort.api.entity.CompanyNote;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CompanyNoteRepository extends UserScopedRepository<CompanyNote> {

    List<CompanyNote> findAllByUserIdAndCompanyIdOrderByCreatedAtDesc(UUID userId, UUID companyId);

    Optional<CompanyNote> findByIdAndUserIdAndCompanyId(UUID id, UUID userId, UUID companyId);
}
