ALTER TABLE company_role_patterns
    ADD COLUMN difficulty_level SMALLINT CHECK (difficulty_level BETWEEN 1 AND 5),
    ADD COLUMN difficulty_basis TEXT;
