ALTER TABLE interview_questions DROP CONSTRAINT IF EXISTS fk_interview_questions_linked_question;
ALTER TABLE interview_questions DROP CONSTRAINT IF EXISTS fk_interview_questions_topic;
DROP TABLE IF EXISTS questions;
