package com.cohort.api.repository;

import com.cohort.api.entity.Application;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ApplicationRepository extends UserScopedRepository<Application>, JpaSpecificationExecutor<Application> {

    java.util.List<Application> findAllByUserIdAndCompanyId(java.util.UUID userId, java.util.UUID companyId);
}
