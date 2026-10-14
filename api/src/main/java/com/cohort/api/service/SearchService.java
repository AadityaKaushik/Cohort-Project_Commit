package com.cohort.api.service;

import com.cohort.api.dto.SearchResponse;
import com.cohort.api.dto.SearchResult;
import com.cohort.api.entity.Application;
import com.cohort.api.entity.Company;
import com.cohort.api.entity.CompanyNote;
import com.cohort.api.repository.ApplicationRepository;
import com.cohort.api.repository.CompanyNoteRepository;
import com.cohort.api.repository.CompanyRepository;
import com.cohort.api.repository.QuestionRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SearchService {

    static final int MIN_QUERY_LENGTH = 2;
    private static final int LIMIT = 5;
    private static final int SNIPPET_LENGTH = 80;

    private final ApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository;
    private final CompanyNoteRepository noteRepository;
    private final QuestionRepository questionRepository;

    public SearchService(
            ApplicationRepository applicationRepository,
            CompanyRepository companyRepository,
            CompanyNoteRepository noteRepository,
            QuestionRepository questionRepository) {
        this.applicationRepository = applicationRepository;
        this.companyRepository = companyRepository;
        this.noteRepository = noteRepository;
        this.questionRepository = questionRepository;
    }

    @Transactional(readOnly = true)
    public SearchResponse search(UUID userId, String query) {
        String needle = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (needle.length() < MIN_QUERY_LENGTH) {
            return new SearchResponse(List.of(), List.of(), List.of(), List.of());
        }
        List<Company> companies = companyRepository.findAllByUserId(userId);
        Map<UUID, String> companyNames = companies.stream().collect(Collectors.toMap(Company::getId, Company::getName));

        List<SearchResult> applications = applicationRepository.findAllByUserId(userId).stream()
                .filter(application -> contains(application.getJobTitle(), needle)
                        || contains(companyNames.get(application.getCompanyId()), needle))
                .limit(LIMIT)
                .map(application -> new SearchResult(application.getId(), application.getJobTitle(),
                        companyNames.get(application.getCompanyId()), null))
                .toList();

        List<SearchResult> matchingCompanies = companies.stream()
                .filter(company -> contains(company.getName(), needle))
                .limit(LIMIT)
                .map(company -> new SearchResult(company.getId(), company.getName(), company.getIndustry(), null))
                .toList();

        List<SearchResult> notes = noteRepository.findAllByUserId(userId).stream()
                .filter(note -> contains(note.getBody(), needle))
                .limit(LIMIT)
                .map(note -> new SearchResult(note.getId(), snippet(note), companyNames.get(note.getCompanyId()),
                        note.getCompanyId()))
                .toList();

        List<SearchResult> questions = questionRepository.search(userId, escape(needle), PageRequest.of(0, LIMIT)).stream()
                .map(question -> new SearchResult(question.getId(), question.getQuestionText(),
                        question.getCategory().name(), question.getTopicId()))
                .toList();

        return new SearchResponse(applications, matchingCompanies, questions, notes);
    }

    private static boolean contains(String text, String needle) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(needle);
    }

    private static String snippet(CompanyNote note) {
        String body = note.getBody();
        return body.length() <= SNIPPET_LENGTH ? body : body.substring(0, SNIPPET_LENGTH) + "…";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
