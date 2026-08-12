-- V10__add_group_course_recurrence.sql
--
-- LR-081/LR-082 (LR-ADR-023): Group carries the recurrence rule for a
-- Course-linked group — Course itself stays purely descriptive.
-- course_id is nullable and mutually exclusive with the existing
-- workshop_id (enforced in GroupService, not the DB — same as the
-- Workshop.courseId/Performance.courseId convention from LR-ADR-021).
--
-- recurrence_pattern holds a JSON array of {dayOfWeek,startTime,
-- durationMinutes} — one entry per selected weekday, each with its own
-- time/duration (customer's explicit spec, 2026-08-12: not one shared
-- time for every selected day). recurrence_start_date/end_date is the
-- admin-set generation window; workshop_groups.start_date_time/
-- end_date_time keep meaning "actual first/last occurrence" (synced
-- from real Session rows, LR-067/LR-074) — a deliberately separate pair
-- of fields, not a rename, so the two concepts don't collide.

ALTER TABLE workshop_groups
    ADD COLUMN course_id             BIGINT REFERENCES courses (id),
    ADD COLUMN recurrence_pattern    TEXT,
    ADD COLUMN recurrence_start_date DATE,
    ADD COLUMN recurrence_end_date   DATE;
