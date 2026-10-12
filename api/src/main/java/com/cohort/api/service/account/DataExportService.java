package com.cohort.api.service.account;

import com.cohort.api.dto.UserProfileResponse;
import com.cohort.api.entity.Resume;
import com.cohort.api.entity.User;
import com.cohort.api.repository.ResumeRepository;
import com.cohort.api.repository.UserDataExportRepository;
import com.cohort.api.repository.UserRepository;
import com.cohort.api.service.resume.storage.FileStorageStrategy;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * US-07: everything the user has entered, as one JSON document -- profile,
 * every owned table (see {@link UserDataExportRepository}), and each uploaded
 * resume file base64-encoded under {@code resume_files}. Nothing here is
 * fetched by anything other than the caller's own ID.
 */
@Service
public class DataExportService {

    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final UserDataExportRepository exportRepository;
    private final FileStorageStrategy resumeStorage;

    public DataExportService(
            UserRepository userRepository,
            ResumeRepository resumeRepository,
            UserDataExportRepository exportRepository,
            FileStorageStrategy resumeStorage) {
        this.userRepository = userRepository;
        this.resumeRepository = resumeRepository;
        this.exportRepository = exportRepository;
        this.resumeStorage = resumeStorage;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> export(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("Authenticated user " + userId + " no longer exists"));

        Map<String, Object> document = new LinkedHashMap<>();
        document.put("exported_at", Instant.now());
        document.put("profile", UserProfileResponse.from(user));
        Map<String, List<JsonNode>> tables = exportRepository.findAllOwnedBy(userId);
        tables.forEach(document::put);
        document.put("resume_files", resumeFiles(userId));
        return document;
    }

    private List<Map<String, Object>> resumeFiles(UUID userId) {
        return resumeRepository.findAllByUserId(userId).stream()
                .filter(Resume::hasFile)
                .map(resume -> {
                    Map<String, Object> file = new LinkedHashMap<>();
                    file.put("resume_id", resume.getId());
                    file.put("file_name", resume.getFileOriginalName());
                    file.put("content_type", resume.getFileMime());
                    file.put("content_base64", Base64.getEncoder().encodeToString(resumeStorage.load(resume.getFilePath())));
                    return file;
                })
                .toList();
    }
}
