package com.cohort.api.service;

import com.cohort.api.dto.CreatePracticeAttemptRequest;
import com.cohort.api.dto.CreateQuestionRequest;
import com.cohort.api.dto.PagedResponse;
import com.cohort.api.dto.PracticeAttemptResponse;
import com.cohort.api.dto.QuestionResponse;
import com.cohort.api.dto.TopicGraphResponse;
import com.cohort.api.dto.TopicResponse;
import com.cohort.api.dto.UpdateQuestionRequest;
import com.cohort.api.entity.Difficulty;
import com.cohort.api.entity.PracticeAttempt;
import com.cohort.api.entity.Question;
import com.cohort.api.entity.Topic;
import com.cohort.api.exception.QuestionNotFoundException;
import com.cohort.api.exception.TopicNotFoundException;
import com.cohort.api.mapper.PreparationMapper;
import com.cohort.api.repository.PracticeAttemptRepository;
import com.cohort.api.repository.QuestionRepository;
import com.cohort.api.repository.TopicRepository;
import com.cohort.api.repository.specification.QuestionSpecifications;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-35..38: the question bank (topics + questions, mixed shared/owned
 * data) and practice attempts (strictly user-owned). Topics are read-only
 * system data — there is no create/update/delete path for them here, only
 * for questions and attempts (FR-94/95 apply to the latter two).
 */
@Service
public class PreparationService {

    private final TopicRepository topicRepository;
    private final QuestionRepository questionRepository;
    private final PracticeAttemptRepository attemptRepository;
    private final PreparationMapper mapper;

    public PreparationService(
            TopicRepository topicRepository,
            QuestionRepository questionRepository,
            PracticeAttemptRepository attemptRepository,
            PreparationMapper mapper) {
        this.topicRepository = topicRepository;
        this.questionRepository = questionRepository;
        this.attemptRepository = attemptRepository;
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
}
