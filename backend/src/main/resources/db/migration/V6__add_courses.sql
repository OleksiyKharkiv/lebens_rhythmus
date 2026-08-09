-- V6__add_courses.sql
--
-- LR-069 (LR-ADR-023, Roundtable #8): Course is a purely descriptive/
-- marketing entity — no schedule fields at all (regularity lives on
-- Group, see LR-081). teacher_id here mirrors workshops.teacher_id's
-- existing shape (FK to users, not teachers) — the same already-flagged
-- temporary pattern as Workshop.teacher (see LR-072), not a new
-- inconsistency introduced by this migration.
--
-- is_online/is_synchronous/has_recordings + format_disclaimer_* exist to
-- structurally support the ZFU/FernUSG compliance argument
-- (docs/compliance/tlab29-zfu-compliance-brief.md) — format_disclaimer
-- is the single source of the compliance-sensitive text rendered on the
-- public course page, not duplicated free text per page.

CREATE TABLE courses (
    id                   BIGSERIAL PRIMARY KEY,
    title_de             VARCHAR(255) NOT NULL,
    title_en             VARCHAR(255) NOT NULL,
    title_ua             VARCHAR(255) NOT NULL,
    description_de       TEXT,
    description_en       TEXT,
    description_ua       TEXT,
    age_group_id         BIGINT REFERENCES age_groups (id),
    teacher_id           BIGINT REFERENCES users (id),
    is_online            BOOLEAN NOT NULL DEFAULT FALSE,
    is_synchronous        BOOLEAN NOT NULL DEFAULT TRUE,
    has_recordings        BOOLEAN NOT NULL DEFAULT FALSE,
    format_disclaimer_de TEXT,
    format_disclaimer_en TEXT,
    format_disclaimer_ua TEXT,
    created_at           TIMESTAMP NOT NULL,
    updated_at           TIMESTAMP
);
