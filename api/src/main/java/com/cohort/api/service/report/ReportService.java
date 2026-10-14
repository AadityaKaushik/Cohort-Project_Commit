package com.cohort.api.service.report;

import com.cohort.api.dto.CreateReportExportRequest;
import com.cohort.api.dto.ReportExportResponse;
import com.cohort.api.entity.Application;
import com.cohort.api.entity.Company;
import com.cohort.api.entity.Interview;
import com.cohort.api.entity.ReportExport;
import com.cohort.api.entity.User;
import com.cohort.api.exception.InvalidReportRequestException;
import com.cohort.api.exception.ReportExportExpiredException;
import com.cohort.api.exception.ReportExportNotFoundException;
import com.cohort.api.repository.ApplicationRepository;
import com.cohort.api.repository.CompanyRepository;
import com.cohort.api.repository.InterviewRepository;
import com.cohort.api.repository.ReportExportRepository;
import com.cohort.api.repository.UserRepository;
import com.cohort.api.service.analytics.ApplicationSnapshot;
import com.cohort.api.service.analytics.ApplicationSnapshotLoader;
import com.cohort.api.service.resume.storage.FileStorageStrategy;
import com.cohort.api.service.resume.storage.FileUpload;
import com.cohort.api.service.resume.storage.StoredFile;
import java.time.DateTimeException;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportService {
    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    private final ReportExportRepository reportExportRepository;
    private final ApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository;
    private final InterviewRepository interviewRepository;
    private final UserRepository userRepository;
    private final ApplicationSnapshotLoader snapshotLoader;
    private final FileStorageStrategy reportStorage;
    private final Map<ReportFormat, ReportRenderer> renderers;
    private final Duration ttl;
    private final int maxPerUser;

    public ReportService(
            ReportExportRepository reportExportRepository,
            ApplicationRepository applicationRepository,
            CompanyRepository companyRepository,
            InterviewRepository interviewRepository,
            UserRepository userRepository,
            ApplicationSnapshotLoader snapshotLoader,
            @Qualifier("reportFileStorage") FileStorageStrategy reportStorage,
            List<ReportRenderer> renderers,
            @Value("${app.reports.ttl:P7D}") Duration ttl,
            @Value("${app.reports.max-per-user:20}") int maxPerUser) {
        this.reportExportRepository = reportExportRepository;
        this.applicationRepository = applicationRepository;
        this.companyRepository = companyRepository;
        this.interviewRepository = interviewRepository;
        this.userRepository = userRepository;
        this.snapshotLoader = snapshotLoader;
        this.reportStorage = reportStorage;
        this.renderers = renderers.stream().collect(Collectors.toMap(ReportRenderer::format, Function.identity()));
        this.ttl = ttl;
        this.maxPerUser = maxPerUser;
    }

    @Transactional
    public ReportExportResponse create(UUID userId, CreateReportExportRequest request) {
        validate(request);
        Instant now = Instant.now();
        User owner = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user " + userId + " no longer exists"));
        ZoneId zone = zoneOf(owner);

        Report report = ReportBuilder.forOwner(owner.getDisplayName(), loadInputs(userId))
                .generatedAt(now, zone)
                .withDateRange(request.dateFrom(), request.dateTo())
                .withSectionsFor(request.reportType())
                .build();
        byte[] content = renderers.get(request.format()).render(report);

        String fileName = fileName(request, now.atZone(zone).toLocalDate());
        StoredFile stored = reportStorage.store(userId, new FileUpload(fileName, request.format().contentType(), content));
        ReportExport export = reportExportRepository.save(new ReportExport(
                userId, request.reportType(), request.format(), request.dateFrom(), request.dateTo(),
                stored.storageKey(), fileName, request.format().contentType(), content.length, now, now.plus(ttl)));

        pruneBeyondLimit(userId, now);
        return ReportExportResponse.from(export);
    }

    @Transactional(readOnly = true)
    public List<ReportExportResponse> list(UUID userId) {
        return reportExportRepository.findAllByUserIdAndExpiresAtAfterOrderByGeneratedAtDesc(userId, Instant.now()).stream()
                .map(ReportExportResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DownloadableReport download(UUID userId, UUID exportId) {
        ReportExport export = requireOwned(userId, exportId);
        if (export.isExpired(Instant.now())) {
            throw new ReportExportExpiredException();
        }
        return new DownloadableReport(reportStorage.load(export.getFilePath()), export.getFileName(), export.getContentType());
    }

    @Transactional
    public void delete(UUID userId, UUID exportId) {
        ReportExport export = requireOwned(userId, exportId);
        reportExportRepository.delete(export);
        reportStorage.delete(export.getFilePath());
    }

    @Transactional
    public int deleteExpired(Instant now) {
        List<ReportExport> expired = reportExportRepository.findAllByExpiresAtLessThanEqual(now);
        for (ReportExport export : expired) {
            reportExportRepository.delete(export);
            deleteFileQuietly(export);
        }
        return expired.size();
    }

    private void validate(CreateReportExportRequest request) {
        if (!request.reportType().supports(request.format())) {
            throw new InvalidReportRequestException("format",
                    "A progress report combines two tables, so it is only available as a PDF.");
        }
        if (request.dateFrom() != null && request.dateTo() != null && request.dateFrom().isAfter(request.dateTo())) {
            throw new InvalidReportRequestException("date_from", "The start date must be on or before the end date.");
        }
    }

    private List<ReportApplicationInput> loadInputs(UUID userId) {
        Map<UUID, ApplicationSnapshot> snapshots = snapshotLoader.load(userId).stream()
                .collect(Collectors.toMap(ApplicationSnapshot::applicationId, Function.identity()));
        Map<UUID, String> companyNames = companyRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(Company::getId, Company::getName));
        Map<UUID, Long> interviewCounts = interviewRepository.findAllByUserId(userId).stream()
                .collect(Collectors.groupingBy(Interview::getApplicationId, Collectors.counting()));

        return applicationRepository.findAllByUserId(userId).stream()
                .filter(application -> snapshots.containsKey(application.getId()))
                .map(application -> toInput(application, companyNames, interviewCounts, snapshots.get(application.getId())))
                .toList();
    }

    private ReportApplicationInput toInput(
            Application application, Map<UUID, String> companyNames, Map<UUID, Long> interviewCounts, ApplicationSnapshot snapshot) {
        return new ReportApplicationInput(
                companyNames.getOrDefault(application.getCompanyId(), "Unknown company"),
                application.getJobTitle(),
                application.getDateApplied(),
                application.getLocation(),
                application.getWorkMode() == null ? null : application.getWorkMode().name(),
                application.getPriority() == null ? null : application.getPriority().name(),
                application.getSource(),
                application.isReferral(),
                interviewCounts.getOrDefault(application.getId(), 0L).intValue(),
                snapshot);
    }

    private void pruneBeyondLimit(UUID userId, Instant now) {
        List<ReportExport> live = reportExportRepository.findAllByUserIdAndExpiresAtAfterOrderByGeneratedAtDesc(userId, now);
        for (ReportExport stale : live.subList(Math.min(maxPerUser, live.size()), live.size())) {
            reportExportRepository.delete(stale);
            deleteFileQuietly(stale);
        }
    }

    private void deleteFileQuietly(ReportExport export) {
        try {
            reportStorage.delete(export.getFilePath());
        } catch (RuntimeException ex) {
            log.warn("Could not delete report file for export {}: {}", export.getId(), ex.getMessage());
        }
    }

    private ReportExport requireOwned(UUID userId, UUID exportId) {
        return reportExportRepository.findByIdAndUserId(exportId, userId).orElseThrow(ReportExportNotFoundException::new);
    }

    private static ZoneId zoneOf(User user) {
        String timezone = user.getTimezone();
        if (timezone == null || timezone.isBlank()) {
            return ZoneOffset.UTC;
        }
        try {
            return ZoneId.of(timezone);
        } catch (DateTimeException invalid) {
            return ZoneOffset.UTC;
        }
    }

    static String fileName(CreateReportExportRequest request, LocalDate generatedOn) {
        String base = switch (request.reportType()) {
            case applications -> "cohort-applications";
            case analytics_summary -> "cohort-analytics-summary";
            case progress_report -> "cohort-progress-report";
        };
        String range;
        if (request.dateFrom() == null && request.dateTo() == null) {
            range = generatedOn.toString();
        } else {
            range = (request.dateFrom() == null ? "start" : request.dateFrom().toString())
                    + "-to-" + (request.dateTo() == null ? generatedOn.toString() : request.dateTo().toString());
        }
        return base + "-" + range + "." + request.format().extension();
    }

    public record DownloadableReport(byte[] content, String fileName, String contentType) {
    }
}
