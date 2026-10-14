package com.cohort.api.dto;

import java.util.List;
import java.util.UUID;

public record TopicGraphResponse(List<TopicResponse> nodes, List<Edge> edges) {
    public record Edge(UUID topicId, UUID prerequisiteTopicId) {
    }
}
