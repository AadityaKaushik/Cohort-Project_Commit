package com.cohort.api.service;

import com.cohort.api.dto.CreateStoryRequest;
import com.cohort.api.dto.StoryResponse;
import com.cohort.api.dto.UpdateStoryRequest;
import com.cohort.api.entity.Story;
import com.cohort.api.exception.InterviewNotFoundException;
import com.cohort.api.exception.StoryNotFoundException;
import com.cohort.api.mapper.StoryMapper;
import com.cohort.api.repository.InterviewRepository;
import com.cohort.api.repository.StoryRepository;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoryService {

    private final StoryRepository storyRepository;
    private final InterviewRepository interviewRepository;
    private final StoryMapper mapper;

    public StoryService(StoryRepository storyRepository, InterviewRepository interviewRepository, StoryMapper mapper) {
        this.storyRepository = storyRepository;
        this.interviewRepository = interviewRepository;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public List<StoryResponse> list(UUID userId, String competency) {
        return storyRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .filter(story -> competency == null || story.getCompetencies().contains(competency))
                .map(mapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StoryResponse get(UUID userId, UUID id) {
        return mapper.toResponse(requireOwned(userId, id));
    }

    @Transactional
    public StoryResponse create(UUID userId, CreateStoryRequest request) {
        requireOwnedInterview(userId, request.interviewId());
        Story story = new Story(userId, request.title().trim());
        story.setSituation(request.situation());
        story.setTask(request.task());
        story.setAction(request.action());
        story.setResult(request.result());
        if (request.competencies() != null) {
            story.setCompetencies(request.competencies().stream().distinct().toList());
        }
        story.setInterviewId(request.interviewId());
        return mapper.toResponse(storyRepository.save(story));
    }

    @Transactional
    public StoryResponse update(UUID userId, UUID id, UpdateStoryRequest request) {
        Story story = requireOwned(userId, id);
        requireOwnedInterview(userId, request.interviewId());
        if (request.title() != null) {
            story.setTitle(request.title().trim());
        }
        if (request.situation() != null) {
            story.setSituation(request.situation());
        }
        if (request.task() != null) {
            story.setTask(request.task());
        }
        if (request.action() != null) {
            story.setAction(request.action());
        }
        if (request.result() != null) {
            story.setResult(request.result());
        }
        if (request.competencies() != null) {
            story.setCompetencies(request.competencies().stream().distinct().toList());
        }
        if (request.interviewId() != null) {
            story.setInterviewId(request.interviewId());
        }
        return mapper.toResponse(storyRepository.save(story));
    }

    @Transactional
    public void delete(UUID userId, UUID id) {
        requireOwned(userId, id);
        storyRepository.deleteByIdAndUserId(id, userId);
    }

    private Story requireOwned(UUID userId, UUID id) {
        return storyRepository.findByIdAndUserId(id, userId).orElseThrow(StoryNotFoundException::new);
    }

    private void requireOwnedInterview(UUID userId, UUID interviewId) {
        if (interviewId != null && !interviewRepository.existsByIdAndUserId(interviewId, userId)) {
            throw new InterviewNotFoundException();
        }
    }
}
