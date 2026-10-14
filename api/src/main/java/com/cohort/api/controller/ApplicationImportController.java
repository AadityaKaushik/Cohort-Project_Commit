package com.cohort.api.controller;

import com.cohort.api.dto.ApplicationImportResponse;
import com.cohort.api.exception.InvalidImportException;
import com.cohort.api.service.application.imports.ApplicationImportService;
import java.io.IOException;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/applications/import")
public class ApplicationImportController {
    private final ApplicationImportService importService;

    public ApplicationImportController(ApplicationImportService importService) {
        this.importService = importService;
    }

    @PostMapping(consumes = "multipart/form-data")
    public ApplicationImportResponse importCsv(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam("file") MultipartFile file,
            @RequestParam(defaultValue = "false") boolean dryRun) {
        try {
            return importService.importCsv(UUID.fromString(jwt.getSubject()), file.getBytes(), dryRun);
        } catch (IOException unreadable) {
            throw new InvalidImportException("The file could not be read.");
        }
    }
}
