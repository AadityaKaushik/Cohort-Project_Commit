package com.cohort.api.service;

import com.cohort.api.dto.CreateResumeRequest;
import com.cohort.api.dto.PagedResponse;
import com.cohort.api.dto.ResumeDetailResponse;
import com.cohort.api.dto.ResumeSummary;
import com.cohort.api.dto.UpdateResumeRequest;
import com.cohort.api.entity.Resume;
import com.cohort.api.exception.ResumeFileNotFoundException;
import com.cohort.api.exception.ResumeNotFoundException;
import com.cohort.api.mapper.ResumeMapper;
import com.cohort.api.repository.ResumeRepository;
import com.cohort.api.service.resume.storage.FileStorageStrategy;
import com.cohort.api.service.resume.storage.FileUpload;
import com.cohort.api.service.resume.storage.StoredFile;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-20..22: resume CRUD plus the file upload/download pair. File I/O goes
 * through {@link FileStorageStrategy}, injected as the interface — Spring
 * wires in {@code ValidatingFileStorage} (the {@code @Primary} bean, P19),
 * so every upload runs the size cap and the extension -&gt; MIME ->
 * magic-byte validation chain without this class knowing that chain exists.
 *
 * <p><strong>"Expiring, authenticated download" (FR-32):</strong> rather
 * than invent a second, parallel token scheme, the download endpoint is
 * secured by the exact same bearer JWT as every other endpoint
 * (SecurityConfig's {@code anyRequest().authenticated()}), and that JWT
 * already expires after {@code app.jwt.access-token-ttl-minutes} (15 min by
 * default, see JwtConfig/TokenService). That satisfies FR-32's "authenticated,
 * expiring" requirement directly and keeps this phase's diff to the
 * resume-specific logic instead of a bespoke signed-URL subsystem.
 */
@Service
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final FileStorageStrategy fileStorage;
    private final ResumeMapper mapper;

    public ResumeService(ResumeRepository resumeRepository, FileStorageStrategy fileStorage, ResumeMapper mapper) {
        this.resumeRepository = resumeRepository;
        this.fileStorage = fileStorage;
        this.mapper = mapper;
    }

    @Transactional
    public ResumeDetailResponse create(UUID userId, CreateResumeRequest request) {
        boolean wantsDefault = Boolean.TRUE.equals(request.isDefault());
        if (wantsDefault) {
            resumeRepository.clearDefaultForUser(userId);
        }

        Resume resume = new Resume(userId, request.name());
        resume.setVersionLabel(request.versionLabel());
        resume.setTargetRole(request.targetRole());
        resume.setSummary(request.summary());
        resume.setDefault(wantsDefault);

        resume = resumeRepository.save(resume);
        return mapper.toDetail(resume);
    }

    @Transactional(readOnly = true)
    public PagedResponse<ResumeSummary> list(UUID userId, int page, int size, String sort, String direction) {
        Pageable pageable = PageRequest.of(page, size, resolveSort(sort, direction));
        Page<Resume> result = resumeRepository.findAllByUserId(userId, pageable);
        List<ResumeSummary> items = result.getContent().stream().map(mapper::toSummary).toList();
        return new PagedResponse<>(items, result.getTotalElements(), page, size);
    }

    @Transactional(readOnly = true)
    public ResumeDetailResponse getById(UUID userId, UUID resumeId) {
        return mapper.toDetail(requireOwned(userId, resumeId));
    }

    @Transactional
    public ResumeDetailResponse update(UUID userId, UUID resumeId, UpdateResumeRequest request) {
        if (Boolean.TRUE.equals(request.isDefault())) {
            resumeRepository.clearDefaultForUser(userId);
        }

        Resume resume = requireOwned(userId, resumeId);
        if (request.name() != null) {
            resume.setName(request.name());
        }
        if (request.versionLabel() != null) {
            resume.setVersionLabel(request.versionLabel());
        }
        if (request.targetRole() != null) {
            resume.setTargetRole(request.targetRole());
        }
        if (request.summary() != null) {
            resume.setSummary(request.summary());
        }
        if (request.isDefault() != null) {
            resume.setDefault(request.isDefault());
        }

        return mapper.toDetail(resume);
    }

    @Transactional
    public void delete(UUID userId, UUID resumeId) {
        Resume resume = requireOwned(userId, resumeId);
        if (resume.hasFile()) {
            fileStorage.delete(resume.getFilePath());
        }
        resumeRepository.deleteByIdAndUserId(resumeId, userId);
    }

    @Transactional
    public ResumeDetailResponse uploadFile(UUID userId, UUID resumeId, String originalFilename, String contentType, byte[] content) {
        Resume resume = requireOwned(userId, resumeId);
        String previousFilePath = resume.getFilePath();

        FileUpload upload = new FileUpload(originalFilename, contentType, content);
        StoredFile stored = fileStorage.store(userId, upload);
        resume.applyStoredFile(stored.storageKey(), stored.originalFilename(), stored.contentType(), stored.sizeBytes());

        // Replacing a file: delete the old blob only after the new one is
        // safely stored, so a mid-upload failure never leaves the resume
        // pointing at nothing.
        if (previousFilePath != null) {
            fileStorage.delete(previousFilePath);
        }

        return mapper.toDetail(resume);
    }

    @Transactional(readOnly = true)
    public DownloadableFile downloadFile(UUID userId, UUID resumeId) {
        Resume resume = requireOwned(userId, resumeId);
        if (!resume.hasFile()) {
            throw new ResumeFileNotFoundException();
        }
        byte[] content = fileStorage.load(resume.getFilePath());
        return new DownloadableFile(content, resume.getFileOriginalName(), resume.getFileMime());
    }

    private Resume requireOwned(UUID userId, UUID resumeId) {
        return resumeRepository.findByIdAndUserId(resumeId, userId).orElseThrow(ResumeNotFoundException::new);
    }

    private Sort resolveSort(String sort, String direction) {
        boolean descending = !"asc".equalsIgnoreCase(direction);
        Sort.Direction springDirection = descending ? Sort.Direction.DESC : Sort.Direction.ASC;

        return switch (sort == null ? "" : sort) {
            case "name" -> Sort.by(springDirection, "name");
            default -> Sort.by(springDirection, "createdAt");
        };
    }

    /** What the controller needs to stream a stored file back with the right headers. */
    public record DownloadableFile(byte[] content, String originalFilename, String contentType) {
    }
}
