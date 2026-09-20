package com.cohort.api.service;

import com.cohort.api.dto.CategoryProgress;
import com.cohort.api.dto.CreatePracticeAttemptRequest;
import com.cohort.api.dto.CreatePreparationTaskRequest;
import com.cohort.api.dto.CreateQuestionRequest;
import com.cohort.api.dto.PagedResponse;
import com.cohort.api.dto.PracticeAttemptResponse;
import com.cohort.api.dto.PreparationProgressResponse;
import com.cohort.api.dto.PreparationTaskResponse;
import com.cohort.api.dto.QuestionResponse;
import com.cohort.api.dto.TopicGraphResponse;
import com.cohort.api.dto.TopicMasteryView;
import com.cohort.api.dto.TopicResponse;
import com.cohort.api.dto.UpdatePreparationTaskRequest;
import com.cohort.api.dto.UpdateQuestionRequest;
import com.cohort.api.entity.Difficulty;
import com.cohort.api.entity.InterviewQuestion;
import com.cohort.api.entity.PracticeAttempt;
import com.cohort.api.entity.PreparationTask;
import com.cohort.api.entity.Question;
import com.cohort.api.entity.Topic;
import com.cohort.api.exception.PreparationTaskNotFoundException;
import com.cohort.api.exception.QuestionNotFoundException;
import com.cohort.api.exception.TopicNotFoundException;
import com.cohort.api.mapper.PreparationMapper;
import com.cohort.api.repository.InterviewQuestionRepository;
import com.cohort.api.repository.PracticeAttemptRepository;
import com.cohort.api.repository.PreparationTaskRepository;
import com.cohort.api.repository.QuestionRepository;
import com.cohort.api.repository.TopicRepository;
import com.cohort.api.repository.specification.QuestionSpecifications;
import com.cohort.api.service.preparation.InterviewEvidence;
import com.cohort.api.service.preparation.MasteryEngine;
import com.cohort.api.service.preparation.MasteryResult;
import com.cohort.api.service.preparation.PracticeEvidence;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-35..41: the question bank (topics + questions, mixed shared/owned
 * data), practice attempts, preparation tasks (strictly user-owned), and
 * the mastery-driven progress view. Topics are read-only system data —
 * there is no create/update/delete path for them here, only for questions,
 * attempts, and tasks (FR-94/95 apply to the latter three).
 */
@Service
public class PreparationService {

    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final PracticeAttemptRepository attemptRepository;
    private final PreparationTaskRepository taskRepository;
    private final InterviewQuestionRepository interviewQuestionRepository;
    private final MasteryEngine masteryEngine;
    private final PreparationMapper mapper;

    public PreparationService(
            TopicRepository topicRepository,
            QuestionRepository questionRepository,
            PracticeAttemptRepository attemptRepository,
            PreparationTaskRepository taskRepository,
            InterviewQuestionRepository interviewQuestionRepository,
            MasteryEngine masteryEngine,
            PreparationMapper mapper) {
        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
        this.taskRepository = taskRepository;
        this.interviewQuestionRepository = interviewQuestionRepository;
        this.masteryEngine = masteryEngine;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<TopicResponse> listTopics() {
        return topicRepository.findAllByOrderByCategoryAscNameAsc().stream().map(mapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public TopicGraphResponse topicGraph() {
        List<TopicResponse> nodes = listTopics();
        List<TopicGraphResponse.Edge> edges = topicRepository.findAllPrerequisiteEdges().stream()
                .map(edge -> new TopicGraphResponse.Edge(edge.getTopicId(), edge.getPrerequisiteTopicId()))
                .toList();
        return new TopicGraphResponse(nodes, edges);
    }

    @Transactional(readOnly = true)
    public PagedResponse<QuestionResponse> listQuestions(
            UUID userId, Question.Category category, UUID topicId, Difficulty difficulty, Boolean isSystem, String q,
            int page, int size) {
        Specification<Question> spec = QuestionSpecifications.combine(userId, category, topicId, difficulty, isSystem, q);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Question> result = questionRepository.findAll(spec, pageable);
        return new PagedResponse<>(toResponses(result.getContent()), result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public QuestionResponse getVisibleQuestion(UUID userId, UUID questionId) {
        Question question = requireVisible(userId, questionId);
        return mapper.toResponse(question, topicName(question.getTopicId()));
    }

    @Transactional
    public QuestionResponse createQuestion(UUID userId, CreateQuestionRequest request) {
        Topic topic = topicRepository.findById(request.topicId()).orElseThrow(TopicNotFoundException::new);
        Question question = Question.ownedByUser(userId, topic.getId(), request.questionText(), request.category());
        question.setDifficulty(request.difficulty());
        question = questionRepository.save(question);
        return mapper.toResponse(question, topic.getName());
    }

    /** US-36: seeded questions are read-only but can be forked into a personal copy. */
    @Transactional
    public QuestionResponse forkQuestion(UUID userId, UUID questionId) {
        Question source = questionRepository.findById(questionId).orElseThrow(QuestionNotFoundException::new);
        Question fork = Question.ownedByUser(userId, source.getTopicId(), source.getQuestionText(), source.getCategory());
        fork.setDifficulty(source.getDifficulty());
        fork.setSourceQuestionId(source.getId());
        fork = questionRepository.save(fork);
        return mapper.toResponse(fork, topicName(fork.getTopicId()));
    }

    @Transactional
    public QuestionResponse updateQuestion(UUID userId, UUID questionId, UpdateQuestionRequest request) {
        Question question = requireOwned(userId, questionId);

        if (request.topicId() != null) {
            Topic topic = topicRepository.findById(request.topicId()).orElseThrow(TopicNotFoundException::new);
            question.setTopicId(topic.getId());
        }
        if (request.questionText() != null) {
            question.setQuestionText(request.questionText());
        }
        if (request.category() != null) {
            question.setCategory(request.category());
        }
        if (request.difficulty() != null) {
            question.setDifficulty(request.difficulty());
        }

        return mapper.toResponse(question, topicName(question.getTopicId()));
    }

    @Transactional
    public void deleteQuestion(UUID userId, UUID questionId) {
        Question question = requireOwned(userId, questionId);
        questionRepository.delete(question);
    }

    @Transactional
    public PracticeAttemptResponse recordAttempt(UUID userId, UUID questionId, CreatePracticeAttemptRequest request) {
        requireVisible(userId, questionId);

        Instant attemptedAt = request.attemptedAt() != null ? request.attemptedAt() : Instant.now();
        PracticeAttempt attempt = new PracticeAttempt(userId, questionId, request.status(), attemptedAt);
        attempt.setConfidence(request.confidence());
        attempt.setTimeTakenMinutes(request.timeTakenMinutes());
        attempt.setNotes(request.notes());

        attempt = attemptRepository.save(attempt);
        return mapper.toResponse(attempt);
    }

    @Transactional(readOnly = true)
    public List<PracticeAttemptResponse> listAttempts(UUID userId, UUID questionId) {
        requireVisible(userId, questionId);
        return attemptRepository.findAllByUserIdAndQuestionIdOrderByAttemptedAtDesc(userId, questionId).stream()
                .map(mapper::toResponse)
                .toList();
    }

    /** A question is visible if it is seeded (system) or owned by the caller — read access, US-35. */
    private Question requireVisible(UUID userId, UUID questionId) {
        Question question = questionRepository.findById(questionId).orElseThrow(QuestionNotFoundException::new);
        if (!question.isSystem() && !userId.equals(question.getUserId())) {
            throw new QuestionNotFoundException();
        }
        return question;
    }

    /** Write access (update/delete) is always own-only, even for a caller who could otherwise see the row (US-36: "seeded questions are read-only"). */
    private Question requireOwned(UUID userId, UUID questionId) {
        return questionRepository.findByIdAndUserId(questionId, userId).orElseThrow(QuestionNotFoundException::new);
    }

    private String topicName(UUID topicId) {
        return topicRepository.findById(topicId).map(Topic::getName).orElse(null);
    }

    private List<QuestionResponse> toResponses(List<Question> questions) {
        Map<UUID, String> topicNames = new HashMap<>();
        return questions.stream()
                .map(question -> mapper.toResponse(
                        question, topicNames.computeIfAbsent(question.getTopicId(), this::topicName)))
                .toList();
    }

    // ---- US-40: preparation tasks ----

    @Transactional
    public PreparationTaskResponse createTask(UUID userId, CreatePreparationTaskRequest request) {
        Topic topic = topicRepository.findById(request.topicId()).orElseThrow(TopicNotFoundException::new);
        PreparationTask task = new PreparationTask(userId, topic.getId(), request.title());
        task.setDescription(request.description());
        task.setDueDate(request.dueDate());
        task.setEstimatedEffortMinutes(request.estimatedEffortMinutes());
        task = taskRepository.save(task);
        return mapper.toResponse(task, topic.getName());
    }

    @Transactional(readOnly = true)
    public List<PreparationTaskResponse> listTasks(UUID userId) {
        List<PreparationTask> tasks = taskRepository.findAllByUserIdOrderByDueDateAscCreatedAtDesc(userId);
        Map<UUID, String> topicNames = new HashMap<>();
        return tasks.stream()
                .map(task -> mapper.toResponse(task, topicNames.computeIfAbsent(task.getTopicId(), this::topicName)))
                .toList();
    }

    @Transactional
    public PreparationTaskResponse updateTask(UUID userId, UUID taskId, UpdatePreparationTaskRequest request) {
        PreparationTask task = requireOwnedTask(userId, taskId);

        if (request.topicId() != null) {
            Topic topic = topicRepository.findById(request.topicId()).orElseThrow(TopicNotFoundException::new);
            task.setTopicId(topic.getId());
        }
        if (request.title() != null) {
            task.setTitle(request.title());
        }
        if (request.description() != null) {
            task.setDescription(request.description());
        }
        if (request.dueDate() != null) {
            task.setDueDate(request.dueDate());
        }
        if (request.estimatedEffortMinutes() != null) {
            task.setEstimatedEffortMinutes(request.estimatedEffortMinutes());
        }
        // US-40: "completion updates progress" -- flipping to completed stamps completedAt exactly once.
        if (request.status() != null && request.status() != task.getStatus()) {
            task.setStatus(request.status());
            task.setCompletedAt(request.status() == PreparationTask.Status.completed ? Instant.now() : null);
        }

        return mapper.toResponse(task, topicName(task.getTopicId()));
    }

    @Transactional
    public void deleteTask(UUID userId, UUID taskId) {
        PreparationTask task = requireOwnedTask(userId, taskId);
        taskRepository.deleteByIdAndUserId(task.getId(), userId);
    }

    private PreparationTask requireOwnedTask(UUID userId, UUID taskId) {
        return taskRepository.findByIdAndUserId(taskId, userId).orElseThrow(PreparationTaskNotFoundException::new);
    }

    // ---- US-41: progress view, built on the P30 MasteryEngine ----

    @Transactional(readOnly = true)
    public PreparationProgressResponse progress(UUID userId) {
        List<Topic> topics = topicRepository.findAllByOrderByCategoryAscNameAsc();

        Map<UUID, List<PracticeEvidence>> practiceByTopic = practiceEvidenceByTopic(userId);
        Map<UUID, List<InterviewEvidence>> interviewByTopic = interviewEvidenceByTopic(userId);
        List<PreparationTask> tasks = taskRepository.findAllByUserId(userId);

        Instant now = Instant.now();
        List<TopicMasteryView> topicViews = new ArrayList<>();
        Map<UUID, MasteryResult> masteryByTopic = new HashMap<>();
        for (Topic topic : topics) {
            MasteryResult result = masteryEngine.computeMastery(
                    practiceByTopic.getOrDefault(topic.getId(), List.of()),
                    interviewByTopic.getOrDefault(topic.getId(), List.of()),
                    topic.getDefaultDifficulty(),
                    now);
            masteryByTopic.put(topic.getId(), result);
            topicViews.add(new TopicMasteryView(
                    topic.getId(), topic.getName(), topic.getSlug(), topic.getCategory(),
                    result.masteryScore(), result.observationCount(), result.sufficientEvidence()));
        }

        List<CategoryProgress> categoryProgress = new ArrayList<>();
        for (Topic.Category category : Topic.Category.values()) {
            List<Topic> topicsInCategory = topics.stream().filter(t -> t.getCategory() == category).toList();
            if (topicsInCategory.isEmpty()) {
                continue;
            }
            Set<UUID> topicIdsInCategory = topicsInCategory.stream().map(Topic::getId).collect(Collectors.toSet());

            long tasksTotal = tasks.stream().filter(t -> topicIdsInCategory.contains(t.getTopicId())).count();
            long tasksCompleted = tasks.stream()
                    .filter(t -> topicIdsInCategory.contains(t.getTopicId()))
                    .filter(t -> t.getStatus() == PreparationTask.Status.completed)
                    .count();

            List<Double> confidentScores = topicsInCategory.stream()
                    .map(t -> masteryByTopic.get(t.getId()))
                    .filter(MasteryResult::sufficientEvidence)
                    .map(MasteryResult::masteryScore)
                    .toList();
            Double averageMastery = confidentScores.isEmpty()
                    ? null
                    : confidentScores.stream().mapToDouble(Double::doubleValue).average().orElse(0);

            categoryProgress.add(
                    new CategoryProgress(category, topicsInCategory.size(), tasksTotal, tasksCompleted, averageMastery));
        }

        return new PreparationProgressResponse(categoryProgress, topicViews);
    }

    private Map<UUID, List<PracticeEvidence>> practiceEvidenceByTopic(UUID userId) {
        List<PracticeAttempt> attempts = attemptRepository.findAllByUserId(userId);
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

    /**
     * Evidence date is the interview question's own {@code createdAt}
     * (when the debrief captured it), not the interview's scheduled/
     * completed date — a deliberate simplification for this phase's scope,
     * since debrief capture (US-30) happens close in time to the interview
     * itself and avoids a second join against {@code interviews} purely to
     * read a timestamp.
     */
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
