package com.cohort.api.repository;

import com.cohort.api.entity.AuthToken;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuthTokenRepository extends JpaRepository<AuthToken, UUID> {

    Optional<AuthToken> findByTokenHashAndPurpose(String tokenHash, AuthToken.Purpose purpose);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update AuthToken t set t.usedAt = :now "
            + "where t.user.id = :userId and t.purpose = :purpose and t.usedAt is null")
    int invalidateOutstanding(@Param("userId") UUID userId, @Param("purpose") AuthToken.Purpose purpose,
                              @Param("now") Instant now);
}
