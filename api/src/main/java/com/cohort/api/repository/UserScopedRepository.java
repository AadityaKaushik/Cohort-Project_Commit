package com.cohort.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

@NoRepositoryBean
public interface UserScopedRepository<T> extends Repository<T, UUID> {
    T save(T entity);

    Optional<T> findByIdAndUserId(UUID id, UUID userId);

    List<T> findAllByUserId(UUID userId);

    Page<T> findAllByUserId(UUID userId, Pageable pageable);

    boolean existsByIdAndUserId(UUID id, UUID userId);

    void deleteByIdAndUserId(UUID id, UUID userId);
}
