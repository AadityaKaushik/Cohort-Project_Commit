package com.cohort.api.repository;

import com.cohort.api.entity.ApplicationTag;
import com.cohort.api.entity.ApplicationTagId;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * A plain join-table repository, not a {@link UserScopedRepository} — an
 * {@code application_tags} row carries no {@code userId} of its own;
 * ownership is derived transitively through the {@code Tag} (or
 * {@code Application}) it references, which is exactly what
 * {@link com.cohort.api.repository.specification.ApplicationSpecifications#hasTag}
 * enforces when it filters.
 */
public interface ApplicationTagRepository extends JpaRepository<ApplicationTag, ApplicationTagId> {
}
