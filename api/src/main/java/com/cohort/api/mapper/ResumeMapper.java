package com.cohort.api.mapper;

import com.cohort.api.dto.ResumeDetailResponse;
import com.cohort.api.dto.ResumeSummary;
import com.cohort.api.entity.Resume;
import org.springframework.stereotype.Component;

@Component
public class ResumeMapper {

    public ResumeSummary toSummary(Resume resume) {
        return new ResumeSummary(
                resume.getId(),
                resume.getName(),
                resume.getVersionLabel(),
                resume.getTargetRole(),
                resume.isDefault(),
                resume.hasFile(),
                resume.getParentResumeId(),
                resume.getCreatedAt(),
                resume.getUpdatedAt());
    }

    public ResumeDetailResponse toDetail(Resume resume) {
        return new ResumeDetailResponse(
                resume.getId(),
                resume.getName(),
                resume.getVersionLabel(),
                resume.getTargetRole(),
                resume.getSummary(),
                resume.isDefault(),
                resume.getParentResumeId(),
                resume.hasFile(),
                resume.getFileOriginalName(),
                resume.getFileSizeBytes(),
                resume.getFileMime(),
                resume.getFileUploadedAt(),
                resume.getCreatedAt(),
                resume.getUpdatedAt());
    }
}
