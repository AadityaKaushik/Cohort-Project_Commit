package com.cohort.api.repository;

import com.cohort.api.entity.Application;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Every finder is inherited from {@link UserScopedRepository} — no custom
 * unscoped query is added here. Filtering/search (FR-15, FR-16) is added via
 * {@link JpaSpecificationExecutor}, whose {@code findAll(Specification, Pageable)}
 * is what {@link com.cohort.api.repository.specification.ApplicationSpecifications}
 * (Specification pattern) is built against — layered on top without touching
 * this interface's isolation guarantee: every Specification this codebase
 * builds still starts from {@code ApplicationSpecifications.ownedBy(userId)}. */
public interface ApplicationRepository extends UserScopedRepository<Application>, JpaSpecificationExecutor<Application> {
}
