package com.cohort.api.service.fl;

import com.cohort.api.entity.User;
import com.cohort.api.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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
