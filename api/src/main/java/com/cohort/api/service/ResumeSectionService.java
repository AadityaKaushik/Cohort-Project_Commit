package com.cohort.api.service;

import com.cohort.api.dto.ReorderResumeSectionsRequest;
import com.cohort.api.dto.ResumeSectionRequest;
import com.cohort.api.dto.ResumeSectionResponse;
import com.cohort.api.entity.ResumeSection;
import com.cohort.api.exception.ResumeNotFoundException;
import com.cohort.api.exception.ResumeSectionNotFoundException;
import com.cohort.api.repository.ResumeRepository;
import com.cohort.api.repository.ResumeSectionRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResumeSectionService {
    private final ResumeRepository resumeRepository;
    private final ResumeSectionRepository sectionRepository;

    public ResumeSectionService(ResumeRepository resumeRepository, ResumeSectionRepository sectionRepository) {
        this.resumeRepository = resumeRepository;
        this.sectionRepository = sectionRepository;
    }

    @Transactional(readOnly = true)
    public List<ResumeSectionResponse> list(UUID userId, UUID resumeId) {
        requireOwnedResume(userId, resumeId);
        return sections(resumeId).stream().map(this::toResponse).toList();
    }

    @Transactional
    public ResumeSectionResponse add(UUID userId, UUID resumeId, ResumeSectionRequest request) {
        requireOwnedResume(userId, resumeId);
        ResumeSection section = new ResumeSection(resumeId, request.sectionType());
        apply(section, request);
        section.setSortOrder(sections(resumeId).size());
        return toResponse(sectionRepository.save(section));
    }

    @Transactional
    public ResumeSectionResponse update(UUID userId, UUID resumeId, UUID sectionId, ResumeSectionRequest request) {
        requireOwnedResume(userId, resumeId);
        ResumeSection section = requireSection(resumeId, sectionId);
        apply(section, request);
        return toResponse(sectionRepository.save(section));
    }

    @Transactional
    public void delete(UUID userId, UUID resumeId, UUID sectionId) {
        requireOwnedResume(userId, resumeId);
        sectionRepository.delete(requireSection(resumeId, sectionId));
        renumber(sections(resumeId).stream().filter(s -> !s.getId().equals(sectionId)).toList());
    }

    @Transactional
    public List<ResumeSectionResponse> reorder(UUID userId, UUID resumeId, ReorderResumeSectionsRequest request) {
        requireOwnedResume(userId, resumeId);
        Map<UUID, ResumeSection> byId = sections(resumeId).stream()
                .collect(Collectors.toMap(ResumeSection::getId, Function.identity()));
        if (request.sectionIds().size() != byId.size() || !byId.keySet().containsAll(request.sectionIds())) {
            throw new ResumeSectionNotFoundException();
        }
        renumber(request.sectionIds().stream().map(byId::get).toList());
        return sections(resumeId).stream().map(this::toResponse).toList();
    }

    private void renumber(List<ResumeSection> ordered) {
        for (int i = 0; i < ordered.size(); i++) {
            ordered.get(i).setSortOrder(i);
        }
        sectionRepository.saveAll(ordered);
    }

    private List<ResumeSection> sections(UUID resumeId) {
        return sectionRepository.findByResumeIdOrderBySortOrder(resumeId);
    }

    private void requireOwnedResume(UUID userId, UUID resumeId) {
        resumeRepository.findByIdAndUserId(resumeId, userId).orElseThrow(ResumeNotFoundException::new);
    }

    private ResumeSection requireSection(UUID resumeId, UUID sectionId) {
        return sectionRepository.findById(sectionId)
                .filter(section -> section.getResumeId().equals(resumeId))
                .orElseThrow(ResumeSectionNotFoundException::new);
    }

    private void apply(ResumeSection section, ResumeSectionRequest request) {
        section.setTitle(request.title());
        section.setOrganisation(request.organisation());
        section.setStartDate(request.startDate());
        section.setEndDate(request.endDate());
        section.setDescription(request.description());
    }

    private ResumeSectionResponse toResponse(ResumeSection section) {
        return new ResumeSectionResponse(
                section.getId(),
                section.getSectionType(),
                section.getTitle(),
                section.getOrganisation(),
                section.getStartDate(),
                section.getEndDate(),
                section.getDescription(),
                section.getSortOrder());
    }
}
