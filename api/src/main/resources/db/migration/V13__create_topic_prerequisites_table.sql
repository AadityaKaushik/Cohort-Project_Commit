-- Rollback: db/rollback/U13__drop_topic_prerequisites_table.sql

-- US-38/DR-15: a self-referencing join table forming a DAG over topics.
-- Postgres cannot express "no cycles" as a constraint, so acyclicity is
-- enforced by TopicPrerequisiteCycleTest (a real DFS + recursion-stack
-- traversal over this seeded data), not by the schema — the schema only
-- prevents self-loops and duplicate edges.
CREATE TABLE topic_prerequisites (
    topic_id              UUID NOT NULL REFERENCES topics (id) ON DELETE CASCADE,
    prerequisite_topic_id UUID NOT NULL REFERENCES topics (id) ON DELETE CASCADE,
    PRIMARY KEY (topic_id, prerequisite_topic_id),
    CONSTRAINT chk_topic_prerequisites_no_self_loop CHECK (topic_id <> prerequisite_topic_id)
);

CREATE INDEX idx_topic_prerequisites_prerequisite_topic_id ON topic_prerequisites (prerequisite_topic_id);

-- 39 edges over the 48 topics seeded in V12, hand-curated so the graph
-- reads sensibly (Arrays -> Two Pointers -> Sliding Window; Recursion ->
-- Trees -> Graphs -> Shortest Path, matching PROJECT_PLAN.md §21.4's own
-- example) rather than being randomly generated.
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'two_pointers' AND p.slug = 'arrays';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'sliding_window' AND p.slug = 'two_pointers';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'searching' AND p.slug = 'sorting';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'binary_search' AND p.slug = 'searching';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'queues' AND p.slug = 'stacks';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'trees' AND p.slug = 'recursion';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'binary_search_trees' AND p.slug = 'trees';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'binary_search_trees' AND p.slug = 'binary_search';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'heaps' AND p.slug = 'trees';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'tries' AND p.slug = 'trees';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'tries' AND p.slug = 'strings';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'graphs' AND p.slug = 'trees';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'graph_traversal_bfs_dfs' AND p.slug = 'graphs';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'shortest_path' AND p.slug = 'graph_traversal_bfs_dfs';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'minimum_spanning_tree' AND p.slug = 'graph_traversal_bfs_dfs';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'topological_sort' AND p.slug = 'graph_traversal_bfs_dfs';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'union_find' AND p.slug = 'graphs';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'backtracking' AND p.slug = 'recursion';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'dynamic_programming' AND p.slug = 'recursion';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'dp_on_arrays' AND p.slug = 'dynamic_programming';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'dp_on_arrays' AND p.slug = 'arrays';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'dp_on_strings' AND p.slug = 'dynamic_programming';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'dp_on_strings' AND p.slug = 'strings';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'dp_on_trees' AND p.slug = 'dynamic_programming';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'dp_on_trees' AND p.slug = 'trees';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'greedy_algorithms' AND p.slug = 'sorting';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'matrix_problems' AND p.slug = 'arrays';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'intervals' AND p.slug = 'sorting';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'scalability_load_balancing' AND p.slug = 'system_design_fundamentals';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'caching_strategies' AND p.slug = 'system_design_fundamentals';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'database_design' AND p.slug = 'system_design_fundamentals';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'distributed_systems' AND p.slug = 'scalability_load_balancing';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'distributed_systems' AND p.slug = 'database_design';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'api_design' AND p.slug = 'system_design_fundamentals';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'sql_joins_indexing' AND p.slug = 'sql_fundamentals';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'concurrency_multithreading' AND p.slug = 'operating_systems_basics';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'tcp_ip_http' AND p.slug = 'networking_basics';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'design_patterns' AND p.slug = 'oop_principles';
INSERT INTO topic_prerequisites (topic_id, prerequisite_topic_id) SELECT t.id, p.id FROM topics t, topics p WHERE t.slug = 'conflict_resolution' AND p.slug = 'behavioural_storytelling';
