-- V8__add_performance_course_fk.sql
--
-- LR-071 (LR-ADR-021): Performance can conclude either a Course or a
-- single Workshop, or neither, independently of each other — course_id
-- added alongside the already-existing workshop_id, not replacing it.

ALTER TABLE performances
    ADD COLUMN course_id BIGINT REFERENCES courses (id);
