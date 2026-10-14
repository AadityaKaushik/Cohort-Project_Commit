package com.cohort.api.service.fl;

import com.cohort.api.entity.Application;
import com.cohort.api.entity.Difficulty;
import com.cohort.api.entity.Interview;
import com.cohort.api.entity.InterviewFeedback;
import com.cohort.api.entity.PracticeAttempt;
import com.cohort.api.entity.PreparationTask;
import com.cohort.api.entity.Question;
import com.cohort.api.entity.Resume;
import com.cohort.api.repository.ApplicationRepository;
import com.cohort.api.repository.InterviewFeedbackRepository;
import com.cohort.api.repository.InterviewRepository;
import com.cohort.api.repository.PracticeAttemptRepository;
import com.cohort.api.repository.PreparationTaskRepository;
import com.cohort.api.repository.QuestionRepository;
import com.cohort.api.repository.ResumeRepository;
import com.cohort.api.repository.TopicRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class FlFeatureExtractionService {
    private static final int TAXONOMY_FALLBACK_SIZE = 40;
    private static final double RECENCY_DECAY_DAYS = 30.0;
    private static final int CONSISTENCY_WINDOW_WEEKS = 8;
    private static final int CONFIDENCE_WINDOW = 10;
    private static final int TREND_WINDOW = 20;
    private static final int SELF_RATING_WINDOW = 5;
    private static final double PREP_LEAD_TIME_CAP_DAYS = 60.0;
    private static final double ROUNDS_PER_APPLICATION_CAP = 6.0;
    private static final int RESUME_VARIANT_CAP = 5;
    private static final double APPLICATION_VOLUME_LOG_CAP = 300.0;
    private static final double PRACTICE_VOLUME_LOG_CAP = 500.0;
    private static final double INTERVIEW_EXPERIENCE_LOG_CAP = 50.0;

    private final PracticeAttemptRepository practiceAttemptRepository;
    private final QuestionRepository questionRepository;
    private final TopicRepository topicRepository;
    private final PreparationTaskRepository preparationTaskRepository;
    private final ApplicationRepository applicationRepository;
    private final ResumeRepository resumeRepository;
    private final InterviewRepository interviewRepository;
    private final InterviewFeedbackRepository interviewFeedbackRepository;

    public FlFeatureExtractionService(
            PracticeAttemptRepository practiceAttemptRepository,
            QuestionRepository questionRepository,
            TopicRepository topicRepository,
            PreparationTaskRepository preparationTaskRepository,
            ApplicationRepository applicationRepository,
            ResumeRepository resumeRepository,
            InterviewRepository interviewRepository,
            InterviewFeedbackRepository interviewFeedbackRepository) {
        this.practiceAttemptRepository = practiceAttemptRepository;
        this.questionRepository = questionRepository;
        this.topicRepository = topicRepository;
        this.preparationTaskRepository = preparationTaskRepository;
        this.applicationRepository = applicationRepository;
        this.resumeRepository = resumeRepository;
        this.interviewRepository = interviewRepository;
        this.interviewFeedbackRepository = interviewFeedbackRepository;
    }

    public FlFeatureVector extract(UUID userId, Instant asOf) {
        List<PracticeAttempt> attempts = filterBefore(
                practiceAttemptRepository.findAllByUserId(userId), asOf, PracticeAttempt::getAttemptedAt);
        Map<UUID, Question> questionsById = loadQuestions(attempts);

        List<PreparationTask> tasks = preparationTaskRepository.findAllByUserId(userId).stream()
                .filter(t -> t.getCreatedAt() != null && !t.getCreatedAt().isAfter(asOf))
                .toList();
        List<Application> applications = filterBefore(applicationRepository.findAllByUserId(userId), asOf, Application::getCreatedAt);
        List<Resume> resumeVariants = filterBefore(resumeRepository.findAllByUserId(userId), asOf, Resume::getCreatedAt);
        List<Interview> rounds = filterBefore(interviewRepository.findAllByUserId(userId), asOf, this::interviewOccurredAt);
        List<InterviewFeedback> feedback = filterBefore(
                interviewFeedbackRepository.findAllByUserId(userId), asOf, InterviewFeedback::getSubmittedAt);

        Map<UUID, List<Double>> confidenceByTopic = new HashMap<>();
        for (PracticeAttempt attempt : attempts) {
            if (attempt.getConfidence() == null) {
                continue;
            }
            Question question = questionsById.get(attempt.getQuestionId());
            if (question == null) {
                continue;
            }
            confidenceByTopic
                    .computeIfAbsent(question.getTopicId(), k -> new ArrayList<>())
                    .add(clip(attempt.getConfidence() / 10.0));
        }
        List<Double> masteryPerTopic = confidenceByTopic.values().stream().map(this::mean).toList();
        double masteryMean = masteryPerTopic.isEmpty() ? 0.0 : mean(masteryPerTopic);
        double masteryMin = masteryPerTopic.isEmpty() ? 0.0 : masteryPerTopic.stream().mapToDouble(d -> d).min().orElse(0.0);
        double masteryStd = masteryPerTopic.isEmpty() ? 0.0 : clip(stddev(masteryPerTopic, masteryMean));

        double practiceVolume = clip(Math.log1p(attempts.size()) / Math.log1p(PRACTICE_VOLUME_LOG_CAP));

        Set<UUID> practicedTopics = new HashSet<>();
        for (PracticeAttempt attempt : attempts) {
            Question question = questionsById.get(attempt.getQuestionId());
            if (question != null) {
                practicedTopics.add(question.getTopicId());
            }
        }
        long taxonomySize = topicRepository.count();
        double topicBreadth = clip((double) practicedTopics.size() / (taxonomySize > 0 ? taxonomySize : TAXONOMY_FALLBACK_SIZE));

        double practiceRecency = attempts.isEmpty() ? 0.0 : clip(Math.exp(
                -daysBetween(latest(attempts, PracticeAttempt::getAttemptedAt), asOf) / RECENCY_DECAY_DAYS));

        int weeksWithActivity = 0;
        for (int week = 0; week < CONSISTENCY_WINDOW_WEEKS; week++) {
            Instant windowEnd = asOf.minusSeconds(week * 7L * 86400L);
            Instant windowStart = windowEnd.minusSeconds(7L * 86400L);
            boolean any = attempts.stream()
                    .anyMatch(a -> a.getAttemptedAt() != null && a.getAttemptedAt().isAfter(windowStart) && !a.getAttemptedAt().isAfter(windowEnd));
            if (any) {
                weeksWithActivity++;
            }
        }
        double practiceConsistency = (double) weeksWithActivity / CONSISTENCY_WINDOW_WEEKS;

        List<PracticeAttempt> byTime = attempts.stream()
                .filter(a -> a.getAttemptedAt() != null && a.getConfidence() != null)
                .sorted((a, b) -> a.getAttemptedAt().compareTo(b.getAttemptedAt()))
                .toList();
        List<PracticeAttempt> recentForConfidence = lastN(byTime, CONFIDENCE_WINDOW);
        double confidenceMean = recentForConfidence.isEmpty() ? 0.0
                : clip(recentForConfidence.stream().mapToDouble(a -> a.getConfidence() / 10.0).average().orElse(0.0));

        List<PracticeAttempt> recentForTrend = lastN(byTime, TREND_WINDOW);
        double confidenceTrend = trend(recentForTrend);

        double hardRatio = attempts.isEmpty() ? 0.0 : attempts.stream()
                .filter(a -> {
                    Question q = questionsById.get(a.getQuestionId());
                    return q != null && q.getDifficulty() == Difficulty.hard;
                })
                .count() / (double) attempts.size();

        double shareDsa = share(attempts, questionsById, Question.Category.dsa);
        double shareTechnical = share(attempts, questionsById, Question.Category.technical);
        double shareBehavioural = share(attempts, questionsById, Question.Category.behavioural);
        double shareSystemDesign = share(attempts, questionsById, Question.Category.system_design);

        long completedCount = tasks.stream()
                .filter(t -> t.getStatus() == PreparationTask.Status.completed && t.getCompletedAt() != null && !t.getCompletedAt().isAfter(asOf))
                .count();
        double taskCompletionRate = tasks.isEmpty() ? 0.0 : completedCount / (double) tasks.size();

        double prepLeadTime = attempts.isEmpty() ? 0.0
                : clip(Math.min(daysBetween(earliest(attempts, PracticeAttempt::getAttemptedAt), asOf), PREP_LEAD_TIME_CAP_DAYS)
                        / PREP_LEAD_TIME_CAP_DAYS);

        double interviewExperience = clip(Math.log1p(rounds.size()) / Math.log1p(INTERVIEW_EXPERIENCE_LOG_CAP));

        Map<UUID, Long> roundsByApplication = new HashMap<>();
        for (Interview round : rounds) {
            roundsByApplication.merge(round.getApplicationId(), 1L, Long::sum);
        }
        double roundsPerApplication = roundsByApplication.isEmpty() ? 0.0
                : clip(roundsByApplication.values().stream().mapToLong(Long::longValue).average().orElse(0.0) / ROUNDS_PER_APPLICATION_CAP);

        List<InterviewFeedback> ratedRecent = lastN(
                feedback.stream()
                        .filter(f -> f.getSelfRating() != null && f.getSubmittedAt() != null)
                        .sorted((a, b) -> a.getSubmittedAt().compareTo(b.getSubmittedAt()))
                        .toList(),
                SELF_RATING_WINDOW);
        double recentSelfRating = ratedRecent.isEmpty() ? 0.0
                : clip((ratedRecent.stream().mapToInt(InterviewFeedback::getSelfRating).average().orElse(1.0) - 1.0) / 4.0);

        Set<UUID> targetTopics = tasks.stream().map(PreparationTask::getTopicId).collect(java.util.stream.Collectors.toSet());
        Set<UUID> completedTaskTopics = tasks.stream()
                .filter(t -> t.getStatus() == PreparationTask.Status.completed)
                .map(PreparationTask::getTopicId)
                .collect(java.util.stream.Collectors.toSet());
        double coverageRatio = targetTopics.isEmpty() ? 0.0 : intersectionSize(practicedTopics, targetTopics) / (double) targetTopics.size();
        double repeatTopicRatio = targetTopics.isEmpty() ? 0.0 : intersectionSize(completedTaskTopics, targetTopics) / (double) targetTopics.size();

        double applicationVolume = clip(Math.log1p(applications.size()) / Math.log1p(APPLICATION_VOLUME_LOG_CAP));

        double resumeVariantsFeature = Math.min(resumeVariants.size(), RESUME_VARIANT_CAP) / (double) RESUME_VARIANT_CAP;

        double roundTypeOrdinal = rounds.isEmpty() ? 0.0
                : latestInterview(rounds).getRoundType().ordinal()
                        / (double) (com.cohort.api.entity.InterviewRoundType.values().length - 1);

        return new FlFeatureVector(
                masteryMean, masteryMin, masteryStd, practiceVolume, topicBreadth, practiceRecency,
                practiceConsistency, confidenceMean, confidenceTrend, hardRatio, shareDsa, shareTechnical,
                shareBehavioural, shareSystemDesign, taskCompletionRate, prepLeadTime, interviewExperience,
                roundsPerApplication, recentSelfRating, coverageRatio, repeatTopicRatio, applicationVolume,
                resumeVariantsFeature, roundTypeOrdinal);
    }

    private Instant interviewOccurredAt(Interview interview) {
        if (interview.getCompletedAt() != null) {
            return interview.getCompletedAt();
        }
        if (interview.getScheduledAt() != null) {
            return interview.getScheduledAt();
        }
        return interview.getCreatedAt();
    }

    private Map<UUID, Question> loadQuestions(List<PracticeAttempt> attempts) {
        Set<UUID> questionIds = attempts.stream().map(PracticeAttempt::getQuestionId).collect(java.util.stream.Collectors.toSet());
        if (questionIds.isEmpty()) {
            return Map.of();
        }
        Map<UUID, Question> result = new HashMap<>();
        for (Question question : questionRepository.findAllById(questionIds)) {
            result.put(question.getId(), question);
        }
        return result;
    }

    private double share(List<PracticeAttempt> attempts, Map<UUID, Question> questionsById, Question.Category category) {
        if (attempts.isEmpty()) {
            return 0.0;
        }
        long count = attempts.stream()
                .filter(a -> {
                    Question q = questionsById.get(a.getQuestionId());
                    return q != null && q.getCategory() == category;
                })
                .count();
        return count / (double) attempts.size();
    }

    private <T> List<T> filterBefore(List<T> items, Instant asOf, java.util.function.Function<T, Instant> timestamp) {
        return items.stream().filter(item -> {
            Instant ts = timestamp.apply(item);
            return ts != null && !ts.isAfter(asOf);
        }).toList();
    }

    private <T> List<T> lastN(List<T> sorted, int n) {
        if (sorted.size() <= n) {
            return sorted;
        }
        return sorted.subList(sorted.size() - n, sorted.size());
    }

    private <T> Instant latest(List<T> items, java.util.function.Function<T, Instant> timestamp) {
        return items.stream().map(timestamp).filter(java.util.Objects::nonNull).max(Instant::compareTo).orElse(null);
    }

    private <T> Instant earliest(List<T> items, java.util.function.Function<T, Instant> timestamp) {
        return items.stream().map(timestamp).filter(java.util.Objects::nonNull).min(Instant::compareTo).orElse(null);
    }

    private Interview latestInterview(List<Interview> rounds) {
        return rounds.stream().max((a, b) -> interviewOccurredAt(a).compareTo(interviewOccurredAt(b))).orElseThrow();
    }

    private double daysBetween(Instant from, Instant to) {
        if (from == null) {
            return Double.MAX_VALUE;
        }
        return (to.getEpochSecond() - from.getEpochSecond()) / 86400.0;
    }

    private double mean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double stddev(List<Double> values, double mean) {
        double variance = values.stream().mapToDouble(v -> (v - mean) * (v - mean)).average().orElse(0.0);
        return Math.sqrt(variance);
    }

    private double trend(List<PracticeAttempt> windowed) {
        if (windowed.size() < 2) {
            return 0.0;
        }
        int n = windowed.size();
        double meanX = (n - 1) / 2.0;
        double meanY = windowed.stream().mapToDouble(a -> a.getConfidence() / 10.0).average().orElse(0.0);
        double numerator = 0.0;
        double denominator = 0.0;
        for (int i = 0; i < n; i++) {
            double y = windowed.get(i).getConfidence() / 10.0;
            numerator += (i - meanX) * (y - meanY);
            denominator += (i - meanX) * (i - meanX);
        }
        double slope = denominator > 0 ? numerator / denominator : 0.0;
        return Math.max(-1.0, Math.min(1.0, slope * (n - 1)));
    }

    private long intersectionSize(Set<UUID> a, Set<UUID> b) {
        return a.stream().filter(b::contains).count();
    }

    private double clip(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
