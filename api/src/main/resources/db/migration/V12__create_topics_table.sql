-- Rollback: db/rollback/U12__drop_topics_table.sql

-- US-38: system-owned topic taxonomy (PROJECT_PLAN.md §17.2, line 1539).
-- Seeded and versioned here rather than in the dev-only R__seed_dev_data.sql
-- (US-72) because topics/questions are reference data every environment
-- needs, not synthetic demo content gated behind the `dev` profile.
CREATE TABLE topics (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name                TEXT NOT NULL UNIQUE,
    slug                TEXT NOT NULL UNIQUE,
    category            TEXT NOT NULL
                            CHECK (category IN ('dsa', 'system_design', 'databases',
                                                 'os_networking', 'oop', 'behavioural')),
    description         TEXT,
    default_difficulty  TEXT NOT NULL CHECK (default_difficulty IN ('easy', 'medium', 'hard')),
    taxonomy_version    SMALLINT NOT NULL DEFAULT 1,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_topics_category ON topics (category);

-- ~40-60 topics across categories (US-38 acceptance criterion), forming the
-- DAG that topic_prerequisites (V13) draws its edges over. See
-- TopicPrerequisiteCycleTest for the mandatory cycle-detection check.
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Arrays', 'arrays', 'dsa', 'Arrays — part of the dsa category, default difficulty easy.', 'easy', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Strings', 'strings', 'dsa', 'Strings — part of the dsa category, default difficulty easy.', 'easy', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Two Pointers', 'two_pointers', 'dsa', 'Two Pointers — part of the dsa category, default difficulty easy.', 'easy', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Sliding Window', 'sliding_window', 'dsa', 'Sliding Window — part of the dsa category, default difficulty medium.', 'medium', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Hashing', 'hashing', 'dsa', 'Hashing — part of the dsa category, default difficulty easy.', 'easy', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Sorting', 'sorting', 'dsa', 'Sorting — part of the dsa category, default difficulty easy.', 'easy', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Searching', 'searching', 'dsa', 'Searching — part of the dsa category, default difficulty easy.', 'easy', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Binary Search', 'binary_search', 'dsa', 'Binary Search — part of the dsa category, default difficulty medium.', 'medium', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Recursion', 'recursion', 'dsa', 'Recursion — part of the dsa category, default difficulty easy.', 'easy', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Linked Lists', 'linked_lists', 'dsa', 'Linked Lists — part of the dsa category, default difficulty easy.', 'easy', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Stacks', 'stacks', 'dsa', 'Stacks — part of the dsa category, default difficulty easy.', 'easy', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Queues', 'queues', 'dsa', 'Queues — part of the dsa category, default difficulty easy.', 'easy', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Trees', 'trees', 'dsa', 'Trees — part of the dsa category, default difficulty medium.', 'medium', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Binary Search Trees', 'binary_search_trees', 'dsa', 'Binary Search Trees — part of the dsa category, default difficulty medium.', 'medium', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Heaps', 'heaps', 'dsa', 'Heaps — part of the dsa category, default difficulty medium.', 'medium', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Tries', 'tries', 'dsa', 'Tries — part of the dsa category, default difficulty medium.', 'medium', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Graphs', 'graphs', 'dsa', 'Graphs — part of the dsa category, default difficulty medium.', 'medium', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Graph Traversal (BFS/DFS)', 'graph_traversal_bfs_dfs', 'dsa', 'Graph Traversal (BFS/DFS) — part of the dsa category, default difficulty medium.', 'medium', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Shortest Path Algorithms', 'shortest_path', 'dsa', 'Shortest Path Algorithms — part of the dsa category, default difficulty hard.', 'hard', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Minimum Spanning Tree', 'minimum_spanning_tree', 'dsa', 'Minimum Spanning Tree — part of the dsa category, default difficulty hard.', 'hard', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Topological Sort', 'topological_sort', 'dsa', 'Topological Sort — part of the dsa category, default difficulty medium.', 'medium', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Union-Find (Disjoint Set)', 'union_find', 'dsa', 'Union-Find (Disjoint Set) — part of the dsa category, default difficulty medium.', 'medium', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Backtracking', 'backtracking', 'dsa', 'Backtracking — part of the dsa category, default difficulty medium.', 'medium', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Dynamic Programming', 'dynamic_programming', 'dsa', 'Dynamic Programming — part of the dsa category, default difficulty hard.', 'hard', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('DP on Arrays', 'dp_on_arrays', 'dsa', 'DP on Arrays — part of the dsa category, default difficulty hard.', 'hard', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('DP on Strings', 'dp_on_strings', 'dsa', 'DP on Strings — part of the dsa category, default difficulty hard.', 'hard', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('DP on Trees', 'dp_on_trees', 'dsa', 'DP on Trees — part of the dsa category, default difficulty hard.', 'hard', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Greedy Algorithms', 'greedy_algorithms', 'dsa', 'Greedy Algorithms — part of the dsa category, default difficulty medium.', 'medium', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Bit Manipulation', 'bit_manipulation', 'dsa', 'Bit Manipulation — part of the dsa category, default difficulty medium.', 'medium', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Math & Number Theory', 'math_number_theory', 'dsa', 'Math & Number Theory — part of the dsa category, default difficulty medium.', 'medium', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Matrix Problems', 'matrix_problems', 'dsa', 'Matrix Problems — part of the dsa category, default difficulty medium.', 'medium', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Intervals', 'intervals', 'dsa', 'Intervals — part of the dsa category, default difficulty medium.', 'medium', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('System Design Fundamentals', 'system_design_fundamentals', 'system_design', 'System Design Fundamentals — part of the system design category, default difficulty medium.', 'medium', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Scalability & Load Balancing', 'scalability_load_balancing', 'system_design', 'Scalability & Load Balancing — part of the system design category, default difficulty hard.', 'hard', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Caching Strategies', 'caching_strategies', 'system_design', 'Caching Strategies — part of the system design category, default difficulty medium.', 'medium', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Database Design', 'database_design', 'system_design', 'Database Design — part of the system design category, default difficulty medium.', 'medium', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Distributed Systems', 'distributed_systems', 'system_design', 'Distributed Systems — part of the system design category, default difficulty hard.', 'hard', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('API Design', 'api_design', 'system_design', 'API Design — part of the system design category, default difficulty medium.', 'medium', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('SQL Fundamentals', 'sql_fundamentals', 'databases', 'SQL Fundamentals — part of the databases category, default difficulty easy.', 'easy', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('SQL Joins & Indexing', 'sql_joins_indexing', 'databases', 'SQL Joins & Indexing — part of the databases category, default difficulty medium.', 'medium', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Operating Systems Basics', 'operating_systems_basics', 'os_networking', 'Operating Systems Basics — part of the os networking category, default difficulty easy.', 'easy', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Concurrency & Multithreading', 'concurrency_multithreading', 'os_networking', 'Concurrency & Multithreading — part of the os networking category, default difficulty hard.', 'hard', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Networking Basics', 'networking_basics', 'os_networking', 'Networking Basics — part of the os networking category, default difficulty easy.', 'easy', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('TCP/IP & HTTP', 'tcp_ip_http', 'os_networking', 'TCP/IP & HTTP — part of the os networking category, default difficulty medium.', 'medium', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('OOP Principles', 'oop_principles', 'oop', 'OOP Principles — part of the oop category, default difficulty easy.', 'easy', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Design Patterns', 'design_patterns', 'oop', 'Design Patterns — part of the oop category, default difficulty medium.', 'medium', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Behavioural Storytelling (STAR)', 'behavioural_storytelling', 'behavioural', 'Behavioural Storytelling (STAR) — part of the behavioural category, default difficulty easy.', 'easy', 1);
INSERT INTO topics (name, slug, category, description, default_difficulty, taxonomy_version) VALUES ('Conflict Resolution', 'conflict_resolution', 'behavioural', 'Conflict Resolution — part of the behavioural category, default difficulty medium.', 'medium', 1);
