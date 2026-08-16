-- LR-084 — Enrollment extended to also support Course (was Workshop-only)
-- and linked to Order for paid registrations.
ALTER TABLE enrollments
    ALTER COLUMN workshop_id DROP NOT NULL,
    ADD COLUMN course_id BIGINT REFERENCES courses (id),
    ADD COLUMN order_id BIGINT REFERENCES orders (id);

-- Order didn't have a course reference at all (only workshop/event) —
-- needed so a Course-enrollment's auto-created Order records what was
-- actually purchased, same as the existing workshop_id does for Workshop.
ALTER TABLE orders
    ADD COLUMN course_id BIGINT REFERENCES courses (id);

-- uk_user_workshop alone stops catching duplicate registrations once
-- workshop_id is nullable (Postgres treats NULL <> NULL in a unique
-- constraint) — a parallel constraint is needed for the Course path.
ALTER TABLE enrollments
    ADD CONSTRAINT uk_user_course UNIQUE (user_id, course_id);

CREATE INDEX idx_enrollment_course ON enrollments (course_id);
CREATE INDEX idx_enrollment_order ON enrollments (order_id);

-- architect-reviewer, 2026-08-16 — DEPLOY-BLOCKING without this: capacity_left
-- has only ever been set once, at row-creation time (capacity_left = capacity),
-- never decremented before this same diff introduces the first code that
-- reads it as the source of truth. Every pre-existing Group with real
-- PENDING/CONFIRMED enrollments would otherwise report full remaining
-- capacity on deploy, silently allowing new enrollments up to `capacity`
-- MORE than what's actually free — exactly the overbooking bug this ticket
-- exists to close, just moved from concurrent-request time to deploy time.
UPDATE workshop_groups g
SET capacity_left = GREATEST(0, g.capacity - COALESCE((
    SELECT COUNT(*) FROM enrollments e
    WHERE e.group_id = g.id AND e.status IN ('PENDING', 'CONFIRMED')
), 0));
