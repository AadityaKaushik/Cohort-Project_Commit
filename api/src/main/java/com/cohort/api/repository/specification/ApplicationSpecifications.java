package com.cohort.api.repository.specification;

import com.cohort.api.dto.ApplicationSearchCriteria;
import com.cohort.api.entity.Application;
import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.entity.ApplicationTag;
import com.cohort.api.entity.Company;
import com.cohort.api.entity.Tag;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.data.jpa.domain.Specification;

/**
 * Specification pattern (ARCHITECTURE_REVISION_JAVA.md §4.1): each method
 * returns one small, independently testable {@link Specification}, and
 * {@link #combine} composes them with {@code .and()} rather than a
 * hand-rolled dynamic-query builder with a growing parameter list — adding a
 * new filterable field (FR-16) means adding one new method here, never
 * editing an existing one or the repository interface.
 *
 * <p>Every individual specification tolerates a {@code null}/blank filter
 * value by returning a {@code null} predicate from its lambda, which Spring
 * Data JPA's {@code Specification} composition treats as "no restriction" —
 * so {@link #combine} can unconditionally chain every filter without a
 * branch per field.
 */
public final class ApplicationSpecifications {

    private ApplicationSpecifications() {
    }

    /** Every composed specification starts here — the one non-optional filter, mirroring {@code UserScopedRepository}'s guarantee. */
    public static Specification<Application> ownedBy(UUID userId) {
        return (root, query, cb) -> cb.equal(root.get("userId"), userId);
    }

    public static Specification<Application> hasStatus(ApplicationStatus status) {
        return (root, query, cb) -> status == null ? null : cb.equal(root.get("status"), status);
    }

    public static Specification<Application> hasStage(ApplicationStatus stage) {
        return (root, query, cb) -> stage == null ? null : cb.equal(root.get("currentStage"), stage);
    }

    public static Specification<Application> hasPriority(Application.Priority priority) {
        return (root, query, cb) -> priority == null ? null : cb.equal(root.get("priority"), priority);
    }

    public static Specification<Application> hasWorkMode(Application.WorkMode workMode) {
        return (root, query, cb) -> workMode == null ? null : cb.equal(root.get("workMode"), workMode);
    }

    public static Specification<Application> dateAppliedBetween(LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            if (from == null && to == null) {
                return null;
            }
            if (from != null && to != null) {
                return cb.between(root.get("dateApplied"), from, to);
            }
            return from != null
                    ? cb.greaterThanOrEqualTo(root.get("dateApplied"), from)
                    : cb.lessThanOrEqualTo(root.get("dateApplied"), to);
        };
    }

    /**
     * FR-15: case-insensitive substring match across job title and the
     * owning company's name. {@code Application} deliberately stores
     * {@code companyId} as a plain column rather than a JPA association
     * (UserScopedRepository.java's javadoc), so the company-name half of
     * this match is a correlated subquery rather than a join.
     */
    public static Specification<Application> keywordMatches(UUID userId, String keyword) {
        return (root, query, cb) -> {
            if (keyword == null || keyword.isBlank()) {
                return null;
            }
            String pattern = "%" + keyword.trim().toLowerCase() + "%";

            Subquery<UUID> matchingCompanyIds = query.subquery(UUID.class);
            Root<Company> companyRoot = matchingCompanyIds.from(Company.class);
            matchingCompanyIds
                    .select(companyRoot.get("id"))
                    .where(cb.equal(companyRoot.get("userId"), userId), cb.like(cb.lower(companyRoot.get("name")), pattern));

            return cb.or(
                    cb.like(cb.lower(root.get("jobTitle")), pattern),
                    root.get("companyId").in(matchingCompanyIds));
        };
    }

    /** FR-16: filter to applications carrying a tag with this exact (case-insensitive) name, owned by the same user. */
    public static Specification<Application> hasTag(UUID userId, String tagName) {
        return (root, query, cb) -> {
            if (tagName == null || tagName.isBlank()) {
                return null;
            }

            Subquery<UUID> matchingTagIds = query.subquery(UUID.class);
            Root<Tag> tagRoot = matchingTagIds.from(Tag.class);
            matchingTagIds
                    .select(tagRoot.get("id"))
                    .where(cb.equal(tagRoot.get("userId"), userId), cb.equal(cb.lower(tagRoot.get("name")), tagName.trim().toLowerCase()));

            Subquery<UUID> taggedApplicationIds = query.subquery(UUID.class);
            Root<ApplicationTag> applicationTagRoot = taggedApplicationIds.from(ApplicationTag.class);
            taggedApplicationIds
                    .select(applicationTagRoot.get("applicationId"))
                    .where(applicationTagRoot.get("tagId").in(matchingTagIds));

            return root.get("id").in(taggedApplicationIds);
        };
    }

    /** Combines every filter in {@code criteria} onto the owning user's applications with AND semantics (US-17). */
    public static Specification<Application> combine(UUID userId, ApplicationSearchCriteria criteria) {
        return ownedBy(userId)
                .and(hasStatus(criteria.status()))
                .and(hasStage(criteria.stage()))
                .and(hasPriority(criteria.priority()))
                .and(hasWorkMode(criteria.workMode()))
                .and(dateAppliedBetween(criteria.dateFrom(), criteria.dateTo()))
                .and(hasTag(userId, criteria.tag()))
                .and(keywordMatches(userId, criteria.q()));
    }
}
