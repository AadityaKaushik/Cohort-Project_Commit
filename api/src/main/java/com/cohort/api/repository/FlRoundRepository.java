package com.cohort.api.repository;

import com.cohort.api.entity.FlRound;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FlRoundRepository extends JpaRepository<FlRound, UUID> {
}
