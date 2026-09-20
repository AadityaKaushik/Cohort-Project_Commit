package com.cohort.api.dto;

import java.util.List;
import java.util.UUID;

/**
 * {@code GET /topics/graph} (PROJECT_PLAN.md §18.5): nodes plus prerequisite
 * edges, exactly what {@code TopicGrid} (P31) needs to draw prerequisite
 * relationships on hover without a second round-trip per topic.
 */
public record TopicGraphResponse(List<TopicResponse> nodes, List<Edge> edges) {

    public record Edge(UUID topicId, UUID prerequisiteTopicId) {
    }
}
