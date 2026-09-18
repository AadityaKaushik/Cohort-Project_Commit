package com.cohort.api.repository;

import com.cohort.api.entity.Application;

/** Every finder is inherited from {@link UserScopedRepository} — no custom
 * unscoped query is added here. Filtering/search (FR-15, FR-16) arrives in
 * Sprint 2 (P16) via Specifications, layered on top without touching this
 * interface's isolation guarantee. */
public interface ApplicationRepository extends UserScopedRepository<Application> {
}
