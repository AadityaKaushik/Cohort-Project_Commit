-- Rollback: db/rollback/U6__drop_tags_tables.sql

CREATE TABLE tags (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id    UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    name       TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX uq_tags_user_name ON tags (user_id, name);

CREATE TABLE application_tags (
    application_id UUID NOT NULL REFERENCES applications (id) ON DELETE CASCADE,
    tag_id         UUID NOT NULL REFERENCES tags (id) ON DELETE CASCADE,
    PRIMARY KEY (application_id, tag_id)
);

CREATE INDEX idx_application_tags_tag_id ON application_tags (tag_id);
