package com.cohort.api.repository;

import com.cohort.api.entity.CompanyProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, UUID> {

    List<CompanyProfile> findByNameContainingIgnoreCaseOrderByNameAsc(String name);

    Optional<CompanyProfile> findBySlug(String slug);

    Optional<CompanyProfile> findFirstByNameIgnoreCase(String name);
}
