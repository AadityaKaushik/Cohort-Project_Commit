package com.cohort.api.service.fl;

import com.cohort.api.entity.FlRound;
import com.cohort.api.entity.PrivacyLedgerEntry;
import com.cohort.api.entity.User;
import com.cohort.api.exception.PrivacyLedgerEntryNotFoundException;
import com.cohort.api.repository.FlRoundRepository;
import com.cohort.api.repository.PrivacyLedgerEntryRepository;
import com.cohort.api.repository.UserRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-61/US-63 (P55): triggers a federated round through
 * {@link FederatedLearningGateway} and persists exactly what fl-service
 * reports: round metadata into {@code fl.fl_rounds}, one immutable row
 * per client into {@code fl.privacy_ledger_entries}, carrying the real
 * transmitted vector {@code FlowerServiceAdapter} parsed straight out of
 * fl-service's response (P46's clip-then-noise output, never a
 * placeholder). Rows are never updated after insert. "Entries are
 * immutable" (US-63) is enforced by this service having no update
 * method at all.
 *
 * <p>Only users opted in <em>right now</em> are sent as participants
 * (US-64). fl-service's feature store still holds a withdrawn user's last
 * export, so the participant list, not the store's contents, decides who
 * is in a round. Any entry fl-service returns for someone outside that
 * list is dropped here as a second line of defence.
 */
@Service
public class PrivacyLedgerService {

    private static final Logger log = LoggerFactory.getLogger(PrivacyLedgerService.class);

    private final FederatedLearningGateway gateway;
    private final FlRoundRepository roundRepository;
    private final PrivacyLedgerEntryRepository ledgerRepository;
    private final UserRepository userRepository;

    public PrivacyLedgerService(
            FederatedLearningGateway gateway,
            FlRoundRepository roundRepository,
            PrivacyLedgerEntryRepository ledgerRepository,
            UserRepository userRepository) {
        this.gateway = gateway;
        this.roundRepository = roundRepository;
        this.ledgerRepository = ledgerRepository;
        this.userRepository = userRepository;
    }

    /**
     * Runs the next round and persists its outcome. Best-effort like
     * {@code FlFeatureExportJob}: if nobody is opted in, or fl-service is
     * unreachable, this logs and returns empty rather than throwing. It is
     * background orchestration, not a user-facing request that P53's
     * circuit breaker needs to protect.
     */
    @Transactional
    public Optional<FlRound> runNextRoundAndPersist() {
        Set<UUID> participants = userRepository.findAllByFlOptInTrue().stream()
                .map(User::getId)
                .collect(Collectors.toSet());
        if (participants.isEmpty()) {
            log.info("FL round skipped: no users are opted in");
            return Optional.empty();
        }

        int nextRoundNumber = (int) roundRepository.count() + 1;
        FlRoundOutcome outcome;
        try {
            outcome = gateway.runRound(nextRoundNumber, participants.stream().map(UUID::toString).sorted().toList());
        } catch (RuntimeException ex) {
            log.warn("FL round {} could not be run: {}", nextRoundNumber, ex.getMessage());
            return Optional.empty();
        }

        FlRound round = roundRepository.save(new FlRound(outcome.roundNumber(), outcome.accepted(), outcome.reason(), outcome.cohortSize()));
        for (FlLedgerEntryData entry : outcome.entries()) {
            Optional<UUID> userId = parseUserId(entry.clientId());
            if (userId.isEmpty() || !participants.contains(userId.get())) {
                log.warn("FL round {}: dropping ledger entry for non-participant client {}", outcome.roundNumber(), entry.clientId());
                continue;
            }
            ledgerRepository.save(new PrivacyLedgerEntry(
                    round.getId(), userId.get(), entry.schemaVersion(), entry.preClipNorm(), entry.postClipNorm(),
                    entry.noiseMultiplier(), outcome.cohortSize(), entry.epsilon(), entry.transmittedVector()));
        }
        return Optional.of(round);
    }

    @Transactional(readOnly = true)
    public List<PrivacyLedgerEntryView> listForUser(UUID userId) {
        List<PrivacyLedgerEntry> entries = ledgerRepository.findAllByUserIdOrderByCreatedAtDesc(userId);
        Map<UUID, Integer> roundNumbersById = new HashMap<>();
        for (PrivacyLedgerEntry entry : entries) {
            roundNumbersById.computeIfAbsent(entry.getRoundId(), this::roundNumberOf);
        }
        return entries.stream()
                .map(entry -> new PrivacyLedgerEntryView(entry, roundNumbersById.get(entry.getRoundId())))
                .toList();
    }

    /** FR-95: a missing ID and another user's ID are the same 404. */
    @Transactional(readOnly = true)
    public PrivacyLedgerEntryView getForUser(UUID entryId, UUID userId) {
        PrivacyLedgerEntry entry = ledgerRepository.findByIdAndUserId(entryId, userId)
                .orElseThrow(PrivacyLedgerEntryNotFoundException::new);
        return new PrivacyLedgerEntryView(entry, roundNumberOf(entry.getRoundId()));
    }

    private int roundNumberOf(UUID roundId) {
        return roundRepository.findById(roundId).map(FlRound::getRoundNumber).orElse(-1);
    }

    private Optional<UUID> parseUserId(String clientId) {
        try {
            return Optional.of(UUID.fromString(clientId));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    /** Pairs a persisted entry with its round number, resolved once per unique round rather than per row. */
    public record PrivacyLedgerEntryView(PrivacyLedgerEntry entry, int roundNumber) {
    }
}
