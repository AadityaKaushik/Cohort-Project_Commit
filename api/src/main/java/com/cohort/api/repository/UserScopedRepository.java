package com.cohort.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.Repository;

/**
 * Repository pattern (ARCHITECTURE_REVISION_JAVA.md §4.1): shared base for
 * every repository whose entity is owned by a single user (applications,
 * resumes, interviews, ...). It deliberately extends the bare Spring Data
 * marker {@link Repository} rather than {@link org.springframework.data.jpa.repository.JpaRepository}
 * — so it exposes <em>no</em> unscoped finder such as {@code findById}. Every
 * query method declared here requires {@code userId}, which means it is
 * impossible to write a lookup against this base that forgets to scope by
 * owner: the isolation guarantee (FR-94, FR-95, PROJECT_PLAN.md §17.4) is
 * enforced by the method signature, not by convention or code review.
 *
 * <p>Concrete repositories extend this for their entity, e.g.
 * {@code interface ApplicationRepository extends UserScopedRepository<Application>}.
 * Spring Data derives these queries against whichever entity the concrete
 * repository is parameterised with, so that entity must expose a direct
 * {@code userId} property — denormalised onto the entity rather than
 * resolved through a nested association, the same choice already made for
 * {@code interviews.user_id} (PROJECT_PLAN.md §17.2) precisely so every
 * isolation query stays a single-table filter.
 *
 * @param <T> the owned entity type
 */
@NoRepositoryBean
public interface UserScopedRepository<T> extends Repository<T, UUID> {

    T save(T entity);

    Optional<T> findByIdAndUserId(UUID id, UUID userId);

    List<T> findAllByUserId(UUID userId);

    Page<T> findAllByUserId(UUID userId, Pageable pageable);

    boolean existsByIdAndUserId(UUID id, UUID userId);

    void deleteByIdAndUserId(UUID id, UUID userId);
}
