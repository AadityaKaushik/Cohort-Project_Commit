package com.cohort.api.repository.specification;

import com.cohort.api.entity.Interview;
import com.cohort.api.entity.InterviewRoundType;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specification pattern (ARCHITECTURE_REVISION_JAVA.md §4.1), reusing the
 * same composable-filter approach {@code ApplicationSpecifications}
 * established in P16: each method tolerates a {@code null} filter value by
 * returning a {@code null} predicate, so {@link #combine} can chain every
 * filter unconditionally.
 */
public final class InterviewSpecifications {

    private InterviewSpecifications() {
    }

    /** Every composed specification starts here, mirroring {@code UserScopedRepository}'s guarantee. */
    public static Specification<Interview> ownedBy(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<Interview> forApplication(UUID applicationId) {
        return (root, query, cb) -> applicationId == null ? null : cb.equal(root.get("applicationId"), applicationId);
    }

    public static Specification<Interview> hasRoundType(InterviewRoundType roundType) {
        return (root, query, cb) -> roundType == null ? null : cb.equal(root.get("roundType"), roundType);
    }

    /**
     * US-28: "split into upcoming and completed." Upcoming means a
     * scheduled time in the future; {@code upcoming = false} is everything
     * else (unscheduled or already in the past), not "outcome recorded."
     */
    public static Specification<Interview> isUpcoming(Boolean upcoming) {
        return (root, query, cb) -> {
            if (upcoming == null) {
                return null;
            }
            Instant now = Instant.now();
            var inTheFuture = cb.and(cb.isNotNull(root.get("scheduledAt")), cb.greaterThan(root.get("scheduledAt"), now));
            return upcoming ? inTheFuture : cb.not(inTheFuture);
        };
    }

    public static Specification<Interview> combine(UUID userId, UUID applicationId, Boolean upcoming, InterviewRoundType roundType) {
        return ownedBy(userId)
                .and(forApplication(applicationId))
                .and(isUpcoming(upcoming))
                .and(hasRoundType(roundType));
    }
}
