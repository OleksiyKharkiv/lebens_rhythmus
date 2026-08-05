-- V4__venue_to_group_level_plus_room.sql
--
-- LR-015 (Круглый стол #3, 2026-08-05): venue belongs to a scheduled
-- session (Group), not the workshop as a whole — a workshop can have
-- multiple groups at different times, and each needs its own place.
-- "room" is modeled as a plain field on venues, not a separate table —
-- one physical room = one venues row (e.g. "TLab29 — Blauer Saal" and
-- "TLab29 — Roter Saal" as two distinct rows sharing the same address),
-- matching the existing venue_id-per-select pattern already used
-- everywhere else in this schema (workshops.teacher_id, workshop_groups.
-- teacher_id, etc.) rather than inventing a new relation shape.
--
-- Data preserved, not dropped blind: any workshop that already has
-- venue_id set gets that value copied down to every one of its existing
-- groups BEFORE workshops.venue_id is removed.

ALTER TABLE venues
    ADD COLUMN room VARCHAR(100);

ALTER TABLE workshop_groups
    ADD COLUMN venue_id BIGINT REFERENCES venues (id);

UPDATE workshop_groups g
SET venue_id = w.venue_id
FROM workshops w
WHERE g.workshop_id = w.workshop_id
  AND w.venue_id IS NOT NULL;

ALTER TABLE workshops
    DROP COLUMN venue_id;
