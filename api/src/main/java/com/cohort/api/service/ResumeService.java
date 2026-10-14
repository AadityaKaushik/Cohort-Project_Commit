package com.cohort.api.service;

import com.cohort.api.dto.CreateResumeRequest;
import com.cohort.api.dto.PagedResponse;
import com.cohort.api.dto.ResumeDetailResponse;
import com.cohort.api.dto.ResumeSummary;
import com.cohort.api.dto.UpdateResumeRequest;
import com.cohort.api.entity.Resume;
import com.cohort.api.entity.ResumeSection;
import com.cohort.api.exception.ResumeFileNotFoundException;
import com.cohort.api.exception.ResumeNotFoundException;
import com.cohort.api.mapper.ResumeMapper;
import com.cohort.api.repository.ResumeRepository;
import com.cohort.api.repository.ResumeSectionRepository;
import com.cohort.api.service.resume.storage.FileStorageStrategy;
import com.cohort.api.service.resume.storage.FileUpload;
import com.cohort.api.service.resume.storage.StoredFile;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResumeService {
    private static final Pattern VERSION_LABEL_PATTERN = Pattern.compile("(?i)^v(\\d+)$");

    private final ResumeRepository resumeRepository;
    private final ResumeSectionRepository resumeSectionRepository;
    private final FileStorageStrategy fileStorage;
    private final ResumeMapper mapper;

    public ResumeService(
            ResumeRepository resumeRepository,
            ResumeSectionRepository resumeSectionRepository,
            FileStorageStrategy fileStorage,
            ResumeMapper mapper) {
        this.resumeRepository = resumeRepository;
        this.resumeSectionRepository = resumeSectionRepository;
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

        // Replacing a file: delete the old blob only after the new one is safely stored, so a mid-upload failure never leaves the resume pointing at nothing.
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

    @Transactional
    public ResumeDetailResponse clone(UUID userId, UUID resumeId) {
        Resume source = requireOwned(userId, resumeId);

        Resume clone = new Resume(userId, source.getName());
        clone.setVersionLabel(nextVersionLabel(source.getVersionLabel()));
        clone.setTargetRole(source.getTargetRole());
        clone.setSummary(source.getSummary());
        clone.setParentResumeId(source.getId());
        clone = resumeRepository.save(clone);

        List<ResumeSection> sourceSections = resumeSectionRepository.findByResumeIdOrderBySortOrder(source.getId());
        for (ResumeSection section : sourceSections) {
            ResumeSection copy = new ResumeSection(clone.getId(), section.getSectionType());
            copy.setTitle(section.getTitle());
            copy.setOrganisation(section.getOrganisation());
            copy.setStartDate(section.getStartDate());
            copy.setEndDate(section.getEndDate());
            copy.setDescription(section.getDescription());
            copy.setSortOrder(section.getSortOrder());
            copy.setMetadata(section.getMetadata());
            resumeSectionRepository.save(copy);
        }

        return mapper.toDetail(clone);
    }

    private String nextVersionLabel(String currentLabel) {
        if (currentLabel == null || currentLabel.isBlank()) {
            return "v2";
        }
        Matcher matcher = VERSION_LABEL_PATTERN.matcher(currentLabel.trim());
        if (matcher.matches()) {
            int next = Integer.parseInt(matcher.group(1)) + 1;
            return "v" + next;
        }
        return currentLabel + " copy";
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

    public record DownloadableFile(byte[] content, String originalFilename, String contentType) {
    }
}
