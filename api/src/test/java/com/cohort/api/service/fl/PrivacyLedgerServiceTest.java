package com.cohort.api.service.fl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cohort.api.entity.FlRound;
import com.cohort.api.entity.PrivacyLedgerEntry;
import com.cohort.api.entity.User;
import com.cohort.api.repository.FlRoundRepository;
import com.cohort.api.repository.PrivacyLedgerEntryRepository;
import com.cohort.api.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PrivacyLedgerServiceTest {
    @Mock
    private FederatedLearningGateway gateway;
    @Mock
    private FlRoundRepository roundRepository;
    @Mock
    private PrivacyLedgerEntryRepository ledgerRepository;
    @Mock
    private UserRepository userRepository;

    @Test
    void persistsTheRoundAndOneLedgerEntryPerRealTransmittedVector() {
        PrivacyLedgerService service = new PrivacyLedgerService(gateway, roundRepository, ledgerRepository, userRepository);
        UUID userId = UUID.randomUUID();
        List<Double> realTransmittedVector = List.of(0.11, -0.22, 0.33);
        FlRoundOutcome outcome = new FlRoundOutcome(
                1, true, null, 3,
                List.of(new FlLedgerEntryData(userId.toString(), 1, 1.4, 1.0, 1.0, 1.0, realTransmittedVector)));
        when(userRepository.findAllByFlOptInTrue()).thenReturn(List.of(user(userId)));
        when(roundRepository.count()).thenReturn(0L);
        when(gateway.runRound(1, List.of(userId.toString()))).thenReturn(outcome);
        FlRound savedRound = new FlRound(1, true, null, 3);
        setRoundId(savedRound, UUID.randomUUID());
        when(roundRepository.save(any())).thenReturn(savedRound);

        Optional<FlRound> result = service.runNextRoundAndPersist();

        assertThat(result).isPresent();
        var entryCaptor = org.mockito.ArgumentCaptor.forClass(PrivacyLedgerEntry.class);
        verify(ledgerRepository).save(entryCaptor.capture());
        PrivacyLedgerEntry persisted = entryCaptor.getValue();
        assertThat(persisted.getUserId()).isEqualTo(userId);
        // The whole point of P55: this must be the REAL vector fl-service reported, verbatim.
        assertThat(persisted.getTransmittedVector()).isEqualTo(realTransmittedVector);
        assertThat(persisted.getPreClipNorm()).isEqualTo(1.4);
        assertThat(persisted.getPostClipNorm()).isEqualTo(1.0);
    }

    @Test
    void aRefusedRoundIsStillPersistedWithItsEntries() {
        PrivacyLedgerService service = new PrivacyLedgerService(gateway, roundRepository, ledgerRepository, userRepository);
        when(userRepository.findAllByFlOptInTrue()).thenReturn(List.of(user(UUID.randomUUID())));
        when(roundRepository.count()).thenReturn(4L);
        FlRoundOutcome outcome = new FlRoundOutcome(5, false, "too few participants", 1, List.of());
        when(gateway.runRound(anyInt(), anyList())).thenReturn(outcome);
        when(roundRepository.save(any())).thenReturn(new FlRound(5, false, "too few participants", 1));

        Optional<FlRound> result = service.runNextRoundAndPersist();

        assertThat(result).isPresent();
        assertThat(result.get().isAccepted()).isFalse();
        verify(ledgerRepository, never()).save(any());
    }

    @Test
    void whenFlServiceIsUnreachableItLogsAndReturnsEmptyRatherThanThrowing() {
        PrivacyLedgerService service = new PrivacyLedgerService(gateway, roundRepository, ledgerRepository, userRepository);
        when(userRepository.findAllByFlOptInTrue()).thenReturn(List.of(user(UUID.randomUUID())));
        when(roundRepository.count()).thenReturn(0L);
        when(gateway.runRound(anyInt(), anyList())).thenThrow(new FlServiceUnavailableException("down", null));

        Optional<FlRound> result = service.runNextRoundAndPersist();

        assertThat(result).isEmpty();
        verify(roundRepository, never()).save(any());
    }

    @Test
    void withNobodyOptedInNoRoundIsRunAtAll() {
        PrivacyLedgerService service = new PrivacyLedgerService(gateway, roundRepository, ledgerRepository, userRepository);
        when(userRepository.findAllByFlOptInTrue()).thenReturn(List.of());

        assertThat(service.runNextRoundAndPersist()).isEmpty();
        verifyNoInteractions(gateway);
        verify(roundRepository, never()).save(any());
    }

    @Test
    void anEntryForAClientThatIsNotCurrentlyOptedInIsNeverPersisted() {
        PrivacyLedgerService service = new PrivacyLedgerService(gateway, roundRepository, ledgerRepository, userRepository);
        UUID participant = UUID.randomUUID();
        UUID withdrawn = UUID.randomUUID();
        when(userRepository.findAllByFlOptInTrue()).thenReturn(List.of(user(participant)));
        when(roundRepository.count()).thenReturn(0L);
        when(gateway.runRound(1, List.of(participant.toString()))).thenReturn(new FlRoundOutcome(1, true, null, 2, List.of(
                new FlLedgerEntryData(participant.toString(), 1, 1.0, 1.0, 1.0, 1.0, List.of(0.1)),
                new FlLedgerEntryData(withdrawn.toString(), 1, 1.0, 1.0, 1.0, 1.0, List.of(0.2)),
                new FlLedgerEntryData("not-a-uuid", 1, 1.0, 1.0, 1.0, 1.0, List.of(0.3)))));
        FlRound savedRound = new FlRound(1, true, null, 2);
        setRoundId(savedRound, UUID.randomUUID());
        when(roundRepository.save(any())).thenReturn(savedRound);

        service.runNextRoundAndPersist();

        var captor = org.mockito.ArgumentCaptor.forClass(PrivacyLedgerEntry.class);
        verify(ledgerRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(participant);
    }

    private User user(UUID id) {
        User user = new User(id + "@example.com", "hash", "Test User");
        try {
            java.lang.reflect.Field field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
        return user;
    }

    private void setRoundId(FlRound round, UUID id) {
        try {
            java.lang.reflect.Field field = FlRound.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(round, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
