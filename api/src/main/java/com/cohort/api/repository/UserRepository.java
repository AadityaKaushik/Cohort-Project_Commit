package com.cohort.api.repository;

import com.cohort.api.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    // email is CITEXT at the DB level, so equality here is already
    // case-insensitive — no need for a Java-side IgnoreCase wrapper.
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    /** US-57/US-58: the export job's cohort -- re-read every run so an opt-out takes effect from the very next cycle (US-64). */
    List<User> findAllByFlOptInTrue();
}
