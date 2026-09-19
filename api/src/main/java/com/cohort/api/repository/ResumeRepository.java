package com.cohort.api.repository;

import com.cohort.api.entity.Resume;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Every finder is inherited from {@link UserScopedRepository} except
 * {@link #clearDefaultForUser}, which backs the "exactly one default resume
 * per user" rule (FR-33) enforced in the DB by the partial unique index
 * {@code uq_resumes_user_default} (V7 migration). It is called as a direct,
 * immediately-executed bulk update <em>before</em> the new default row is
 * inserted/updated in {@code ResumeService} — never after — so the old and
 * new default rows are never both {@code true} at once and the unique index
 * is never at risk of a transient violation.
 */
public interface ResumeRepository extends UserScopedRepository<Resume> {

    @Modifying(clearAutomatically = true)
    @Query("update Resume r set r.isDefault = false where r.userId = :userId and r.isDefault = true")
    void clearDefaultForUser(@Param("userId") UUID userId);
}
