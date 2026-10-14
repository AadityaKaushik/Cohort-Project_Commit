package com.cohort.api.service.application.imports;

import com.cohort.api.dto.ApplicationImportResponse;
import com.cohort.api.dto.CreateApplicationRequest;
import com.cohort.api.entity.Application;
import com.cohort.api.entity.Company;
import com.cohort.api.exception.FileTooLargeException;
import com.cohort.api.repository.ApplicationRepository;
import com.cohort.api.repository.CompanyRepository;
import com.cohort.api.service.ApplicationService;
import com.cohort.api.service.application.imports.ApplicationCsvParser.Candidate;
import com.cohort.api.service.application.imports.ApplicationCsvParser.Parsed;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ApplicationImportService {
    public static final int MAX_BYTES = 1024 * 1024;

    private final ApplicationService applicationService;
    private final ApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository;

    public ApplicationImportService(
            ApplicationService applicationService,
            ApplicationRepository applicationRepository,
            CompanyRepository companyRepository) {
        this.applicationService = applicationService;
        this.applicationRepository = applicationRepository;
        this.companyRepository = companyRepository;
    }

    @Transactional
    public ApplicationImportResponse importCsv(UUID userId, byte[] content, boolean dryRun) {
        if (content.length > MAX_BYTES) {
            throw new FileTooLargeException("The CSV file exceeds the 1 MB limit.");
        }
        Parsed parsed = ApplicationCsvParser.parse(content);
        LocalDate today = LocalDate.now();
        Set<String> seen = existingKeys(userId);

        List<ApplicationImportResponse.Row> rows = new ArrayList<>();
        int importable = 0;
        int duplicates = 0;
        int invalid = 0;
        int created = 0;

        for (ApplicationCsvParser.RawRow raw : parsed.rows()) {
            Candidate candidate = ApplicationCsvParser.interpret(raw, today);
            List<String> warnings = new ArrayList<>(candidate.warnings());
            String outcome;
            if (!candidate.valid()) {
                warnings.add(candidate.error());
                outcome = "invalid";
                invalid++;
            } else if (!seen.add(key(candidate.company(), candidate.role(), candidate.dateApplied()))) {
                outcome = "duplicate";
                duplicates++;
            } else {
                outcome = "import";
                importable++;
                if (!dryRun) {
                    applicationService.create(userId, toRequest(candidate));
                    created++;
                }
            }
            rows.add(new ApplicationImportResponse.Row(
                    candidate.line(), candidate.company(), candidate.role(), candidate.status(),
                    candidate.dateApplied(), candidate.location(), candidate.url(), outcome, warnings));
        }

        List<ApplicationImportResponse.Column> columns = parsed.columns().stream()
                .map(column -> new ApplicationImportResponse.Column(column.header(), column.field()))
                .toList();
        return new ApplicationImportResponse(dryRun, rows.size(), importable, duplicates, invalid, created, columns, rows);
    }

    private Set<String> existingKeys(UUID userId) {
        Map<UUID, String> companyNames = new HashMap<>();
        for (Company company : companyRepository.findAllByUserId(userId)) {
            companyNames.put(company.getId(), company.getName());
        }
        Set<String> keys = new HashSet<>();
        for (Application application : applicationRepository.findAllByUserId(userId)) {
            String company = companyNames.get(application.getCompanyId());
            if (company != null) {
                keys.add(key(company, application.getJobTitle(), application.getDateApplied()));
            }
        }
        return keys;
    }

    private static String key(String company, String role, LocalDate date) {
        return company.trim().toLowerCase(Locale.ROOT) + "|" + role.trim().toLowerCase(Locale.ROOT) + "|" + date;
    }

    private static CreateApplicationRequest toRequest(Candidate candidate) {
        return new CreateApplicationRequest(
                candidate.company(), candidate.role(), null, candidate.url(), candidate.location(),
                null, null, null, null, null, candidate.dateApplied(), candidate.status(),
                null, null, null, null, null, null, candidate.notes(), null);
    }
}
