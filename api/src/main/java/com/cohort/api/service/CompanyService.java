package com.cohort.api.service;

import com.cohort.api.dto.ApplicationSummary;
import com.cohort.api.dto.CompanyDetailResponse;
import com.cohort.api.dto.CompanyListItem;
import com.cohort.api.dto.CompanyNoteRequest;
import com.cohort.api.dto.CompanyNoteResponse;
import com.cohort.api.dto.ContactResponse;
import com.cohort.api.dto.UpdateCompanyRequest;
import com.cohort.api.entity.Application;
import com.cohort.api.entity.ApplicationStatus;
import com.cohort.api.entity.Company;
import com.cohort.api.entity.CompanyNote;
import com.cohort.api.entity.Contact;
import com.cohort.api.entity.Interview;
import com.cohort.api.exception.CompanyNoteNotFoundException;
import com.cohort.api.exception.CompanyNotFoundException;
import com.cohort.api.mapper.ApplicationMapper;
import com.cohort.api.repository.ApplicationRepository;
import com.cohort.api.repository.CompanyNoteRepository;
import com.cohort.api.repository.CompanyRepository;
import com.cohort.api.repository.ContactRepository;
import com.cohort.api.repository.InterviewRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** US-96: the aggregated company page and its notes, strictly scoped to the owner. */
@Service
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final CompanyNoteRepository noteRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;
    private final ContactRepository contactRepository;
    private final ContactService contactService;
    private final ApplicationMapper applicationMapper;

    public CompanyService(
            CompanyRepository companyRepository,
            CompanyNoteRepository noteRepository,
            ApplicationRepository applicationRepository,
            InterviewRepository interviewRepository,
            ContactRepository contactRepository,
            ContactService contactService,
            ApplicationMapper applicationMapper) {
        this.companyRepository = companyRepository;
        this.noteRepository = noteRepository;
        this.applicationRepository = applicationRepository;
        this.interviewRepository = interviewRepository;
        this.contactRepository = contactRepository;
        this.contactService = contactService;
        this.applicationMapper = applicationMapper;
    }

    @Transactional(readOnly = true)
    public List<CompanyListItem> list(UUID userId, String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        Map<UUID, List<Application>> applicationsByCompany = applicationRepository.findAllByUserId(userId).stream()
                .collect(Collectors.groupingBy(Application::getCompanyId));
        Map<UUID, Long> contactCounts = contactRepository.findAllByUserId(userId).stream()
                .filter(contact -> contact.getCompanyId() != null)
                .collect(Collectors.groupingBy(Contact::getCompanyId, Collectors.counting()));
        return companyRepository.findAllByUserId(userId).stream()
                .filter(company -> needle.isEmpty() || company.getName().toLowerCase(Locale.ROOT).contains(needle))
                .map(company -> toListItem(company, applicationsByCompany.getOrDefault(company.getId(), List.of()),
                        contactCounts.getOrDefault(company.getId(), 0L).intValue()))
                .sorted(Comparator.comparing(CompanyListItem::name, String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    @Transactional(readOnly = true)
    public CompanyDetailResponse get(UUID userId, UUID companyId) {
        Company company = requireOwned(userId, companyId);
        return detail(userId, company);
    }

    @Transactional
    public CompanyDetailResponse update(UUID userId, UUID companyId, UpdateCompanyRequest request) {
        Company company = requireOwned(userId, companyId);
        if (request.website() != null) {
            company.setWebsite(request.website().isBlank() ? null : request.website().trim());
        }
        if (request.industry() != null) {
            company.setIndustry(request.industry().isBlank() ? null : request.industry().trim());
        }
        companyRepository.save(company);
        return detail(userId, company);
    }

    @Transactional
    public CompanyNoteResponse addNote(UUID userId, UUID companyId, CompanyNoteRequest request) {
        requireOwned(userId, companyId);
        return toResponse(noteRepository.save(new CompanyNote(userId, companyId, request.body().trim())));
    }

    @Transactional
    public CompanyNoteResponse updateNote(UUID userId, UUID companyId, UUID noteId, CompanyNoteRequest request) {
        CompanyNote note = requireNote(userId, companyId, noteId);
        note.setBody(request.body().trim());
        return toResponse(noteRepository.save(note));
    }

    @Transactional
    public void deleteNote(UUID userId, UUID companyId, UUID noteId) {
        requireNote(userId, companyId, noteId);
        noteRepository.deleteByIdAndUserId(noteId, userId);
    }

    private Company requireOwned(UUID userId, UUID companyId) {
        return companyRepository.findByIdAndUserId(companyId, userId).orElseThrow(CompanyNotFoundException::new);
    }

    private CompanyNote requireNote(UUID userId, UUID companyId, UUID noteId) {
        return noteRepository.findByIdAndUserIdAndCompanyId(noteId, userId, companyId)
                .orElseThrow(CompanyNoteNotFoundException::new);
    }

    private CompanyDetailResponse detail(UUID userId, Company company) {
        List<Application> applications = applicationRepository.findAllByUserIdAndCompanyId(userId, company.getId());
        Set<UUID> applicationIds = applications.stream().map(Application::getId).collect(Collectors.toSet());
        int interviews = (int) interviewRepository.findAllByUserId(userId).stream()
                .map(Interview::getApplicationId)
                .filter(applicationIds::contains)
                .count();
        int offers = (int) applications.stream()
                .filter(application -> application.getCurrentStage() == ApplicationStatus.OFFER)
                .count();
        List<ApplicationSummary> summaries = applications.stream()
                .sorted(Comparator.comparing(Application::getDateApplied).reversed())
                .map(application -> applicationMapper.toSummary(application, company))
                .toList();
        List<ContactResponse> contacts = contactService.list(userId, company.getId(), null);
        List<CompanyNoteResponse> notes = noteRepository
                .findAllByUserIdAndCompanyIdOrderByCreatedAtDesc(userId, company.getId()).stream()
                .map(this::toResponse)
                .toList();
        return new CompanyDetailResponse(
                company.getId(),
                company.getName(),
                company.getWebsite(),
                company.getIndustry(),
                new CompanyDetailResponse.Stats(
                        applications.size(), activeCount(applications), interviews, offers, lastActivity(applications)),
                summaries,
                contacts,
                notes);
    }

    private CompanyListItem toListItem(Company company, List<Application> applications, int contactCount) {
        return new CompanyListItem(
                company.getId(),
                company.getName(),
                company.getWebsite(),
                company.getIndustry(),
                applications.size(),
                activeCount(applications),
                contactCount,
                lastActivity(applications));
    }

    private int activeCount(List<Application> applications) {
        return (int) applications.stream()
                .filter(application -> !application.getStatus().isTerminal() && application.getArchivedAt() == null)
                .count();
    }

    private Instant lastActivity(List<Application> applications) {
        return applications.stream().map(Application::getUpdatedAt).max(Comparator.naturalOrder()).orElse(null);
    }

    private CompanyNoteResponse toResponse(CompanyNote note) {
        return new CompanyNoteResponse(note.getId(), note.getBody(), note.getCreatedAt(), note.getUpdatedAt());
    }
}
