package com.cohort.api.service.fl;

import com.cohort.api.dto.FlDemoStatusResponse;
import com.cohort.api.dto.FlDemoStatusResponse.CurrentModel;
import com.cohort.api.dto.FlDemoStatusResponse.RoundRow;
import com.cohort.api.dto.FlDemoStatusResponse.VersionPoint;
import com.cohort.api.dto.FlDemoStatusResponse.Weight;
import com.cohort.api.entity.User;
import com.cohort.api.repository.FlRoundRepository;
import com.cohort.api.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Dev profile only: assembles the federated learning demo page from fl-service and the stored rounds. */
@Profile("dev")
@Service
public class FlDemoService {
    private static final int MINIMUM_RATED_ROUNDS = 5;

    private final FlDemoClient demoClient;
    private final FlRoundRepository roundRepository;
    private final UserRepository userRepository;
    private final FlFeatureExtractionService extractionService;
    private final FederatedLiftCalculator liftCalculator;

    public FlDemoService(FlDemoClient demoClient, FlRoundRepository roundRepository, UserRepository userRepository,
                         FlFeatureExtractionService extractionService, FederatedLiftCalculator liftCalculator) {
        this.demoClient = demoClient;
        this.roundRepository = roundRepository;
        this.userRepository = userRepository;
        this.extractionService = extractionService;
        this.liftCalculator = liftCalculator;
    }

    @Transactional(readOnly = true)
    public FlDemoStatusResponse status(UUID userId) {
        boolean optedIn = userRepository.findById(userId).map(User::isFlOptIn).orElse(false);
        int ratedRounds = extractionService.trainingExamples(userId).size();
        List<RoundRow> rounds = roundRepository.findTop20ByOrderByCreatedAtDesc().stream()
                .map(round -> new RoundRow(
                        round.getRoundNumber(), round.getCreatedAt(), round.isAccepted(), round.getReason(),
                        round.getCohortSize(), round.getSatOutCount(), round.getModelVersion(),
                        round.getCheckAccuracy(), round.getNoiseMultiplier()))
                .toList();

        FlDemoClient.ModelHistory history;
        FlDemoClient.DemoStatus demo;
        try {
            history = demoClient.modelHistory();
            demo = demoClient.demoStatus();
        } catch (FlServiceUnavailableException ex) {
            return new FlDemoStatusResponse(false, ex.getMessage(), optedIn, ratedRounds, MINIMUM_RATED_ROUNDS, null,
                    0, List.of(), null, null, List.of(), rounds);
        }

        FlDemoClient.ModelVersion current = history.versions().getLast();
        List<String> names = history.featureNames();
        List<Weight> weights = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            weights.add(new Weight(names.get(i), current.parameters().get(i)));
        }
        FlModelInfo model = new FlModelInfo(
                current.version(), current.roundNumber(), current.participantCount(), current.createdAt(), current.parameters());
        double predicted = liftCalculator.predictedConfidence(model, extractionService.extract(userId, Instant.now()));
        List<VersionPoint> versions = history.versions().stream()
                .map(v -> new VersionPoint(v.version(), v.roundNumber(), v.participantCount(), v.checkAccuracy()))
                .toList();
        return new FlDemoStatusResponse(
                true, null, optedIn, ratedRounds, MINIMUM_RATED_ROUNDS, predicted, demo.syntheticClients(), demo.noiseLevels(),
                history.centralCeiling(),
                new CurrentModel(current.version(), current.roundNumber(), current.participantCount(), current.checkAccuracy(),
                        weights, current.parameters().get(names.size())),
                versions, rounds);
    }

    public void setSyntheticClients(int count, int seed) {
        demoClient.setSyntheticClients(count, seed);
    }

    public void reset() {
        demoClient.reset();
    }
}
