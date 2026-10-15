package com.cohort.api.service.preparation.review;

import com.cohort.api.dto.ReviewItemResponse;
import com.cohort.api.dto.ReviewQueueResponse;
import com.cohort.api.entity.PracticeAttempt;
import com.cohort.api.entity.Question;
import com.cohort.api.repository.PracticeAttemptRepository;
import com.cohort.api.repository.QuestionRepository;
import com.cohort.api.service.recommendation.TopicMasteryLoader;
import com.cohort.api.service.recommendation.TopicMasterySnapshot;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReviewQueueService {
    private static final short MISS_CONFIDENCE_AT_MOST = 2;

    private final PracticeAttemptRepository attemptRepository;
    private final QuestionRepository questionRepository;
    private final TopicMasteryLoader masteryLoader;
    private final ReviewScheduler scheduler;
    private final Clock clock;

    public ReviewQueueService(
            PracticeAttemptRepository attemptRepository,
            QuestionRepository questionRepository,
            TopicMasteryLoader masteryLoader,
            ReviewScheduler scheduler,
            Clock clock) {
        this.attemptRepository = attemptRepository;
        this.questionRepository = questionRepository;
        this.masteryLoader = masteryLoader;
        this.scheduler = scheduler;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public ReviewQueueResponse queue(UUID userId) {
        Instant now = clock.instant();
        LocalDate today = now.atZone(ZoneOffset.UTC).toLocalDate();

        List<PracticeAttempt> attempts = attemptRepository.findAllByUserId(userId).stream()
                .filter(attempt -> attempt.getConfidence() != null)
                .toList();
        if (attempts.isEmpty()) {
            return new ReviewQueueResponse(today, List.of());
        }

        List<UUID> questionIds = attempts.stream().map(PracticeAttempt::getQuestionId).distinct().toList();
        Map<UUID, UUID> topicByQuestion = questionRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, Question::getTopicId));

        Map<UUID, List<PracticeAttempt>> attemptsByTopic = new HashMap<>();
        for (PracticeAttempt attempt : attempts) {
            UUID topicId = topicByQuestion.get(attempt.getQuestionId());
            if (topicId != null) {
                attemptsByTopic.computeIfAbsent(topicId, id -> new ArrayList<>()).add(attempt);
            }
        }

        Map<UUID, TopicMasterySnapshot> mastery = masteryLoader.loadAll(userId, now).stream()
                .collect(Collectors.toMap(TopicMasterySnapshot::topicId, Function.identity()));

        List<ReviewItemResponse> items = new ArrayList<>();
        attemptsByTopic.forEach((topicId, topicAttempts) -> {
            TopicMasterySnapshot snapshot = mastery.get(topicId);
            if (snapshot != null) {
                dueItem(userId, topicId, snapshot, topicAttempts, today).ifPresent(items::add);
            }
        });

        items.sort(Comparator.comparing(ReviewItemResponse::dueDate)
                .thenComparing(item -> item.masteryScore() == null ? 1.0 : item.masteryScore())
                .thenComparing(ReviewItemResponse::topicName));
        return new ReviewQueueResponse(today, items);
    }

    private Optional<ReviewItemResponse> dueItem(
            UUID userId, UUID topicId, TopicMasterySnapshot snapshot, List<PracticeAttempt> topicAttempts, LocalDate today) {
        List<ReviewAttempt> history = topicAttempts.stream()
                .map(attempt -> new ReviewAttempt(attempt.getAttemptedAt(), isMiss(attempt)))
                .toList();
        Optional<ReviewSchedule> schedule = scheduler.schedule(history);
        if (schedule.isEmpty() || schedule.get().dueDate().isAfter(today)) {
            return Optional.empty();
        }
        Optional<Question> question = pickQuestion(userId, topicId, topicAttempts);
        if (question.isEmpty()) {
            return Optional.empty();
        }

        LocalDate lastDay = schedule.get().lastPractisedAt().atZone(ZoneOffset.UTC).toLocalDate();
        Double masteryScore = snapshot.sufficientEvidence() ? snapshot.masteryScore() : null;
        return Optional.of(new ReviewItemResponse(
                topicId,
                snapshot.topicName(),
                question.get().getId(),
                question.get().getQuestionText(),
                schedule.get().dueDate(),
                ChronoUnit.DAYS.between(schedule.get().dueDate(), today),
                masteryScore,
                schedule.get().lastPractisedAt(),
                reason(ChronoUnit.DAYS.between(lastDay, today), masteryScore)));
    }

    private boolean isMiss(PracticeAttempt attempt) {
        return attempt.getStatus() == PracticeAttempt.Status.needs_review
                || attempt.getConfidence() <= MISS_CONFIDENCE_AT_MOST;
    }

    private Optional<Question> pickQuestion(UUID userId, UUID topicId, List<PracticeAttempt> topicAttempts) {
        Map<UUID, Instant> lastAttemptByQuestion = new HashMap<>();
        for (PracticeAttempt attempt : topicAttempts) {
            lastAttemptByQuestion.merge(attempt.getQuestionId(), attempt.getAttemptedAt(),
                    (a, b) -> a.isAfter(b) ? a : b);
        }
        return questionRepository.findAllByTopicId(topicId).stream()
                .filter(question -> question.isSystem() || userId.equals(question.getUserId()))
                .min(Comparator
                        .comparing((Question question) -> lastAttemptByQuestion.getOrDefault(question.getId(), Instant.MIN))
                        .thenComparing(Question::getQuestionText));
    }

    private String reason(long daysAgo, Double masteryScore) {
        String practised = daysAgo == 0 ? "Last practised today"
                : daysAgo == 1 ? "Last practised 1 day ago"
                : "Last practised " + daysAgo + " days ago";
        return masteryScore == null ? practised : practised + ", mastery " + Math.round(masteryScore * 100) + "%";
    }
}
