package com.cohort.api.controller;

import com.cohort.api.dto.CreateReportExportRequest;
import com.cohort.api.dto.ReportExportResponse;
import com.cohort.api.service.report.ReportService;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * US-80 / FR-102..104. Thin by design (ARCHITECTURE_REVISION_JAVA.md §4.2):
 * every method delegates straight to ReportService. The download mirrors
 * {@code ResumeController#downloadFile}: bearer-JWT authenticated, owner-only,
 * served as an attachment -- plus {@code Cache-Control: no-store}, since a
 * report is a bundle of personal data no shared cache should keep.
 */
@RestController
@RequestMapping("/api/v1/reports")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReportExportResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateReportExportRequest request) {
        return reportService.create(userId(jwt), request);
    }

    @GetMapping
    public List<ReportExportResponse> list(@AuthenticationPrincipal Jwt jwt) {
        return reportService.list(userId(jwt));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> download(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        ReportService.DownloadableReport report = reportService.download(userId(jwt), id);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(report.fileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(report.contentType()))
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(report.content());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        reportService.delete(userId(jwt), id);
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
