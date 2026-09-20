package com.cohort.api.migration;

import static org.assertj.core.api.Assertions.assertThat;

import com.cohort.api.AbstractIntegrationTest;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * US-38 / PROJECT_PLAN.md line 1541: {@code topic_prerequisites} "must
 * remain acyclic — enforced by a seed-time cycle-detection test, since
 * Postgres cannot express this as a constraint." This test loads the real
 * seeded graph from a real Postgres instance (V12/V13 migrations) and runs
 * an actual depth-first traversal with recursion-stack (white/gray/black)
 * colouring to prove it: a topic re-entered while still on the current DFS
 * path (coloured GRAY) means a cycle, which is exactly the classic
 * back-edge detection algorithm, not just an ad-hoc row count check. If any
 * future migration ever adds a prerequisite edge that closes a loop, this
 * test fails the build.
 */
@SpringBootTest
@Import(AbstractIntegrationTest.ContainerConfig.class)
class TopicPrerequisiteCycleTest extends AbstractIntegrationTest {

    private enum Colour { WHITE, GRAY, BLACK }

    @Autowired
    private DataSource dataSource;

    @Test
    void seededTopicCountIsWithinThePlannedRange() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM topics")) {
            rs.next();
            int count = rs.getInt(1);
            // US-38 acceptance criterion: ~40-60 topics.
            assertThat(count).isBetween(40, 60);
        }
    }

    @Test
    void everyQuestionLinksToExactlyOneTopic() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery(
                        "SELECT COUNT(*) FROM questions WHERE topic_id IS NULL")) {
            rs.next();
            assertThat(rs.getInt(1)).isZero();
        }
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("SELECT COUNT(*) FROM questions WHERE is_system = true")) {
            rs.next();
            // US-35 acceptance criterion: seeded with >= 150 questions.
            assertThat(rs.getInt(1)).isGreaterThanOrEqualTo(150);
        }
    }

    @Test
    void seededPrerequisiteGraphIsAcyclic() throws Exception {
        Map<String, List<String>> graph = loadGraph();
        assertThat(graph).isNotEmpty();

        Map<String, Colour> colour = new HashMap<>();
        for (String topic : graph.keySet()) {
            colour.put(topic, Colour.WHITE);
        }

        for (String topic : graph.keySet()) {
            if (colour.get(topic) == Colour.WHITE) {
                String cycleDescription = dfsFindCycle(topic, graph, colour, new ArrayDeque<>());
                assertThat(cycleDescription)
                        .as("prerequisite graph must be acyclic, found: %s", cycleDescription)
                        .isNull();
            }
        }
    }

    /**
     * Classic DFS cycle detection: a node is GRAY while it is still on the
     * current recursion path and BLACK once fully explored. Encountering a
     * GRAY node again is a back edge, i.e. a cycle — the same algorithm
     * taught for detecting cycles in a directed graph. Returns a
     * human-readable description of the cycle if one is found, else null.
     */
    private String dfsFindCycle(
            String node, Map<String, List<String>> graph, Map<String, Colour> colour, ArrayDeque<String> path) {
        colour.put(node, Colour.GRAY);
        path.push(node);

        for (String next : graph.getOrDefault(node, List.of())) {
            if (colour.get(next) == Colour.GRAY) {
                return path.toString() + " -> " + next;
            }
            if (colour.get(next) == Colour.WHITE) {
                String found = dfsFindCycle(next, graph, colour, path);
                if (found != null) {
                    return found;
                }
            }
        }

        path.pop();
        colour.put(node, Colour.BLACK);
        return null;
    }

    /** Edges point from a topic to its prerequisite, mirroring the FK columns. */
    private Map<String, List<String>> loadGraph() throws Exception {
        Map<String, List<String>> graph = new HashMap<>();
        Set<String> allTopics = new HashSet<>();

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            try (ResultSet rs = statement.executeQuery("SELECT id::text FROM topics")) {
                while (rs.next()) {
                    allTopics.add(rs.getString(1));
                }
            }
            for (String topicId : allTopics) {
                graph.put(topicId, new java.util.ArrayList<>());
            }
            try (ResultSet rs = statement.executeQuery(
                    "SELECT topic_id::text, prerequisite_topic_id::text FROM topic_prerequisites")) {
                while (rs.next()) {
                    graph.get(rs.getString(1)).add(rs.getString(2));
                }
            }
        }
        return graph;
    }
}
