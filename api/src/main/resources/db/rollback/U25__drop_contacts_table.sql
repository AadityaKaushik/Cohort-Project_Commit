DELETE FROM notifications WHERE type = 'cold_contact' OR related_entity_type = 'contact';

ALTER TABLE notifications DROP CONSTRAINT notifications_related_entity_type_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_related_entity_type_check
    CHECK (related_entity_type IN ('application', 'interview'));
ALTER TABLE notifications DROP CONSTRAINT notifications_type_check;
ALTER TABLE notifications ADD CONSTRAINT notifications_type_check
    CHECK (type IN ('stalled_application', 'upcoming_interview', 'missing_debrief',
                    'new_recommendation', 'fl_round_result'));

DROP TABLE IF EXISTS contacts;
