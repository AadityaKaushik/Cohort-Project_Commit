package com.cohort.testfixtures.repository;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;

/**
 * Minimal throwaway entity used only by {@code UserScopedRepositoryIntegrationTest}
 * to exercise {@code UserScopedRepository} against a real Postgres instance
 * without waiting for the real Application entity (P11). It deliberately
 * lives outside the {@code com.cohort.api} package tree so Spring Boot's
 * default entity/component scan (rooted at {@code ApiApplication}'s
 * package) never picks it up in any other test — it is wired in explicitly,
 * only by the one test that needs it.
 */
@Entity
@Table(name = "owned_things_test")
public class OwnedThing {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid")
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column
    private String label;

    protected OwnedThing() {
        // JPA
    }

    public OwnedThing(UUID userId, String label) {
        this.userId = userId;
        this.label = label;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getLabel() {
        return label;
    }
}
