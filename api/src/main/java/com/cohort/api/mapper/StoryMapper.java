package com.cohort.api.mapper;

import com.cohort.api.dto.StoryResponse;
import com.cohort.api.entity.Story;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class StoryMapper {

    public StoryResponse toResponse(Story story) {
        return new StoryResponse(
                story.getId(),
                story.getTitle(),
                story.getSituation(),
                story.getTask(),
                story.getAction(),
                story.getResult(),
                List.copyOf(story.getCompetencies()),
                story.getInterviewId(),
                story.getCreatedAt(),
                story.getUpdatedAt());
    }
}
