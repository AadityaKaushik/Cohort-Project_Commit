package com.cohort.api.repository;

import com.cohort.api.entity.ApplicationStatusHistory;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, UUID> {
    List<ApplicationStatusHistory> findByApplicationIdOrderByChangedAtDesc(UUID applicationId);

    List<ApplicationStatusHistory> findByApplicationIdIn(Collection<UUID> applicationIds);
}
