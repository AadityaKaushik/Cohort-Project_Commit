package com.cohort.api.service.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RootCauseRouterTest {
    private final RootCauseRouter router = new RootCauseRouter();

    @Test
    void doesNotRouteWhenTheTopicHasNoPrerequisites() {
        UUID topic = UUID.randomUUID();
        RootCauseRouter.RoutingResult result = router.routeToRootCause(topic, Map.of(topic, 0.3), Map.of());

        assertThat(result.topicId()).isEqualTo(topic);
        assertThat(result.chain()).containsExactly(topic);
    }

    @Test
    void doesNotRouteWhenThePrerequisiteIsNotMeaningfullyWeaker() {
        // §21.5's own worked example, but with the prerequisite only marginally weaker (within the 0.15 margin) -- routing on noise this small is exactly what the margin exists to prevent.
        UUID shortestPath = UUID.randomUUID();
        UUID graphTraversal = UUID.randomUUID();
        Map<UUID, Double> mastery = Map.of(shortestPath, 0.46, graphTraversal, 0.40);
        Map<UUID, List<UUID>> prerequisites = Map.of(shortestPath, List.of(graphTraversal));

        RootCauseRouter.RoutingResult result = router.routeToRootCause(shortestPath, mastery, prerequisites);

        assertThat(result.topicId()).isEqualTo(shortestPath);
    }

    @Test
    void routesOneHopToAMeaningfullyWeakerPrerequisite() {
        UUID shortestPath = UUID.randomUUID();
        UUID graphTraversal = UUID.randomUUID();
        UUID graphs = UUID.randomUUID();
        Map<UUID, Double> mastery = Map.of(shortestPath, 0.46, graphTraversal, 0.30, graphs, 0.50);
        Map<UUID, List<UUID>> prerequisites = Map.of(
                shortestPath, List.of(graphTraversal),
                graphTraversal, List.of(graphs));

        RootCauseRouter.RoutingResult result = router.routeToRootCause(shortestPath, mastery, prerequisites);

        assertThat(result.topicId()).isEqualTo(graphTraversal);
        assertThat(result.chain()).containsExactly(shortestPath, graphTraversal);
    }

    @Test
    void stopsAtDepthTwoEvenWhenAnEvenWeakerAncestorExists() {
        UUID a = UUID.randomUUID();
        UUID b = UUID.randomUUID();
        UUID c = UUID.randomUUID();
        UUID d = UUID.randomUUID();
        Map<UUID, Double> mastery = Map.of(a, 0.9, b, 0.7, c, 0.5, d, 0.3);
        Map<UUID, List<UUID>> prerequisites = Map.of(a, List.of(b), b, List.of(c), c, List.of(d));

        RootCauseRouter.RoutingResult result = router.routeToRootCause(a, mastery, prerequisites);

        assertThat(result.topicId()).isEqualTo(c);
        assertThat(result.chain()).containsExactly(a, b, c);
    }

    @Test
    void depthCapTerminatesOnAPathologicallyLongAndCyclicGraph() {
        int chainLength = 21;
        UUID[] topics = new UUID[chainLength];
        Map<UUID, Double> mastery = new HashMap<>();
        Map<UUID, List<UUID>> prerequisites = new HashMap<>();
        for (int i = 0; i < chainLength; i++) {
            topics[i] = UUID.randomUUID();
            mastery.put(topics[i], 1.0 - (i * 0.2));
        }
        for (int i = 0; i < chainLength - 1; i++) {
            prerequisites.put(topics[i], List.of(topics[i + 1]));
        }
        prerequisites.put(topics[chainLength - 1], List.of(topics[0]));

        RootCauseRouter.RoutingResult result = router.routeToRootCause(topics[0], mastery, prerequisites);

        assertThat(result.topicId()).isEqualTo(topics[2]);
        assertThat(result.chain()).containsExactly(topics[0], topics[1], topics[2]);
    }
}
