package com.cohort.api.repository;

import com.cohort.api.entity.Tag;

/** User-scoped like every owned entity; see {@link Tag} for why this exists ahead of full tag CRUD (US-19). */
public interface TagRepository extends UserScopedRepository<Tag> {
}
