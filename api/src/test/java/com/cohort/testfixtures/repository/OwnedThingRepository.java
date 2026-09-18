package com.cohort.testfixtures.repository;

import com.cohort.api.repository.UserScopedRepository;

/** Test-only concrete repository proving {@link UserScopedRepository} works end to end. */
public interface OwnedThingRepository extends UserScopedRepository<OwnedThing> {
}
