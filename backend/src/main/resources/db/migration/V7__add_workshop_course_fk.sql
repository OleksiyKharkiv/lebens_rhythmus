-- V7__add_workshop_course_fk.sql
--
-- LR-070 (LR-ADR-021): Course -> Workshop is zero-to-many, unidirectional
-- (a Course can include several Workshops, each Workshop belongs to at
-- most one Course) — nullable, existing Workshop rows keep course_id
-- NULL ("Workshop without a Course" is an already-supported case).

ALTER TABLE workshops
    ADD COLUMN course_id BIGINT REFERENCES courses (id);
