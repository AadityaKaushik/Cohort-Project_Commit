-- Rollback: db/rollback/U101__drop_question_content_key.sql

-- US-90: a stable key per system question, so R__question_bank.sql can
-- rewrite the text in place. Keeping the row ids keeps every practice
-- attempt and interview question link that points at them.
ALTER TABLE questions ADD COLUMN content_key TEXT UNIQUE;

-- The V14 placeholders were five fixed templates per topic. Each template
-- gets the slot whose difficulty it already had, so mastery history does
-- not shift.
UPDATE questions q
SET content_key = t.slug || '-' || CASE
        WHEN q.question_text LIKE 'Explain the core idea behind %' THEN 1
        WHEN q.question_text LIKE 'Describe a mistake candidates commonly make with %' THEN 2
        WHEN q.question_text LIKE 'Walk through how you would approach a classic %' THEN 3
        WHEN q.question_text LIKE 'Compare two different approaches to a %' THEN 4
        WHEN q.question_text LIKE 'What are the time and space complexity trade-offs %' THEN 5
    END
FROM topics t
WHERE q.topic_id = t.id
  AND q.is_system;
