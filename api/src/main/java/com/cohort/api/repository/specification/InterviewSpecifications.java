package com.cohort.api.repository.specification;

import com.cohort.api.entity.Interview;
import com.cohort.api.entity.InterviewRoundType;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

public final class InterviewSpecifications {
    private InterviewSpecifications() {
    }

    public static Specification<Interview> ownedBy(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<Interview> forApplication(UUID applicationId) {
        return (root, query, cb) -> applicationId == null ? null : cb.equal(root.get("applicationId"), applicationId);
    }

    public static Specification<Interview> hasRoundType(InterviewRoundType roundType) {
        return (root, query, cb) -> roundType == null ? null : cb.equal(root.get("roundType"), roundType);
    }

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
