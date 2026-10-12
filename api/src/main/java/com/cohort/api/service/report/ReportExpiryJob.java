package com.cohort.api.service.report;

import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** FR-104: periodically deletes expired exports -- rows and files -- so an expired report stops existing, not just stops being downloadable. */
@Component
public class ReportExpiryJob {

    private static final Logger log = LoggerFactory.getLogger(ReportExpiryJob.class);

    private final ReportService reportService;

    public ReportExpiryJob(ReportService reportService) {
        this.reportService = reportService;
    }

    @Scheduled(initialDelayString = "${app.reports.cleanup-interval:PT1H}", fixedDelayString = "${app.reports.cleanup-interval:PT1H}")
    public void deleteExpiredOnSchedule() {
        int removed = reportService.deleteExpired(Instant.now());
        if (removed > 0) {
            log.info("Removed {} expired report export(s)", removed);
        }
    }
}
