package com.cohort.api.service.fl;

import com.cohort.api.entity.User;
import com.cohort.api.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * US-58: the scheduled job that pushes each opted-in user's sanitised
 * feature vector into fl-service's own store. Re-reads {@code
 * fl_opt_in} fresh on every run rather than caching who was opted in last
 * time, which is what makes withdrawal (US-64: "takes effect from the
 * next round") actually true — an opted-out user is simply absent from
 * {@link UserRepository#findAllByFlOptInTrue()} on the very next run.
 *
 * <p>One user's export failing (fl-service briefly unreachable, a
 * malformed record) never aborts the batch — it is logged and skipped, so
 * the rest of the cohort's exports still land. This job is deliberately
 * "best effort background sync," not a fought-for-strong-guarantee
 * operation; the circuit breaker (P53) is what protects the
 * user-facing Recommendations path, not this job.
 */
@Component
public class FlFeatureExportJob {

    private static final Logger log = LoggerFactory.getLogger(FlFeatureExportJob.class);

    private final UserRepository userRepository;
    private final FlFeatureExtractionService extractionService;
    private final FlExportClient exportClient;

    public FlFeatureExportJob(UserRepository userRepository, FlFeatureExtractionService extractionService, FlExportClient exportClient) {
        this.userRepository = userRepository;
        this.extractionService = extractionService;
        this.exportClient = exportClient;
    }

    @Scheduled(cron = "${app.fl.export.cron}")
    public void exportOnSchedule() {
        runExport();
    }

    /**
     * The actual work, as a plain method separate from the {@code @Scheduled}
     * wrapper above so a test (or a future admin-triggered endpoint) can call
     * it directly without waiting on a cron trigger.
     */
    public ExportSummary runExport() {
        Instant asOf = Instant.now();
        List<User> optedIn = userRepository.findAllByFlOptInTrue();
        int succeeded = 0;
        int failed = 0;
        for (User user : optedIn) {
            try {
                FlFeatureVector vector = extractionService.extract(user.getId(), asOf);
                exportClient.export(user.getId(), vector);
                succeeded++;
            } catch (RuntimeException ex) {
                failed++;
                log.warn("FL feature export failed for user {}: {}", user.getId(), ex.getMessage());
            }
        }
        return new ExportSummary(optedIn.size(), succeeded, failed);
    }

    public record ExportSummary(int eligible, int succeeded, int failed) {
    }
}
