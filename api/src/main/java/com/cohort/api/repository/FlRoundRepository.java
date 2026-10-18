package com.cohort.api.repository;

import com.cohort.api.entity.FlRound;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlRoundRepository extends JpaRepository<FlRound, UUID> {
    List<FlRound> findTop20ByOrderByCreatedAtDesc();
}
