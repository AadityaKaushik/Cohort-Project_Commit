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

/**
 * Facade pattern (ARCHITECTURE_REVISION_JAVA.md §4.1): hides the
 * recommendation pipeline's five internal stages (§21.2: build context -&gt;
 * generate -&gt; route -&gt; score -&gt; template, plus persistence) behind two
 * methods a controller can call without knowing any of P39-P41 exists.
 * {@code RecommendationController} calls only {@link #generateRecommendations}
 * and the three action methods below; everything else in this class is
 * private wiring between the Chain of Responsibility (P39), the root-cause
 * router (P40), the Strategy-based scorer, and the template registry (P41).
 */
@Service
public class RecommendationFacade {

    /** US-51: "Ranked list of at most 5." */
    static final int MAX_ACTIVE_RECOMMENDATIONS = 5;

    /** US-56: "capped at 3 concurrently." Applied before the overall cap so stalled-application candidates don't crowd out everything else, but also never exceed their own ceiling. */
    static final int MAX_STALLED_APPLICATION_RECOMMENDATIONS = 3;

    /** US-55: "snooze hides it for 7 days." */
    static final int SNOOZE_DAYS = 7;

    /** US-55: "dismiss ... suppresses that recommendation type for 30 days." */
    public static final int DISMISS_SUPPRESSION_DAYS = 30;

    /** P78: an accepted recommendation (and its task) is not offered again straight away. */
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
            FederatedLiftCalculator liftCalculator) {
        this.topicMasteryLoader = topicMasteryLoader;
        this.topicRepository = topicRepository;
        this.stallDetectionService = stallDetectionService;
        this.interviewRepository = interviewRepository;
        // P39's chain, wired once here -- the one place generator order is decided.
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
    }

    /**
     * US-51: regenerates this user's {@code active} recommendations and
     * returns them, ranked. Snoozed/dismissed rows are left untouched and
     * read back only to suppress regenerating the same candidate (US-55) --
     * {@code active} is the only status this method ever writes.
     */
    @Transactional
    public List<RecommendationResponse> generateRecommendations(UUID userId) {
        return generateRecommendations(userId, false);
    }

    /**
     * {@code federated}: US-65's toggle. When true, this method attempts to
     * fetch the current global model through {@link FlCircuitBreakerGateway}
     * (P53's circuit breaker) and, on success, re-ranks the exact same
     * local-only candidate list by a bounded per-candidate multiplier
     * (P57's {@link FederatedLiftCalculator}) -- every response in this
     * batch is then marked {@code federatedApplied = true} with a
     * {@code rankDelta} showing how far it moved versus the local-only
     * order. On <em>any</em> failure (fl-service down, slow, breaker
     * open), the gateway returns empty and this method silently proceeds
     * with the exact same local-only ranking {@code federated=false}
     * would have produced, every {@code rankDelta} left at 0 -- the
     * 200-not-500 guarantee P53's fault-injection test checks, and the
     * same local-only path the lift viewer compares against.
     */
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

        List<IndexedScoredCandidate> finalOrder = applyFederatedLift(localRanked, model, federatedApplied);

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

    /**
     * P57: re-ranks {@code localRanked} by each candidate's bounded
     * federated multiplier (never done when {@code federatedApplied} is
     * false, which is exactly local-only ranking, unchanged) and computes
     * each candidate's {@code rankDelta} against its original local-only
     * position.
     */
    private List<IndexedScoredCandidate> applyFederatedLift(
            List<RecommendationScorer.ScoredCandidate> localRanked, Optional<FlModelInfo> model, boolean federatedApplied) {
        if (!federatedApplied) {
            return java.util.stream.IntStream.range(0, localRanked.size())
                    .mapToObj(i -> new IndexedScoredCandidate(localRanked.get(i), 0))
                    .toList();
        }
        FlModelInfo currentModel = model.orElseThrow();
        List<ScoredWithMultiplier> withMultiplier = java.util.stream.IntStream.range(0, localRanked.size())
                .mapToObj(i -> new ScoredWithMultiplier(
                        i, localRanked.get(i),
                        liftCalculator.adjustmentMultiplier(currentModel, localRanked.get(i).candidate().severity())))
                .toList();
        List<ScoredWithMultiplier> federatedOrder = withMultiplier.stream()
                .sorted(Comparator.comparingDouble((ScoredWithMultiplier s) -> s.scored().score() * s.multiplier()).reversed())
                .toList();
        return java.util.stream.IntStream.range(0, federatedOrder.size())
                .mapToObj(federatedIndex -> {
                    ScoredWithMultiplier entry = federatedOrder.get(federatedIndex);
                    int rankDelta = entry.localIndex() - federatedIndex; // positive = moved up/earlier
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

    /** P78: everything the user has already acted on, newest first, so it can be reviewed and snoozes or dismissals undone. */
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

    /** P78: undoes a snooze or a dismissal so the recommendation (and its type) can be generated again. */
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

    /** US-52: redirects a weak-topic candidate to its root cause per §21.5, if routing finds one worth redirecting to. */
    private RecommendationCandidate applyRouting(RecommendationCandidate candidate, RecommendationContext context) {
        if (candidate.type() != RecommendationType.weak_topic) {
            return candidate;
        }
        Map<UUID, Double> masteryValues = new HashMap<>();
        context.masteryByTopicId().forEach((id, snapshot) -> masteryValues.put(id, snapshot.masteryScore()));

        RootCauseRouter.RoutingResult result =
                rootCauseRouter.routeToRootCause(candidate.targetTopicId(), masteryValues, context.prerequisitesByTopic());
        if (result.chain().size() <= 1) {
            return candidate; // no routing occurred
        }

        TopicMasterySnapshot rootSnapshot = context.masteryByTopicId().get(result.topicId());
        if (rootSnapshot == null) {
            return candidate; // defensive: routed to a topic this context has no snapshot for
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

    /** US-55's suppression rules: a dismissed type stays suppressed for 30 days; a snoozed exact target stays hidden until its snooze expires. */
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

    /** US-56's per-type cap applied before US-51's overall cap, then ranks everything by score descending. */
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
