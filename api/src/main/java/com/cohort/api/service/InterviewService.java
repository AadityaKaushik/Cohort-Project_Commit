package com.cohort.api.service;

import com.cohort.api.dto.CreateInterviewQuestionRequest;
import com.cohort.api.dto.CreateInterviewRequest;
import com.cohort.api.dto.InterviewDetailResponse;
import com.cohort.api.dto.InterviewFeedbackRequest;
import com.cohort.api.dto.InterviewFeedbackResponse;
import com.cohort.api.dto.InterviewQuestionResponse;
import com.cohort.api.dto.InterviewSummary;
import com.cohort.api.dto.PagedResponse;
import com.cohort.api.dto.UpdateInterviewQuestionRequest;
import com.cohort.api.dto.UpdateInterviewRequest;
import com.cohort.api.entity.Application;
import com.cohort.api.entity.Company;
import com.cohort.api.entity.Interview;
import com.cohort.api.entity.InterviewFeedback;
import com.cohort.api.entity.InterviewQuestion;
import com.cohort.api.entity.InterviewRoundType;
import com.cohort.api.exception.ApplicationNotFoundException;
import com.cohort.api.exception.DuplicateRoundNumberException;
import com.cohort.api.exception.InterviewFeedbackNotFoundException;
import com.cohort.api.exception.InterviewNotFoundException;
import com.cohort.api.exception.InterviewQuestionNotFoundException;
import com.cohort.api.mapper.InterviewMapper;
import com.cohort.api.repository.ApplicationRepository;
import com.cohort.api.repository.CompanyRepository;
import com.cohort.api.repository.InterviewFeedbackRepository;
import com.cohort.api.repository.InterviewQuestionRepository;
import com.cohort.api.repository.InterviewRepository;
import com.cohort.api.repository.specification.InterviewSpecifications;
import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InterviewService {
    private final InterviewRepository interviewRepository;
    private final InterviewQuestionRepository questionRepository;
    private final InterviewFeedbackRepository feedbackRepository;
    private final ApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository;
    private final InterviewMapper mapper;

    public InterviewService(
            InterviewRepository interviewRepository,
            InterviewQuestionRepository questionRepository,
            InterviewFeedbackRepository feedbackRepository,
            ApplicationRepository applicationRepository,
            CompanyRepository companyRepository,
            InterviewMapper mapper) {
        this.interviewRepository = interviewRepository;
        this.questionRepository = questionRepository;
        this.feedbackRepository = feedbackRepository;
        this.applicationRepository = applicationRepository;
        this.companyRepository = companyRepository;
        this.mapper = mapper;
    }

    @Transactional
    public InterviewDetailResponse create(UUID userId, CreateInterviewRequest request) {
        Application application = applicationRepository
                .findByIdAndUserId(request.applicationId(), userId)
                .orElseThrow(ApplicationNotFoundException::new);

        if (interviewRepository.existsByApplicationIdAndRoundNumber(application.getId(), request.roundNumber())) {
            throw new DuplicateRoundNumberException(request.roundNumber());
        }

        Interview interview = new Interview(application.getId(), userId, request.roundNumber(), request.roundType());
        interview.setScheduledAt(request.scheduledAt());
        interview.setCompletedAt(request.completedAt());
        interview.setDurationMinutes(request.durationMinutes());
        interview.setMode(request.mode());
        interview.setInterviewerName(request.interviewerName());
        interview.setInterviewerRole(request.interviewerRole());
        if (request.outcome() != null) {
            interview.setOutcome(request.outcome());
        }
        interview.setPerceivedDifficulty(request.perceivedDifficulty());
        interview.setNotes(request.notes());

        interview = interviewRepository.save(interview);

        Company company = requireCompany(userId, application.getCompanyId());
        return mapper.toDetail(interview, company, application.getJobTitle(), false);
    }

    @Transactional(readOnly = true)
    public PagedResponse<InterviewSummary> list(
            UUID userId,
            UUID applicationId,
            Boolean upcoming,
            InterviewRoundType roundType,
            int page,
            int size,
            String sort,
            String direction) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sort, direction));
        Specification<Interview> spec = InterviewSpecifications.combine(userId, applicationId, upcoming, roundType);
        Page<Interview> result = interviewRepository.findAll(spec, pageable);
        List<Interview> interviews = result.getContent();

        Map<UUID, Application> applicationsById = loadApplications(userId, interviews);
        Map<UUID, Company> companiesById = loadCompanies(userId, applicationsById.values());
        Set<UUID> interviewIdsWithFeedback = feedbackIdsFor(userId, interviews);

        List<InterviewSummary> items = interviews.stream()
                .map(interview -> {
                    Application application = applicationsById.get(interview.getApplicationId());
                    Company company = companiesById.get(application.getCompanyId());
                    boolean hasFeedback = interviewIdsWithFeedback.contains(interview.getId());
                    return mapper.toSummary(interview, company, application.getJobTitle(), hasFeedback);
                })
                .toList();

        return new PagedResponse<>(items, result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public InterviewDetailResponse getById(UUID userId, UUID interviewId) {
        Interview interview = requireOwned(userId, interviewId);
        Application application = requireApplication(userId, interview.getApplicationId());
        Company company = requireCompany(userId, application.getCompanyId());
        boolean hasFeedback = feedbackRepository.findByUserIdAndInterviewId(userId, interviewId).isPresent();
        return mapper.toDetail(interview, company, application.getJobTitle(), hasFeedback);
    }

    @Transactional
    public InterviewDetailResponse update(UUID userId, UUID interviewId, UpdateInterviewRequest request) {
        Interview interview = requireOwned(userId, interviewId);

        if (request.roundNumber() != null && request.roundNumber() != interview.getRoundNumber()) {
            if (interviewRepository.existsByApplicationIdAndRoundNumberAndIdNot(
                    interview.getApplicationId(), request.roundNumber(), interviewId)) {
                throw new DuplicateRoundNumberException(request.roundNumber());
            }
            interview.setRoundNumber(request.roundNumber());
        }
        if (request.scheduledAt() != null) {
            interview.setScheduledAt(request.scheduledAt());
        }
        if (request.completedAt() != null) {
            interview.setCompletedAt(request.completedAt());
        }
        if (request.roundType() != null) {
            interview.setRoundType(request.roundType());
        }
        if (request.durationMinutes() != null) {
            interview.setDurationMinutes(request.durationMinutes());
        }
        if (request.mode() != null) {
            interview.setMode(request.mode());
        }
        if (request.interviewerName() != null) {
            interview.setInterviewerName(request.interviewerName());
        }
        if (request.interviewerRole() != null) {
            interview.setInterviewerRole(request.interviewerRole());
        }
        if (request.outcome() != null) {
            interview.setOutcome(request.outcome());
        }
        if (request.perceivedDifficulty() != null) {
            interview.setPerceivedDifficulty(request.perceivedDifficulty());
        }
        if (request.notes() != null) {
            interview.setNotes(request.notes());
        }

        Application application = requireApplication(userId, interview.getApplicationId());
        Company company = requireCompany(userId, application.getCompanyId());
        boolean hasFeedback = feedbackRepository.findByUserIdAndInterviewId(userId, interviewId).isPresent();
        return mapper.toDetail(interview, company, application.getJobTitle(), hasFeedback);
    }

    @Transactional
    public void delete(UUID userId, UUID interviewId) {
        requireOwned(userId, interviewId);
        interviewRepository.deleteByIdAndUserId(interviewId, userId);
    }

    @Transactional(readOnly = true)
    public List<InterviewQuestionResponse> listQuestions(UUID userId, UUID interviewId) {
        requireOwned(userId, interviewId);
        return questionRepository.findAllByUserIdAndInterviewIdOrderByCreatedAtAsc(userId, interviewId).stream()
                .map(mapper::toQuestionResponse)
                .toList();
    }

    @Transactional
    public InterviewQuestionResponse addQuestion(UUID userId, UUID interviewId, CreateInterviewQuestionRequest request) {
        requireOwned(userId, interviewId);

        InterviewQuestion question = new InterviewQuestion(interviewId, userId, request.questionText(), request.category());
        question.setTopicId(request.topicId());
        question.setDifficulty(request.difficulty());
        question.setSelfRating(request.selfRating());
        question.setTimeTakenMinutes(request.timeTakenMinutes());
        question.setNotes(request.notes());

        question = questionRepository.save(question);
        return mapper.toQuestionResponse(question);
    }

    @Transactional
    public InterviewQuestionResponse updateQuestion(UUID userId, UUID questionId, UpdateInterviewQuestionRequest request) {
        InterviewQuestion question = requireOwnedQuestion(userId, questionId);

        if (request.questionText() != null) {
            question.setQuestionText(request.questionText());
        }
        if (request.topicId() != null) {
            question.setTopicId(request.topicId());
        }
        if (request.category() != null) {
            question.setCategory(request.category());
        }
        if (request.difficulty() != null) {
            question.setDifficulty(request.difficulty());
        }
        if (request.selfRating() != null) {
            question.setSelfRating(request.selfRating());
        }
        if (request.timeTakenMinutes() != null) {
            question.setTimeTakenMinutes(request.timeTakenMinutes());
        }
        if (request.notes() != null) {
            question.setNotes(request.notes());
        }

        return mapper.toQuestionResponse(question);
    }

    @Transactional
    public void deleteQuestion(UUID userId, UUID questionId) {
        requireOwnedQuestion(userId, questionId);
        questionRepository.deleteByIdAndUserId(questionId, userId);
    }

    @Transactional(readOnly = true)
    public InterviewFeedbackResponse getFeedback(UUID userId, UUID interviewId) {
        requireOwned(userId, interviewId);
        InterviewFeedback feedback = feedbackRepository
                .findByUserIdAndInterviewId(userId, interviewId)
                .orElseThrow(InterviewFeedbackNotFoundException::new);
        return mapper.toFeedbackResponse(feedback);
    }

    @Transactional
    public InterviewFeedbackResponse upsertFeedback(UUID userId, UUID interviewId, InterviewFeedbackRequest request) {
        requireOwned(userId, interviewId);

        InterviewFeedback feedback = feedbackRepository
                .findByUserIdAndInterviewId(userId, interviewId)
                .orElseGet(() -> new InterviewFeedback(interviewId, userId));

        feedback.setSelfRating(request.selfRating());
        feedback.setWhatWentWell(request.whatWentWell());
        feedback.setWhatWentPoorly(request.whatWentPoorly());
        feedback.setImprovementAreas(request.improvementAreas());
        feedback.setExternalFeedback(request.externalFeedback());
        feedback.setDraft(request.isDraft());
        if (!request.isDraft() && feedback.getSubmittedAt() == null) {
            feedback.setSubmittedAt(Instant.now());
        }

        feedback = feedbackRepository.save(feedback);
        return mapper.toFeedbackResponse(feedback);
    }

    private Interview requireOwned(UUID userId, UUID interviewId) {
        return interviewRepository.findByIdAndUserId(interviewId, userId).orElseThrow(InterviewNotFoundException::new);
    }

    private InterviewQuestion requireOwnedQuestion(UUID userId, UUID questionId) {
        return questionRepository.findByIdAndUserId(questionId, userId).orElseThrow(InterviewQuestionNotFoundException::new);
    }

    private Application requireApplication(UUID userId, UUID applicationId) {
        return applicationRepository
                .findByIdAndUserId(applicationId, userId)
                .orElseThrow(() -> new IllegalStateException("Interview references an application it does not own"));
    }

    private Company requireCompany(UUID userId, UUID companyId) {
        return companyRepository
                .findByIdAndUserId(companyId, userId)
                .orElseThrow(() -> new IllegalStateException("Application references a company it does not own"));
    }

    private Map<UUID, Application> loadApplications(UUID userId, List<Interview> interviews) {
        Map<UUID, Application> byId = new HashMap<>();
        for (Interview interview : interviews) {
            byId.computeIfAbsent(interview.getApplicationId(), id -> requireApplication(userId, id));
        }
        return byId;
    }

    private Map<UUID, Company> loadCompanies(UUID userId, Collection<Application> applications) {
        Map<UUID, Company> byId = new HashMap<>();
        for (Application application : applications) {
            byId.computeIfAbsent(application.getCompanyId(), id -> requireCompany(userId, id));
        }
        return byId;
    }

    private Set<UUID> feedbackIdsFor(UUID userId, List<Interview> interviews) {
        if (interviews.isEmpty()) {
            return Set.of();
        }
        List<UUID> interviewIds = interviews.stream().map(Interview::getId).toList();
        Set<UUID> result = new HashSet<>();
        for (InterviewFeedback feedback : feedbackRepository.findAllByUserIdAndInterviewIdIn(userId, interviewIds)) {
            result.add(feedback.getInterviewId());
        }
        return result;
    }

    private Sort resolveSort(String sort, String direction) {
        boolean descending = !"asc".equalsIgnoreCase(direction);
        Sort.Direction springDirection = descending ? Sort.Direction.DESC : Sort.Direction.ASC;

        return switch (sort == null ? "" : sort) {
            case "round_number" -> Sort.by(springDirection, "roundNumber");
            case "created_at" -> Sort.by(springDirection, "createdAt");
            default -> Sort.by(springDirection, "scheduledAt");
        };
    }
}
