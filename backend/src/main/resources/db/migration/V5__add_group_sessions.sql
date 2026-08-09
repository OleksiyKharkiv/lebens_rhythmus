-- V5__add_group_sessions.sql
--
-- LR-067 (LR-ADR-022, Круглый стол-less decision — engineering pattern,
-- not a product choice): multi-day workshops (e.g. a 3-day marathon)
-- need per-day start/end/venue, but participants still register ONCE
-- for the whole thing, not once per day. If each day were a separate
-- Group (the alternative considered and rejected in LR-ADR-022), each
-- day would get its own capacity/enrollment roster — wrong for this
-- use case. Session is a child of Group: one Group keeps the single
-- enrollment/capacity roster it already has, Session rows underneath
-- it carry the per-day schedule/venue.
--
-- workshop_groups.start_date_time/end_date_time/venue_id are left
-- untouched — they remain the "day 1 / only day" values for the common
-- single-day Group case. Multi-day Groups additionally get Session
-- rows; this is additive, not a breaking change to existing data.

CREATE TABLE group_sessions
(
    id               BIGSERIAL PRIMARY KEY,
    group_id         BIGINT       NOT NULL REFERENCES workshop_groups (id),
    start_date_time  TIMESTAMP    NOT NULL,
    end_date_time    TIMESTAMP,
    venue_id         BIGINT REFERENCES venues (id),
    created_at       TIMESTAMP    NOT NULL DEFAULT now()
);

CREATE INDEX idx_group_sessions_group ON group_sessions (group_id);
