package com.cohort.api.service.ai;

import com.cohort.api.entity.Application;
import com.cohort.api.entity.Resume;
import com.cohort.api.entity.ResumeSection;
import com.cohort.api.repository.ResumeRepository;
import com.cohort.api.repository.ResumeSectionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ResumeContent {
    public record Line(String text, String source) {
    }

    public record Content(String fullText, List<Line> lines) {
    }

    private final ResumeRepository resumeRepository;
    private final ResumeSectionRepository sectionRepository;

    public ResumeContent(ResumeRepository resumeRepository, ResumeSectionRepository sectionRepository) {
        this.resumeRepository = resumeRepository;
        this.sectionRepository = sectionRepository;
    }

    public Optional<Content> forApplication(UUID userId, Application application) {
        if (application.getResumeId() == null) {
            return Optional.empty();
        }
        return resumeRepository.findByIdAndUserId(application.getResumeId(), userId).map(this::load);
    }

    private Content load(Resume resume) {
        StringBuilder full = new StringBuilder();
        append(full, resume.getTargetRole());
        append(full, resume.getSummary());
        List<Line> lines = new ArrayList<>();
        for (ResumeSection section : sectionRepository.findByResumeIdOrderBySortOrder(resume.getId())) {
            append(full, section.getTitle());
            append(full, section.getOrganisation());
            append(full, section.getDescription());
            if (section.getSectionType() == ResumeSection.SectionType.skill
                    || section.getSectionType() == ResumeSection.SectionType.education
                    || section.getDescription() == null) {
                continue;
            }
            String source = section.getTitle() == null ? section.getSectionType().name() : section.getTitle();
            for (String raw : section.getDescription().split("\\R")) {
                String line = raw.replaceFirst("^[\\s\\-*\\u2022]+", "").strip();
                if (line.length() >= 15) {
                    lines.add(new Line(line, source));
                }
            }
        }
        return new Content(full.toString(), lines);
    }

    private void append(StringBuilder builder, String value) {
        if (value != null && !value.isBlank()) {
            builder.append(value).append('\n');
        }
    }
}
