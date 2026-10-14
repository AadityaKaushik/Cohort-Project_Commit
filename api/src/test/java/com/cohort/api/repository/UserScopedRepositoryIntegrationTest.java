package com.cohort.api.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.cohort.api.AbstractIntegrationTest;
import com.cohort.testfixtures.repository.OwnedThing;
import com.cohort.testfixtures.repository.OwnedThingRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(AbstractIntegrationTest.ContainerConfig.class)
@EntityScan(basePackageClasses = OwnedThing.class)
@EnableJpaRepositories(basePackageClasses = OwnedThingRepository.class)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class UserScopedRepositoryIntegrationTest extends AbstractIntegrationTest {
    @Autowired
    private OwnedThingRepository repository;

    @Test
    void findByIdAndUserIdReturnsTheRowOnlyForItsOwner() {
        UUID ownerA = UUID.randomUUID();
        UUID ownerB = UUID.randomUUID();
        OwnedThing thing = repository.save(new OwnedThing(ownerA, "alpha"));

        assertThat(repository.findByIdAndUserId(thing.getId(), ownerA)).isPresent();
        assertThat(repository.findByIdAndUserId(thing.getId(), ownerB)).isEmpty();
    }

    @Test
    void findAllByUserIdNeverReturnsAnotherUsersRows() {
        UUID ownerA = UUID.randomUUID();
        UUID ownerB = UUID.randomUUID();
        repository.save(new OwnedThing(ownerA, "a1"));
        repository.save(new OwnedThing(ownerA, "a2"));
        repository.save(new OwnedThing(ownerB, "b1"));

        assertThat(repository.findAllByUserId(ownerA))
                .hasSize(2)
                .allMatch(thing -> thing.getUserId().equals(ownerA));
        assertThat(repository.findAllByUserId(ownerB)).hasSize(1);
    }

    @Test
    void findAllByUserIdIsPageableAndStillScoped() {
        UUID owner = UUID.randomUUID();
        UUID otherOwner = UUID.randomUUID();
        for (int i = 0; i < 3; i++) {
            repository.save(new OwnedThing(owner, "item-" + i));
        }
        repository.save(new OwnedThing(otherOwner, "not-mine"));

        Page<OwnedThing> page = repository.findAllByUserId(owner, PageRequest.of(0, 2));

        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(2).allMatch(thing -> thing.getUserId().equals(owner));
    }

    @Test
    void existsByIdAndUserIdIsScopedToTheOwner() {
        UUID ownerA = UUID.randomUUID();
        UUID ownerB = UUID.randomUUID();
        OwnedThing thing = repository.save(new OwnedThing(ownerA, "alpha"));

        assertThat(repository.existsByIdAndUserId(thing.getId(), ownerA)).isTrue();
        assertThat(repository.existsByIdAndUserId(thing.getId(), ownerB)).isFalse();
    }

    @Test
    void deleteByIdAndUserIdNeverDeletesAnotherUsersRow() {
        UUID ownerA = UUID.randomUUID();
        UUID ownerB = UUID.randomUUID();
        OwnedThing thing = repository.save(new OwnedThing(ownerA, "alpha"));

        repository.deleteByIdAndUserId(thing.getId(), ownerB);
        assertThat(repository.findByIdAndUserId(thing.getId(), ownerA)).isPresent();

        repository.deleteByIdAndUserId(thing.getId(), ownerA);
        assertThat(repository.findByIdAndUserId(thing.getId(), ownerA)).isEmpty();
    }
}
