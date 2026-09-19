package com.cohort.api.service;

import com.cohort.api.dto.ApplicationDetailResponse;
import com.cohort.api.dto.ApplicationSearchCriteria;
import com.cohort.api.dto.ApplicationStatusHistoryResponse;
import com.cohort.api.dto.ApplicationSummary;
import com.cohort.api.dto.ChangeStatusRequest;
import com.cohort.api.dto.CreateApplicationRequest;
import com.cohort.api.dto.PagedResponse;
import com.cohort.api.dto.UpdateApplicationRequest;
import com.cohort.api.entity.Application;
import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.entity.ApplicationStatusHistory;
import com.cohort.api.entity.Company;
import com.cohort.api.entity.Interview;
import com.cohort.api.exception.ApplicationNotFoundException;
import com.cohort.api.exception.InvalidStatusTransitionException;
import com.cohort.api.exception.ResumeNotFoundException;
import com.cohort.api.mapper.ApplicationMapper;
import com.cohort.api.repository.ApplicationRepository;
import com.cohort.api.repository.ApplicationStatusHistoryRepository;
import com.cohort.api.repository.CompanyRepository;
import com.cohort.api.repository.InterviewRepository;
import com.cohort.api.repository.ResumeRepository;
import com.cohort.api.repository.specification.ApplicationSpecifications;
import com.cohort.api.service.application.state.ApplicationStatusStateRegistry;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final ApplicationStatusStateRegistry stateRegistry;
    private final ResumeRepository resumeRepository;
    private final InterviewRepository interviewRepository;
    private final ApplicationMapper mapper;

    public ApplicationService(
            ApplicationRepository applicationRepository,
            CompanyRepository companyRepository,
            ApplicationStatusHistoryRepository historyRepository,
            ApplicationStatusStateRegistry stateRegistry,
            ResumeRepository resumeRepository,
            InterviewRepository interviewRepository,
            ApplicationMapper mapper) {
        this.applicationRepository = applicationRepository;
        this.companyRepository = companyRepository;
        this.historyRepository = historyRepository;
        this.stateRegistry = stateRegistry;
        this.resumeRepository = resumeRepository;
        this.interviewRepository = interviewRepository;
        this.mapper = mapper;
    }

    @Transactional
    public ApplicationDetailResponse create(UUID userId, CreateApplicationRequest request) {
        Company company = findOrCreateCompany(userId, request.companyName());

        Application application =
                new Application(userId, company.getId(), request.jobTitle(), request.dateApplied(), request.status());
        applyOptionalFields(application, request);
        application = applicationRepository.save(application);

        ApplicationStatusHistory createdEntry =
                historyRepository.save(new ApplicationStatusHistory(application.getId(), null, application.getStatus(), null));

        return mapper.toDetail(application, company, List.of(createdEntry), List.of());
    }

    @Transactional(readOnly = true)
    public PagedResponse<ApplicationSummary> list(
            UUID userId, ApplicationSearchCriteria criteria, int page, int size, String sort, String direction) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sort, direction));
        Specification<Application> spec = ApplicationSpecifications.combine(userId, criteria);
        Page<Application> result = applicationRepository.findAll(spec, pageable);
        Map<UUID, Company> companiesById = loadCompanies(userId, result.getContent());

        List<ApplicationSummary> items = result.getContent().stream()
                .map(application -> mapper.toSummary(application, companiesById.get(application.getCompanyId())))
                .toList();

        return new PagedResponse<>(items, result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public ApplicationDetailResponse getById(UUID userId, UUID applicationId) {
        Application application = requireOwned(userId, applicationId);
        Company company = requireCompany(userId, application.getCompanyId());
        List<ApplicationStatusHistory> history = historyRepository.findByApplicationIdOrderByChangedAtDesc(applicationId);
        List<Interview> interviews = interviewRepository.findAllByUserIdAndApplicationIdOrderByRoundNumberAsc(userId, applicationId);
        return mapper.toDetail(application, company, history, interviews);
    }

    @Transactional
    public ApplicationDetailResponse update(UUID userId, UUID applicationId, UpdateApplicationRequest request) {
        Application application = requireOwned(userId, applicationId);

        if (request.companyName() != null) {
            application.setCompanyId(findOrCreateCompany(userId, request.companyName()).getId());
        }
        if (request.jobTitle() != null) {
            application.setJobTitle(request.jobTitle());
        }
        if (request.jobDescription() != null) {
            application.setJobDescription(request.jobDescription());
        }
        if (request.jobUrl() != null) {
            application.setJobUrl(request.jobUrl());
        }
        if (request.location() != null) {
            application.setLocation(request.location());
        }
        if (request.workMode() != null) {
            application.setWorkMode(request.workMode());
        }
        if (request.jobType() != null) {
            application.setJobType(request.jobType());
        }
        if (request.salaryMin() != null) {
            application.setSalaryMin(request.salaryMin());
        }
        if (request.salaryMax() != null) {
            application.setSalaryMax(request.salaryMax());
        }
        if (request.salaryCurrency() != null) {
            application.setSalaryCurrency(request.salaryCurrency());
        }
        if (request.dateApplied() != null) {
            application.setDateApplied(request.dateApplied());
        }
        if (request.priority() != null) {
            application.setPriority(request.priority());
        }
        if (request.source() != null) {
            application.setSource(request.source());
        }
        if (request.isReferral() != null) {
            application.setReferral(request.isReferral());
        }
        if (request.deadline() != null) {
            application.setDeadline(request.deadline());
        }
        if (request.recruiterName() != null) {
            application.setRecruiterName(request.recruiterName());
        }
        if (request.recruiterEmail() != null) {
            application.setRecruiterEmail(request.recruiterEmail());
        }
        if (request.notes() != null) {
            application.setNotes(request.notes());
        }
        if (request.coverLetter() != null) {
            application.setCoverLetter(request.coverLetter());
        }

        Company company = requireCompany(userId, application.getCompanyId());
        List<ApplicationStatusHistory> history = historyRepository.findByApplicationIdOrderByChangedAtDesc(applicationId);
        List<Interview> interviews = interviewRepository.findAllByUserIdAndApplicationIdOrderByRoundNumberAsc(userId, applicationId);
        return mapper.toDetail(application, company, history, interviews);
    }

    @Transactional
    public void delete(UUID userId, UUID applicationId) {
        requireOwned(userId, applicationId);
        applicationRepository.deleteByIdAndUserId(applicationId, userId);
    }

    @Transactional
    public ApplicationDetailResponse changeStatus(UUID userId, UUID applicationId, ChangeStatusRequest request) {
        Application application = requireOwned(userId, applicationId);
        ApplicationStatus from = application.getStatus();
        ApplicationStatus to = request.toStatus();
        boolean hasCorrectionNote = request.note() != null && !request.note().isBlank();

        if (!stateRegistry.canTransition(from, to, hasCorrectionNote)) {
            throw new InvalidStatusTransitionException(from, to);
        }

        application.applyStatusChange(to);
        historyRepository.save(new ApplicationStatusHistory(applicationId, from, to, request.note()));

        Company company = requireCompany(userId, application.getCompanyId());
        List<ApplicationStatusHistory> history = historyRepository.findByApplicationIdOrderByChangedAtDesc(applicationId);
        List<Interview> interviews = interviewRepository.findAllByUserIdAndApplicationIdOrderByRoundNumberAsc(userId, applicationId);
        return mapper.toDetail(application, company, history, interviews);
    }

    /**
     * US-24/FR-34: sets, changes, or (given {@code resumeId == null}) clears
     * the one resume associated with this application. A foreign or missing
     * {@code resumeId} is rejected with the same {@code ResumeNotFoundException}
     * a direct resume lookup would throw (FR-95: 404, never 403, and never a
     * signal that lets one user probe whether a given resume ID belongs to
     * someone else). {@code updated_at} bumps automatically via
     * {@code @UpdateTimestamp} once the managed entity's field changes.
     */
    @Transactional
    public ApplicationDetailResponse attachResume(UUID userId, UUID applicationId, UUID resumeId) {
        Application application = requireOwned(userId, applicationId);
        if (resumeId != null) {
            resumeRepository.findByIdAndUserId(resumeId, userId).orElseThrow(ResumeNotFoundException::new);
        }
        application.setResumeId(resumeId);

        Company company = requireCompany(userId, application.getCompanyId());
        List<ApplicationStatusHistory> history = historyRepository.findByApplicationIdOrderByChangedAtDesc(applicationId);
        List<Interview> interviews = interviewRepository.findAllByUserIdAndApplicationIdOrderByRoundNumberAsc(userId, applicationId);
        return mapper.toDetail(application, company, history, interviews);
    }

    @Transactional(readOnly = true)
    public List<ApplicationStatusHistoryResponse> history(UUID userId, UUID applicationId) {
        requireOwned(userId, applicationId);
        return historyRepository.findByApplicationIdOrderByChangedAtDesc(applicationId).stream()
                .map(ApplicationStatusHistoryResponse::from)
                .toList();
    }

    private Application requireOwned(UUID userId, UUID applicationId) {
        return applicationRepository
                .findByIdAndUserId(applicationId, userId)
                .orElseThrow(ApplicationNotFoundException::new);
    }

    private Company requireCompany(UUID userId, UUID companyId) {
        return companyRepository
                .findByIdAndUserId(companyId, userId)
                .orElseThrow(() -> new IllegalStateException("Application references a company it does not own"));
    }

    private Company findOrCreateCompany(UUID userId, String companyName) {
        String trimmedName = companyName.trim();
        String normalizedName = trimmedName.toLowerCase();

        return companyRepository.findByUserIdAndNormalizedName(userId, normalizedName).orElseGet(() -> {
            try {
                return companyRepository.save(new Company(trimmedName, normalizedName, userId));
            } catch (DataIntegrityViolationException raceOnUniqueName) {
                return companyRepository.findByUserIdAndNormalizedName(userId, normalizedName)
                        .orElseThrow(() -> raceOnUniqueName);
            }
        });
    }

    private Map<UUID, Company> loadCompanies(UUID userId, List<Application> applications) {
        Map<UUID, Company> companiesById = new HashMap<>();
        for (Application application : applications) {
            companiesById.computeIfAbsent(application.getCompanyId(), id -> requireCompany(userId, id));
        }
        return companiesById;
    }

    /**
     * A small, explicit allow-list rather than accepting an arbitrary Spring
     * {@code Sort} expression from the client — every accepted value maps to
     * exactly one real entity property. "days_in_stage" reuses
     * {@code stageEnteredAt} with the direction inverted, since more days in
     * stage means an older {@code stageEnteredAt}; there is no stored
     * days-in-stage column to sort by directly.
     */
    private Sort resolveSort(String sort, String direction) {
        boolean descending = !"asc".equalsIgnoreCase(direction);
        Sort.Direction springDirection = descending ? Sort.Direction.DESC : Sort.Direction.ASC;

        return switch (sort == null ? "" : sort) {
            case "status" -> Sort.by(springDirection, "status");
            case "priority" -> Sort.by(springDirection, "priority");
            case "days_in_stage" -> Sort.by(descending ? Sort.Direction.ASC : Sort.Direction.DESC, "stageEnteredAt");
            default -> Sort.by(springDirection, "dateApplied");
        };
    }

    private void applyOptionalFields(Application application, CreateApplicationRequest request) {
        application.setJobDescription(request.jobDescription());
        application.setJobUrl(request.jobUrl());
        application.setLocation(request.location());
        application.setWorkMode(request.workMode());
        application.setJobType(request.jobType());
        application.setSalaryMin(request.salaryMin());
        application.setSalaryMax(request.salaryMax());
        application.setSalaryCurrency(request.salaryCurrency());
        application.setPriority(request.priority());
        application.setSource(request.source());
        if (request.isReferral() != null) {
            application.setReferral(request.isReferral());
        }
        application.setDeadline(request.deadline());
        application.setRecruiterName(request.recruiterName());
        application.setRecruiterEmail(request.recruiterEmail());
        application.setNotes(request.notes());
        application.setCoverLetter(request.coverLetter());
    }
}
