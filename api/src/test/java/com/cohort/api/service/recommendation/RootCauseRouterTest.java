package com.cohort.api.service.recommendation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** US-52 / PROJECT_PLAN.md §21.5: {@link RootCauseRouter#routeToRootCause} against hand-built prerequisite graphs, including the mandatory depth-cap case. */
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
        // §21.5's own worked example, but with the prerequisite only marginally weaker (within the 0.15 margin) --
        // routing on noise this small is exactly what the margin exists to prevent.
        UUID shortestPath = UUID.randomUUID();
        UUID graphTraversal = UUID.randomUUID();
        Map<UUID, Double> mastery = Map.of(shortestPath, 0.46, graphTraversal, 0.40);
        Map<UUID, List<UUID>> prerequisites = Map.of(shortestPath, List.of(graphTraversal));

        RootCauseRouter.RoutingResult result = router.routeToRootCause(shortestPath, mastery, prerequisites);

        assertThat(result.topicId()).isEqualTo(shortestPath);
    }

    @Test
    void routesOneHopToAMeaningfullyWeakerPrerequisite() {
        // §21.5's own worked example: shortest_path (0.46) routes to graph_traversal_bfs_dfs (0.31) -- a 0.15 gap exactly at the boundary, so use 0.30 to be unambiguously past it.
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

        // d (0.3) is the true weakest ancestor, but the depth cap stops routing after 2 hops at c, not d.
        assertThat(result.topicId()).isEqualTo(c);
        assertThat(result.chain()).containsExactly(a, b, c);
    }

    /**
     * The mandatory pathological-graph case: an artificially long chain of
     * 21 topics, each strictly weaker than the last by well over the 0.15
     * margin (so, unlike a real graph, this one keeps satisfying the
     * routing condition far past where any real mastery score could), plus
     * a genuine two-node cycle layered on top. If the depth cap were
     * missing or broken, this would recurse 20 levels deep (or loop
     * forever around the cycle); with it, routing must still terminate at
     * exactly depth 2.
     */
    @Test
    void depthCapTerminatesOnAPathologicallyLongAndCyclicGraph() {
        int chainLength = 21;
        UUID[] topics = new UUID[chainLength];
        Map<UUID, Double> mastery = new HashMap<>();
        Map<UUID, List<UUID>> prerequisites = new HashMap<>();
        for (int i = 0; i < chainLength; i++) {
            topics[i] = UUID.randomUUID();
            mastery.put(topics[i], 1.0 - (i * 0.2)); // strictly decreasing well past [0,1] by the end -- deliberately adversarial
        }
        for (int i = 0; i < chainLength - 1; i++) {
            prerequisites.put(topics[i], List.of(topics[i + 1]));
        }
        // Layer a genuine cycle onto the tail: the last topic's "prerequisite" is the first topic.
        prerequisites.put(topics[chainLength - 1], List.of(topics[0]));

        RootCauseRouter.RoutingResult result = router.routeToRootCause(topics[0], mastery, prerequisites);

        assertThat(result.topicId()).isEqualTo(topics[2]);
        assertThat(result.chain()).containsExactly(topics[0], topics[1], topics[2]);
    }
}
