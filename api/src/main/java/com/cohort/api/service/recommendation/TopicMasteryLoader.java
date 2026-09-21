package com.cohort.api.service.recommendation;

import com.cohort.api.entity.InterviewQuestion;
import com.cohort.api.entity.PracticeAttempt;
import com.cohort.api.entity.Question;
import com.cohort.api.entity.Topic;
import com.cohort.api.repository.InterviewQuestionRepository;
import com.cohort.api.repository.PracticeAttemptRepository;
import com.cohort.api.repository.QuestionRepository;
import com.cohort.api.repository.TopicRepository;
import com.cohort.api.service.preparation.InterviewEvidence;
import com.cohort.api.service.preparation.MasteryEngine;
import com.cohort.api.service.preparation.MasteryResult;
import com.cohort.api.service.preparation.PracticeEvidence;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Loads a {@link TopicMasterySnapshot} per topic for a user -- the
 * recommendation pipeline's own entry point into P30's {@link MasteryEngine},
 * carrying both the raw and decayed score (US-51/US-52's generators need
 * both; {@code PreparationService#progress} only ever exposed the decayed
 * one to the progress screen).
 *
 * <p>The evidence-loading queries below deliberately mirror
 * {@code PreparationService}'s own private {@code practiceEvidenceByTopic}/
 * {@code interviewEvidenceByTopic} methods rather than extracting a shared
 * component out of already-tested P29/P30 code mid-Sprint-4 ("smallest
 * commits" per BUILD_PHASES_JAVA.md) -- a reasonable follow-up refactor,
 * not a correctness concern, since both copies implement the identical
 * §21.4 evidence-gathering rule.
 */
@Component
public class TopicMasteryLoader {

    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final PracticeAttemptRepository practiceAttemptRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final MasteryEngine masteryEngine;

    public TopicMasteryLoader(
            TopicRepository topicRepository,
            QuestionRepository questionRepository,
            PracticeAttemptRepository practiceAttemptRepository,
            InterviewQuestionRepository interviewQuestionRepository,
            MasteryEngine masteryEngine) {
        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
        this.practiceAttemptRepository = practiceAttemptRepository;
        this.interviewQuestionRepository = interviewQuestionRepository;
        this.masteryEngine = masteryEngine;
    }

    public List<TopicMasterySnapshot> loadAll(UUID userId, Instant asOf) {
        List<Topic> topics = topicRepository.findAllByOrderByCategoryAscNameAsc();
        Map<UUID, List<PracticeEvidence>> practiceByTopic = practiceEvidenceByTopic(userId);
        Map<UUID, List<InterviewEvidence>> interviewByTopic = interviewEvidenceByTopic(userId);

        List<TopicMasterySnapshot> snapshots = new ArrayList<>();
        for (Topic topic : topics) {
            List<PracticeEvidence> practice = practiceByTopic.getOrDefault(topic.getId(), List.of());
            List<InterviewEvidence> interview = interviewByTopic.getOrDefault(topic.getId(), List.of());
            MasteryResult result = masteryEngine.computeMastery(practice, interview, topic.getDefaultDifficulty(), asOf);
            snapshots.add(new TopicMasterySnapshot(
                    topic.getId(), topic.getName(), result.rawScore(), result.masteryScore(),
                    result.observationCount(), result.sufficientEvidence(), mostRecentEvidence(practice, interview)));
        }
        return snapshots;
    }

    private Instant mostRecentEvidence(List<PracticeEvidence> practice, List<InterviewEvidence> interview) {
        Instant latest = null;
        for (PracticeEvidence e : practice) {
            latest = latest == null || e.occurredAt().isAfter(latest) ? e.occurredAt() : latest;
        }
        for (InterviewEvidence e : interview) {
            latest = latest == null || e.occurredAt().isAfter(latest) ? e.occurredAt() : latest;
        }
        return latest;
    }

    private Map<UUID, List<PracticeEvidence>> practiceEvidenceByTopic(UUID userId) {
        List<PracticeAttempt> attempts = practiceAttemptRepository.findAllByUserId(userId);
        if (attempts.isEmpty()) {
            return Map.of();
        }
        List<UUID> questionIds = attempts.stream().map(PracticeAttempt::getQuestionId).distinct().toList();
        Map<UUID, UUID> topicIdByQuestionId = new HashMap<>();
        for (Question question : questionRepository.findAllById(questionIds)) {
            topicIdByQuestionId.put(question.getId(), question.getTopicId());
        }

        Map<UUID, List<PracticeEvidence>> result = new HashMap<>();
        for (PracticeAttempt attempt : attempts) {
            if (attempt.getConfidence() == null) {
                continue;
            }
            UUID topicId = topicIdByQuestionId.get(attempt.getQuestionId());
            if (topicId == null) {
                continue;
            }
            result.computeIfAbsent(topicId, id -> new ArrayList<>())
                    .add(new PracticeEvidence(attempt.getConfidence(), attempt.getAttemptedAt()));
        }
        return result;
    }

    private Map<UUID, List<InterviewEvidence>> interviewEvidenceByTopic(UUID userId) {
        Map<UUID, List<InterviewEvidence>> result = new HashMap<>();
        for (InterviewQuestion question :
                interviewQuestionRepository.findAllByUserIdAndTopicIdIsNotNullAndSelfRatingIsNotNull(userId)) {
            result.computeIfAbsent(question.getTopicId(), id -> new ArrayList<>())
                    .add(new InterviewEvidence(question.getSelfRating(), question.getCreatedAt()));
        }
        return result;
    }
}
