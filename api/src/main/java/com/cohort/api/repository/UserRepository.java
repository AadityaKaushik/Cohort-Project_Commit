package com.cohort.api.repository;

import com.cohort.api.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    List<User> findAllByFlOptInTrue();

    List<User> findAllByIsActiveTrue();
}
