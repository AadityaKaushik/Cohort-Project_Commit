package com.cohort.api.repository;

import com.cohort.api.entity.User;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

    // email is CITEXT at the DB level, so equality here is already
    // case-insensitive — no need for a Java-side IgnoreCase wrapper.
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
}
