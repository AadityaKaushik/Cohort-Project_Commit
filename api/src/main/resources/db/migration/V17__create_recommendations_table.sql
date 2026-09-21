-- Rollback: db/rollback/U17__drop_recommendations_table.sql

-- US-51..56 / PROJECT_PLAN.md §21: a user's own generated recommendations.
-- Strictly user-owned, like preparation_tasks. Rows in `active` status are
-- a regenerated cache -- RecommendationFacade (P42) recomputes and replaces
-- them on every read, so `active` rows never accumulate duplicates; rows
-- that move to `accepted`/`snoozed`/`dismissed` are left alone and read
-- back to suppress regenerating the same candidate (US-55).
--
-- `type` intentionally lists the five generator outputs actually built in
-- P39/P40 (weak_topic, prerequisite_gap, decay, stalled_application,
-- baseline) rather than every generator sketched in PROJECT_PLAN.md §21.3
-- (coverage/missing_debrief/cadence/resume_pattern are out of this
-- sprint's scope -- see the P39 commit body). `evidence_tier` mirrors
-- §21.6/FR-73 exactly: A/B/C/D. `model_version` is deliberately omitted
-- here (unlike the illustrative schema sketch in PROJECT_PLAN.md §17.2) --
-- there is no federated model to version until P43+ lands; adding the
-- column now would just be dead schema.
CREATE TABLE recommendations (
    id                     UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    type                   TEXT NOT NULL
                               CHECK (type IN ('weak_topic', 'prerequisite_gap', 'decay',
                                                'stalled_application', 'baseline')),
    evidence_tier          CHAR(1) NOT NULL CHECK (evidence_tier IN ('A', 'B', 'C', 'D')),
    target_topic_id        UUID REFERENCES topics (id),
    target_application_id  UUID REFERENCES applications (id) ON DELETE CASCADE,
    score                  NUMERIC(6, 4) NOT NULL,
    headline               TEXT NOT NULL,
    explanation            TEXT NOT NULL,
    observation_count      INTEGER NOT NULL DEFAULT 0,
    status                 TEXT NOT NULL DEFAULT 'active'
                               CHECK (status IN ('active', 'accepted', 'snoozed', 'dismissed')),
    snoozed_until          TIMESTAMPTZ,
    dismissed_reason       TEXT,
    accepted_task_id       UUID REFERENCES preparation_tasks (id) ON DELETE SET NULL,
    generated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    created_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_recommendations_user_status ON recommendations (user_id, status);
CREATE INDEX idx_recommendations_user_type_status ON recommendations (user_id, type, status);
