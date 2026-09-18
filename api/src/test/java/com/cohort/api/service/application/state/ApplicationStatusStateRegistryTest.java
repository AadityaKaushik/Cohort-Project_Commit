package com.cohort.api.service.application.state;

import static org.assertj.core.api.Assertions.assertThat;

import com.cohort.api.entity.ApplicationStatus;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Exhaustively checks every (from, to) pair in both directions of
 * {@link ApplicationStatusStateRegistry#canTransition}, with and without a
 * correction note — 10 x 10 x 2 = 200 cases — against an expected-result
 * table transcribed directly from the state machine diagram in
 * PROJECT_PLAN.md §17.5, independently of the production code under test.
 * This is the check that the State pattern's transition table matches §17.5
 * exactly, which is the Sprint 1 Definition of Done requirement.
 */
class ApplicationStatusStateRegistryTest {

    private final ApplicationStatusStateRegistry registry = new ApplicationStatusStateRegistry();

    /** Forward/exit transitions — always legal, no note required. */
    private static final Map<ApplicationStatus, Set<ApplicationStatus>> EXPECTED_FORWARD = new EnumMap<>(Map.of(
            ApplicationStatus.SAVED, EnumSet.of(
                    ApplicationStatus.APPLIED, ApplicationStatus.OA, ApplicationStatus.INTERVIEW,
                    ApplicationStatus.FINAL_ROUND, ApplicationStatus.OFFER,
                    ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN),
            ApplicationStatus.APPLIED, EnumSet.of(
                    ApplicationStatus.OA, ApplicationStatus.INTERVIEW, ApplicationStatus.FINAL_ROUND,
                    ApplicationStatus.OFFER, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN),
            ApplicationStatus.OA, EnumSet.of(
                    ApplicationStatus.INTERVIEW, ApplicationStatus.FINAL_ROUND, ApplicationStatus.OFFER,
                    ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN),
            ApplicationStatus.INTERVIEW, EnumSet.of(
                    ApplicationStatus.FINAL_ROUND, ApplicationStatus.OFFER,
                    ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN),
            ApplicationStatus.FINAL_ROUND, EnumSet.of(
                    ApplicationStatus.OFFER, ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN),
            ApplicationStatus.OFFER, EnumSet.of(
                    ApplicationStatus.ACCEPTED, ApplicationStatus.DECLINED,
                    ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN),
            ApplicationStatus.REJECTED, EnumSet.noneOf(ApplicationStatus.class),
            ApplicationStatus.WITHDRAWN, EnumSet.noneOf(ApplicationStatus.class),
            ApplicationStatus.ACCEPTED, EnumSet.noneOf(ApplicationStatus.class),
            ApplicationStatus.DECLINED, EnumSet.noneOf(ApplicationStatus.class)));

    /** Backward transitions legal only when a correction note is supplied. */
    private static final Map<ApplicationStatus, Set<ApplicationStatus>> EXPECTED_CORRECTION = new EnumMap<>(Map.of(
            ApplicationStatus.SAVED, EnumSet.noneOf(ApplicationStatus.class),
            ApplicationStatus.APPLIED, EnumSet.of(ApplicationStatus.SAVED),
            ApplicationStatus.OA, EnumSet.of(ApplicationStatus.SAVED, ApplicationStatus.APPLIED),
            ApplicationStatus.INTERVIEW, EnumSet.of(
                    ApplicationStatus.SAVED, ApplicationStatus.APPLIED, ApplicationStatus.OA),
            ApplicationStatus.FINAL_ROUND, EnumSet.of(
                    ApplicationStatus.SAVED, ApplicationStatus.APPLIED, ApplicationStatus.OA,
                    ApplicationStatus.INTERVIEW),
            ApplicationStatus.OFFER, EnumSet.of(
                    ApplicationStatus.SAVED, ApplicationStatus.APPLIED, ApplicationStatus.OA,
                    ApplicationStatus.INTERVIEW, ApplicationStatus.FINAL_ROUND),
            ApplicationStatus.REJECTED, EnumSet.noneOf(ApplicationStatus.class),
            ApplicationStatus.WITHDRAWN, EnumSet.noneOf(ApplicationStatus.class),
            ApplicationStatus.ACCEPTED, EnumSet.noneOf(ApplicationStatus.class),
            ApplicationStatus.DECLINED, EnumSet.noneOf(ApplicationStatus.class)));

    static Stream<Arguments> everyTransitionPairWithAndWithoutNote() {
        Stream.Builder<Arguments> builder = Stream.builder();
        for (ApplicationStatus from : ApplicationStatus.values()) {
            for (ApplicationStatus to : ApplicationStatus.values()) {
                for (boolean hasNote : new boolean[] {false, true}) {
                    boolean expected = EXPECTED_FORWARD.get(from).contains(to)
                            || (hasNote && EXPECTED_CORRECTION.get(from).contains(to));
                    builder.add(Arguments.of(from, to, hasNote, expected));
                }
            }
        }
        return builder.build();
    }

    @ParameterizedTest(name = "{0} -> {1} (note={2}) should be {3}")
    @MethodSource("everyTransitionPairWithAndWithoutNote")
    void matchesTheStateMachineFromSection17_5(
            ApplicationStatus from, ApplicationStatus to, boolean hasNote, boolean expectedLegal) {
        assertThat(registry.canTransition(from, to, hasNote))
                .as("%s -> %s (note=%s)", from, to, hasNote)
                .isEqualTo(expectedLegal);
    }

    @Test
    void everyStatusHasExactlyOneRegisteredState() {
        for (ApplicationStatus status : ApplicationStatus.values()) {
            assertThat(registry.stateFor(status)).isNotNull();
            assertThat(registry.stateFor(status).status()).isEqualTo(status);
        }
    }

    @Test
    void terminalStatusesAreFullyAbsorbingEvenWithACorrectionNote() {
        for (ApplicationStatus terminal : Set.of(
                ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN,
                ApplicationStatus.ACCEPTED, ApplicationStatus.DECLINED)) {
            for (ApplicationStatus target : ApplicationStatus.values()) {
                assertThat(registry.canTransition(terminal, target, true))
                        .as("%s -> %s should never be legal, even with a note", terminal, target)
                        .isFalse();
            }
        }
    }

    @Test
    void acceptedAndDeclinedAreReachableOnlyFromOffer() {
        for (ApplicationStatus from : ApplicationStatus.values()) {
            boolean canReachAccepted = registry.canTransition(from, ApplicationStatus.ACCEPTED, false);
            boolean canReachDeclined = registry.canTransition(from, ApplicationStatus.DECLINED, false);
            if (from == ApplicationStatus.OFFER) {
                assertThat(canReachAccepted).isTrue();
                assertThat(canReachDeclined).isTrue();
            } else {
                assertThat(canReachAccepted).as("%s -> ACCEPTED", from).isFalse();
                assertThat(canReachDeclined).as("%s -> DECLINED", from).isFalse();
            }
        }
    }
}
