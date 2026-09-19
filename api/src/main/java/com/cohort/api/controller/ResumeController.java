package com.cohort.api.controller;

import com.cohort.api.dto.CreateResumeRequest;
import com.cohort.api.dto.PagedResponse;
import com.cohort.api.dto.ResumeDetailResponse;
import com.cohort.api.dto.ResumeSummary;
import com.cohort.api.dto.UpdateResumeRequest;
import com.cohort.api.exception.FileStorageException;
import com.cohort.api.service.ResumeService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Thin by design (ARCHITECTURE_REVISION_JAVA.md §4.2): every method delegates straight to ResumeService. */
@RestController
@RequestMapping("/api/v1/resumes")
public class ResumeController {

    private final ResumeService resumeService;

    public ResumeController(ResumeService resumeService) {
        this.resumeService = resumeService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ResumeDetailResponse create(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CreateResumeRequest request) {
        return resumeService.create(userId(jwt), request);
    }

    @GetMapping
    public PagedResponse<ResumeSummary> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "created_at") String sort,
            @RequestParam(defaultValue = "desc") String direction) {
        return resumeService.list(userId(jwt), page, size, sort, direction);
    }

    @GetMapping("/{id}")
    public ResumeDetailResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return resumeService.getById(userId(jwt), id);
    }

    @PatchMapping("/{id}")
    public ResumeDetailResponse update(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody UpdateResumeRequest request) {
        return resumeService.update(userId(jwt), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        resumeService.delete(userId(jwt), id);
    }

    @PostMapping("/{id}/file")
    public ResumeDetailResponse uploadFile(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        try {
            return resumeService.uploadFile(
                    userId(jwt), id, file.getOriginalFilename(), file.getContentType(), file.getBytes());
        } catch (IOException e) {
            throw new FileStorageException("Failed to read the uploaded file.", e);
        }
    }

    @PostMapping("/{id}/clone")
    @ResponseStatus(HttpStatus.CREATED)
    public ResumeDetailResponse clone(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return resumeService.clone(userId(jwt), id);
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> downloadFile(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        ResumeService.DownloadableFile file = resumeService.downloadFile(userId(jwt), id);

        MediaType contentType;
        try {
            contentType = file.contentType() != null ? MediaType.parseMediaType(file.contentType()) : MediaType.APPLICATION_OCTET_STREAM;
        } catch (org.springframework.http.InvalidMediaTypeException e) {
            contentType = MediaType.APPLICATION_OCTET_STREAM;
        }

        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(file.originalFilename() == null ? "resume" : file.originalFilename(), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(file.content());
    }

    private UUID userId(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
