package com.cohort.api.repository;

import com.cohort.api.entity.ApplicationTag;
import com.cohort.api.entity.ApplicationTagId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationTagRepository extends JpaRepository<ApplicationTag, ApplicationTagId> {
}
