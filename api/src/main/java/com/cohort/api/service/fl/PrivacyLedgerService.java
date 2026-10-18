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

    @Transactional
    public Optional<FlRound> runNextRoundAndPersist() {
        return runNextRoundAndPersist(null);
    }

    /** A non-null noise multiplier is the dev demo's lower-noise option; scheduled rounds pass null. */
    @Transactional
    public Optional<FlRound> runNextRoundAndPersist(Double noiseMultiplier) {
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
            outcome = gateway.runRound(nextRoundNumber, participants.stream().map(UUID::toString).sorted().toList(), noiseMultiplier);
        } catch (RuntimeException ex) {
            log.warn("FL round {} could not be run: {}", nextRoundNumber, ex.getMessage());
            return Optional.empty();
        }

        Double noiseUsed = outcome.entries().isEmpty() ? noiseMultiplier : Double.valueOf(outcome.entries().getFirst().noiseMultiplier());
        FlRound round = roundRepository.save(new FlRound(
                outcome.roundNumber(), outcome.accepted(), outcome.reason(), outcome.cohortSize(),
                outcome.satOutCount(), outcome.modelVersion(), outcome.checkAccuracy(), noiseUsed));
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

    public record PrivacyLedgerEntryView(PrivacyLedgerEntry entry, int roundNumber) {
    }
}
