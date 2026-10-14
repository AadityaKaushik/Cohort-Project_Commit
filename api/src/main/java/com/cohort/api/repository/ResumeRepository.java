package com.cohort.api.repository;

import com.cohort.api.entity.Resume;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ResumeRepository extends UserScopedRepository<Resume> {
    @Modifying(clearAutomatically = true)
    @Query("update Resume r set r.isDefault = false where r.userId = :userId and r.isDefault = true")
    void clearDefaultForUser(@Param("userId") UUID userId);
}
