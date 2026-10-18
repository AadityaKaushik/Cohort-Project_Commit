package com.cohort.api.service.recommendation;

import com.cohort.api.dto.RecommendationResponse;
import com.cohort.api.entity.PreparationTask;
import com.cohort.api.entity.Recommendation;
import com.cohort.api.entity.RecommendationEvidence;
import com.cohort.api.exception.RecommendationNotFoundException;
import com.cohort.api.mapper.RecommendationMapper;
import com.cohort.api.repository.InterviewRepository;
import com.cohort.api.repository.PreparationTaskRepository;
import com.cohort.api.repository.RecommendationEvidenceRepository;
import com.cohort.api.repository.RecommendationRepository;
import com.cohort.api.repository.TopicRepository;
import com.cohort.api.service.application.StallDetectionService;
import com.cohort.api.service.fl.FederatedLiftCalculator;
import com.cohort.api.service.fl.FlFeatureExtractionService;
import com.cohort.api.service.fl.FlCircuitBreakerGateway;
import com.cohort.api.service.fl.FlModelInfo;
import com.cohort.api.service.recommendation.generator.BaselineGenerator;
import com.cohort.api.service.recommendation.generator.DecayGenerator;
import com.cohort.api.service.recommendation.generator.RecommendationGenerator;
import com.cohort.api.service.recommendation.generator.StalledApplicationGenerator;
import com.cohort.api.service.recommendation.generator.WeakTopicGenerator;
import com.cohort.api.service.recommendation.template.RecommendationTemplateRegistry;
import com.cohort.api.service.recommendation.tiering.RecommendationScorer;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecommendationFacade {
    static final int MAX_ACTIVE_RECOMMENDATIONS = 5;

    // US-56: cap stalled-application candidates at 3 so they do not crowd out the rest.
    static final int MAX_STALLED_APPLICATION_RECOMMENDATIONS = 3;

    static final int SNOOZE_DAYS = 7;

    public static final int DISMISS_SUPPRESSION_DAYS = 30;

    public static final int ACCEPT_SUPPRESSION_DAYS = 14;

    private final TopicMasteryLoader topicMasteryLoader;
    private final TopicRepository topicRepository;
    private final StallDetectionService stallDetectionService;
    private final InterviewRepository interviewRepository;
    private final RecommendationGenerator generatorChain;
    private final RootCauseRouter rootCauseRouter;
    private final RecommendationScorer scorer;
    private final RecommendationTemplateRegistry templateRegistry;
    private final RecommendationRepository recommendationRepository;
    private final RecommendationEvidenceRepository evidenceRepository;
    private final PreparationTaskRepository preparationTaskRepository;
    private final RecommendationMapper mapper;
    private final FlCircuitBreakerGateway flGateway;
    private final FederatedLiftCalculator liftCalculator;
    private final FlFeatureExtractionService featureExtractionService;

    public RecommendationFacade(
            TopicMasteryLoader topicMasteryLoader,
            TopicRepository topicRepository,
            StallDetectionService stallDetectionService,
            InterviewRepository interviewRepository,
            WeakTopicGenerator weakTopicGenerator,
            DecayGenerator decayGenerator,
            StalledApplicationGenerator stalledApplicationGenerator,
            BaselineGenerator baselineGenerator,
            RootCauseRouter rootCauseRouter,
            RecommendationScorer scorer,
            RecommendationTemplateRegistry templateRegistry,
            RecommendationRepository recommendationRepository,
            RecommendationEvidenceRepository evidenceRepository,
            PreparationTaskRepository preparationTaskRepository,
            RecommendationMapper mapper,
            FlCircuitBreakerGateway flGateway,
            FederatedLiftCalculator liftCalculator,
            FlFeatureExtractionService featureExtractionService) {
        this.topicMasteryLoader = topicMasteryLoader;
        this.topicRepository = topicRepository;
        this.stallDetectionService = stallDetectionService;
        this.interviewRepository = interviewRepository;
        weakTopicGenerator.linkTo(decayGenerator).linkTo(stalledApplicationGenerator).linkTo(baselineGenerator);
        this.generatorChain = weakTopicGenerator;
        this.rootCauseRouter = rootCauseRouter;
        this.scorer = scorer;
        this.templateRegistry = templateRegistry;
        this.recommendationRepository = recommendationRepository;
        this.evidenceRepository = evidenceRepository;
        this.preparationTaskRepository = preparationTaskRepository;
        this.mapper = mapper;
        this.flGateway = flGateway;
        this.liftCalculator = liftCalculator;
        this.featureExtractionService = featureExtractionService;
    }

    @Transactional
    public List<RecommendationResponse> generateRecommendations(UUID userId) {
        return generateRecommendations(userId, false);
    }

    @Transactional
    public List<RecommendationResponse> generateRecommendations(UUID userId, boolean federated) {
        Instant asOf = Instant.now();
        RecommendationContext context = buildContext(userId, asOf);

        List<RecommendationCandidate> candidates = generatorChain.handle(context, List.of());
        candidates = candidates.stream().map(candidate -> applyRouting(candidate, context)).toList();

        List<Recommendation> suppressionSource = recommendationRepository.findAllByUserIdAndStatusIn(
                userId, List.of(RecommendationStatus.snoozed, RecommendationStatus.dismissed, RecommendationStatus.accepted));
        candidates = candidates.stream().filter(candidate -> !isSuppressed(candidate, suppressionSource, asOf)).toList();

        List<RecommendationScorer.ScoredCandidate> localRanked = rankAndCap(candidates, asOf);

        Optional<FlModelInfo> model = federated ? flGateway.fetchCurrentModelSafely() : Optional.empty();
        boolean federatedApplied = model.isPresent();

        double predictedConfidence = model
                .map(current -> liftCalculator.predictedConfidence(current, featureExtractionService.extract(userId, asOf)))
                .orElse(FederatedLiftCalculator.NEUTRAL_CONFIDENCE);
        List<IndexedScoredCandidate> finalOrder = applyFederatedLift(localRanked, predictedConfidence, federatedApplied);

        List<CardWithLift> cards = finalOrder.stream()
                .map(indexed -> new CardWithLift(
                        RecommendationCard.builder()
                                .forUser(userId)
                                .withCandidate(indexed.scored().candidate())
                                .withTierAndScore(indexed.scored().tier(), indexed.scored().score())
                                .withCopy(templateRegistry.render(indexed.scored().candidate(), indexed.scored().tier()))
                                .build(),
                        indexed.rankDelta()))
                .toList();

        recommendationRepository.deleteAllByUserIdAndStatus(userId, RecommendationStatus.active);
        return cards.stream().map(cwl -> persistAndRespond(cwl.card(), federatedApplied, cwl.rankDelta())).toList();
    }

    private List<IndexedScoredCandidate> applyFederatedLift(
            List<RecommendationScorer.ScoredCandidate> localRanked, double predictedConfidence, boolean federatedApplied) {
        if (!federatedApplied) {
            return java.util.stream.IntStream.range(0, localRanked.size())
                    .mapToObj(i -> new IndexedScoredCandidate(localRanked.get(i), 0))
                    .toList();
        }
        List<ScoredWithMultiplier> withMultiplier = java.util.stream.IntStream.range(0, localRanked.size())
                .mapToObj(i -> new ScoredWithMultiplier(
                        i, localRanked.get(i),
                        liftCalculator.adjustmentMultiplier(predictedConfidence, localRanked.get(i).candidate().severity())))
                .toList();
        List<ScoredWithMultiplier> federatedOrder = withMultiplier.stream()
                .sorted(Comparator.comparingDouble((ScoredWithMultiplier s) -> s.scored().score() * s.multiplier()).reversed())
                .toList();
        return java.util.stream.IntStream.range(0, federatedOrder.size())
                .mapToObj(federatedIndex -> {
                    ScoredWithMultiplier entry = federatedOrder.get(federatedIndex);
                    int rankDelta = entry.localIndex() - federatedIndex;
                    return new IndexedScoredCandidate(entry.scored(), rankDelta);
                })
                .toList();
    }

    private record ScoredWithMultiplier(int localIndex, RecommendationScorer.ScoredCandidate scored, double multiplier) {
    }

    private record IndexedScoredCandidate(RecommendationScorer.ScoredCandidate scored, int rankDelta) {
    }

    private record CardWithLift(RecommendationCard card, int rankDelta) {
    }

    @Transactional
    public RecommendationResponse accept(UUID userId, UUID recommendationId) {
        Recommendation recommendation = requireOwned(userId, recommendationId);
        if (recommendation.getStatus() == RecommendationStatus.accepted) {
            return respond(recommendation);
        }
        if (recommendation.getTargetTopicId() != null) {
            PreparationTask task = new PreparationTask(userId, recommendation.getTargetTopicId(), recommendation.getHeadline());
            task.setDescription(recommendation.getExplanation());
            task = preparationTaskRepository.save(task);
            recommendation.setAcceptedTaskId(task.getId());
        }
        recommendation.setStatus(RecommendationStatus.accepted);
        return respond(recommendation);
    }

    @Transactional
    public RecommendationResponse snooze(UUID userId, UUID recommendationId) {
        Recommendation recommendation = requireOwned(userId, recommendationId);
        recommendation.setStatus(RecommendationStatus.snoozed);
        recommendation.setSnoozedUntil(Instant.now().plus(SNOOZE_DAYS, ChronoUnit.DAYS));
        return respond(recommendation);
    }

    @Transactional
    public RecommendationResponse dismiss(UUID userId, UUID recommendationId, String reason) {
        Recommendation recommendation = requireOwned(userId, recommendationId);
        recommendation.setStatus(RecommendationStatus.dismissed);
        recommendation.setDismissedReason(reason);
        return respond(recommendation);
    }

    @Transactional(readOnly = true)
    public List<RecommendationResponse> listHandled(UUID userId) {
        Instant asOf = Instant.now();
        return recommendationRepository
                .findAllByUserIdAndStatusIn(userId, List.of(
                        RecommendationStatus.accepted, RecommendationStatus.snoozed, RecommendationStatus.dismissed))
                .stream()
                .filter(recommendation -> {
                    Instant until = respond(recommendation).suppressedUntil();
                    return until == null || until.isAfter(asOf);
                })
                .sorted(Comparator.comparing(Recommendation::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::respond)
                .toList();
    }

    @Transactional
    public RecommendationResponse restore(UUID userId, UUID recommendationId) {
        Recommendation recommendation = requireOwned(userId, recommendationId);
        if (recommendation.getStatus() == RecommendationStatus.snoozed
                || recommendation.getStatus() == RecommendationStatus.dismissed) {
            recommendation.setStatus(RecommendationStatus.active);
            recommendation.setSnoozedUntil(null);
            recommendation.setDismissedReason(null);
        }
        return respond(recommendation);
    }

    private RecommendationContext buildContext(UUID userId, Instant asOf) {
        List<TopicMasterySnapshot> snapshots = topicMasteryLoader.loadAll(userId, asOf);
        Map<UUID, TopicMasterySnapshot> masteryByTopicId = new HashMap<>();
        for (TopicMasterySnapshot snapshot : snapshots) {
            masteryByTopicId.put(snapshot.topicId(), snapshot);
        }

        Map<UUID, List<UUID>> prerequisitesByTopic = new HashMap<>();
        for (TopicRepository.TopicPrerequisiteEdge edge : topicRepository.findAllPrerequisiteEdges()) {
            prerequisitesByTopic.computeIfAbsent(edge.getTopicId(), id -> new ArrayList<>()).add(edge.getPrerequisiteTopicId());
        }

        List<StallDetectionService.StalledApplication> stalledApplications = stallDetectionService.detectStalled(userId);
        int interviewCount = interviewRepository.findAllByUserId(userId).size();

        return new RecommendationContext(userId, asOf, snapshots, masteryByTopicId, prerequisitesByTopic, stalledApplications, interviewCount);
    }

    private RecommendationCandidate applyRouting(RecommendationCandidate candidate, RecommendationContext context) {
        if (candidate.type() != RecommendationType.weak_topic) {
            return candidate;
        }
        Map<UUID, Double> masteryValues = new HashMap<>();
        context.masteryByTopicId().forEach((id, snapshot) -> masteryValues.put(id, snapshot.masteryScore()));

        RootCauseRouter.RoutingResult result =
                rootCauseRouter.routeToRootCause(candidate.targetTopicId(), masteryValues, context.prerequisitesByTopic());
        if (result.chain().size() <= 1) {
            return candidate;
        }

        TopicMasterySnapshot rootSnapshot = context.masteryByTopicId().get(result.topicId());
        if (rootSnapshot == null) {
            return candidate;
        }

        List<String> chainDisplay = result.chain().stream()
                .map(context.masteryByTopicId()::get)
                .filter(Objects::nonNull)
                .map(snapshot -> "%s (%.0f%% mastery)".formatted(snapshot.topicName(), snapshot.masteryScore() * 100))
                .toList();
        List<EvidenceItem> rootEvidence = List.of(new EvidenceItem(
                "Prerequisite mastery",
                "%s mastery is %.0f%% from %d observations".formatted(
                        rootSnapshot.topicName(), rootSnapshot.masteryScore() * 100, rootSnapshot.observationCount()),
                rootSnapshot.lastEvidenceAt()));
        return candidate.routedTo(rootSnapshot, rootEvidence, chainDisplay);
    }

    private boolean isSuppressed(RecommendationCandidate candidate, List<Recommendation> suppressionSource, Instant asOf) {
        Instant dismissSuppressedSince = asOf.minus(DISMISS_SUPPRESSION_DAYS, ChronoUnit.DAYS);
        for (Recommendation existing : suppressionSource) {
            boolean sameType = existing.getType() == candidate.type();
            if (!sameType) {
                continue;
            }
            boolean sameTargetForAccepted = Objects.equals(existing.getTargetTopicId(), candidate.targetTopicId())
                    && Objects.equals(existing.getTargetApplicationId(), candidate.targetApplicationId());
            boolean acceptedRecently = existing.getStatus() == RecommendationStatus.accepted
                    && sameTargetForAccepted
                    && existing.getUpdatedAt() != null
                    && existing.getUpdatedAt().isAfter(asOf.minus(ACCEPT_SUPPRESSION_DAYS, ChronoUnit.DAYS));
            if (acceptedRecently) {
                return true;
            }
            boolean dismissedRecently = existing.getStatus() == RecommendationStatus.dismissed
                    && existing.getUpdatedAt() != null
                    && existing.getUpdatedAt().isAfter(dismissSuppressedSince);
            if (dismissedRecently) {
                return true;
            }
            boolean sameTarget = Objects.equals(existing.getTargetTopicId(), candidate.targetTopicId())
                    && Objects.equals(existing.getTargetApplicationId(), candidate.targetApplicationId());
            boolean stillSnoozed = existing.getStatus() == RecommendationStatus.snoozed
                    && sameTarget
                    && existing.getSnoozedUntil() != null
                    && existing.getSnoozedUntil().isAfter(asOf);
            if (stillSnoozed) {
                return true;
            }
        }
        return false;
    }

    private List<RecommendationScorer.ScoredCandidate> rankAndCap(List<RecommendationCandidate> candidates, Instant asOf) {
        List<RecommendationScorer.ScoredCandidate> scored = candidates.stream().map(c -> scorer.score(c, asOf)).toList();

        List<RecommendationScorer.ScoredCandidate> stalled = scored.stream()
                .filter(sc -> sc.candidate().type() == RecommendationType.stalled_application)
                .sorted(Comparator.comparingDouble(RecommendationScorer.ScoredCandidate::score).reversed())
                .limit(MAX_STALLED_APPLICATION_RECOMMENDATIONS)
                .toList();
        List<RecommendationScorer.ScoredCandidate> others = scored.stream()
                .filter(sc -> sc.candidate().type() != RecommendationType.stalled_application)
                .toList();

        List<RecommendationScorer.ScoredCandidate> combined = new ArrayList<>(stalled);
        combined.addAll(others);
        return combined.stream()
                .sorted(Comparator.comparingDouble(RecommendationScorer.ScoredCandidate::score).reversed())
                .limit(MAX_ACTIVE_RECOMMENDATIONS)
                .toList();
    }

    private RecommendationResponse persistAndRespond(RecommendationCard card, boolean federatedApplied, int rankDelta) {
        RecommendationCandidate candidate = card.candidate();
        Recommendation entity = new Recommendation(
                card.userId(), candidate.type(), card.tier(), candidate.targetTopicId(), candidate.targetApplicationId(),
                card.score(), card.headline(), card.explanation(), candidate.observationCount(), Instant.now());
        entity = recommendationRepository.save(entity);
        for (EvidenceItem item : candidate.evidence()) {
            evidenceRepository.save(new RecommendationEvidence(entity.getId(), item.label(), item.detail(), item.occurredAt()));
        }
        return mapper.toResponse(entity, candidate.evidence(), federatedApplied, rankDelta);
    }

    private RecommendationResponse respond(Recommendation recommendation) {
        List<RecommendationEvidence> evidence = evidenceRepository.findAllByRecommendationId(recommendation.getId());
        return mapper.toResponseFromPersistedEvidence(recommendation, evidence);
    }

    private Recommendation requireOwned(UUID userId, UUID recommendationId) {
        return recommendationRepository.findByIdAndUserId(recommendationId, userId).orElseThrow(RecommendationNotFoundException::new);
    }
}
