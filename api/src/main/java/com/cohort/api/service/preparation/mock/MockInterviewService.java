package com.cohort.api.service.preparation.mock;

import com.cohort.api.dto.MockInterviewQuestionResponse;
import com.cohort.api.dto.MockInterviewResponse;
import com.cohort.api.entity.PracticeAttempt;
import com.cohort.api.entity.Question;
import com.cohort.api.exception.InvalidMockInterviewCountException;
import com.cohort.api.repository.PracticeAttemptRepository;
import com.cohort.api.repository.QuestionRepository;
import com.cohort.api.service.recommendation.TopicMasteryLoader;
import com.cohort.api.service.recommendation.TopicMasterySnapshot;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MockInterviewService {
    public static final int MIN_COUNT = 3;
    public static final int MAX_COUNT = 10;

    private final PracticeAttemptRepository attemptRepository;
    private final QuestionRepository questionRepository;
    private final TopicMasteryLoader masteryLoader;
    private final Clock clock;

    public MockInterviewService(
            PracticeAttemptRepository attemptRepository,
            QuestionRepository questionRepository,
            TopicMasteryLoader masteryLoader,
            Clock clock) {
        this.attemptRepository = attemptRepository;
        this.questionRepository = questionRepository;
        this.masteryLoader = masteryLoader;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public MockInterviewResponse build(UUID userId, int count) {
        if (count < MIN_COUNT || count > MAX_COUNT) {
            throw new InvalidMockInterviewCountException(
                    "Must be between " + MIN_COUNT + " and " + MAX_COUNT + ".");
        }
        List<TopicMasterySnapshot> snapshots = masteryLoader.loadAll(userId, clock.instant());
        Map<UUID, Instant> lastAttemptByQuestion = lastAttemptByQuestion(userId);

        List<TopicMasterySnapshot> ordered = new ArrayList<>(snapshots.stream()
                .filter(TopicMasterySnapshot::sufficientEvidence)
                .sorted(Comparator.comparingDouble(TopicMasterySnapshot::masteryScore)
                        .thenComparing(TopicMasterySnapshot::topicName))
                .toList());
        // Topics with only a few attempts come before untouched ones, weakest first.
        ordered.addAll(snapshots.stream()
                .filter(snapshot -> !snapshot.sufficientEvidence() && snapshot.observationCount() > 0)
                .sorted(Comparator.comparingDouble(TopicMasterySnapshot::masteryScore)
                        .thenComparing(TopicMasterySnapshot::topicName))
                .toList());
        ordered.addAll(snapshots.stream()
                .filter(snapshot -> !snapshot.sufficientEvidence() && snapshot.observationCount() == 0)
                .sorted(Comparator.comparing(TopicMasterySnapshot::topicName))
                .toList());

        List<MockInterviewQuestionResponse> picked = new ArrayList<>();
        for (TopicMasterySnapshot snapshot : ordered) {
            if (picked.size() == count) {
                break;
            }
            pickQuestion(userId, snapshot.topicId(), lastAttemptByQuestion)
                    .map(question -> toResponse(question, snapshot))
                    .ifPresent(picked::add);
        }
        return new MockInterviewResponse(picked);
    }

    private Map<UUID, Instant> lastAttemptByQuestion(UUID userId) {
        return attemptRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(PracticeAttempt::getQuestionId, PracticeAttempt::getAttemptedAt,
                        (a, b) -> a.isAfter(b) ? a : b, HashMap::new));
    }

    private Optional<Question> pickQuestion(UUID userId, UUID topicId, Map<UUID, Instant> lastAttemptByQuestion) {
        return questionRepository.findAllByTopicId(topicId).stream()
                .filter(question -> question.isSystem() || userId.equals(question.getUserId()))
                .min(Comparator
                        .comparing((Question question) -> lastAttemptByQuestion.getOrDefault(question.getId(), Instant.MIN))
                        .thenComparing(Question::getQuestionText));
    }

    private MockInterviewQuestionResponse toResponse(Question question, TopicMasterySnapshot snapshot) {
        Double masteryScore = snapshot.sufficientEvidence() ? snapshot.masteryScore() : null;
        String reason;
        if (masteryScore != null) {
            reason = "Mastery " + Math.round(masteryScore * 100) + "%";
        } else if (snapshot.observationCount() > 0) {
            reason = "Only " + snapshot.observationCount() + (snapshot.observationCount() == 1 ? " attempt" : " attempts") + " so far";
        } else {
            reason = "Not practised yet";
        }
        return new MockInterviewQuestionResponse(
                question.getId(), question.getQuestionText(), snapshot.topicId(), snapshot.topicName(),
                question.getDifficulty(), masteryScore, reason);
    }
}
