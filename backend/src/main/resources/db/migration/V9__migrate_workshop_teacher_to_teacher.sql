-- V9__migrate_workshop_teacher_to_teacher.sql
--
-- LR-072: workshops.teacher_id currently references users(id) — the
-- pre-existing inconsistency vs the correctly Teacher-typed
-- workshop_groups.teacher_id (V1's own comment already flagged this).
-- Before repointing the FK at teachers(id), any real data must be
-- remapped safely, not assumed empty — this migration does the
-- remapping itself rather than depending on a separate manual
-- pre-check, so it is correct regardless of what's actually in the
-- table at deploy time.
--
-- Step 1: for every workshop whose teacher_id currently points to a
-- User, repoint it to the Teacher with the same email (the only link
-- between User and Teacher that exists — see TeacherRepository.
-- findByEmail's own comment, LR-024).
UPDATE workshops w
SET teacher_id = t.id
FROM users u
         JOIN teachers t ON t.email = u.email
WHERE w.teacher_id = u.id;

-- Step 2: anything still pointing at a User id with no matching Teacher
-- row cannot satisfy the new FK — record it first (not silently lost,
-- per the ticket's explicit "не молчаливо терять ссылку" requirement),
-- then null it. The app owner can review this table and either create
-- the missing Teacher record or confirm the workshop legitimately has
-- no teacher assigned yet.
CREATE TABLE IF NOT EXISTS lr072_unmigrated_workshop_teachers
(
    workshop_id         BIGINT NOT NULL,
    workshop_name       VARCHAR(255),
    orphaned_user_id    BIGINT,
    orphaned_user_email VARCHAR(255),
    recorded_at         TIMESTAMP NOT NULL DEFAULT now()
);

INSERT INTO lr072_unmigrated_workshop_teachers (workshop_id, workshop_name, orphaned_user_id, orphaned_user_email)
SELECT w.workshop_id, w.workshop_name, w.teacher_id, u.email
FROM workshops w
         JOIN users u ON u.id = w.teacher_id
WHERE NOT EXISTS (SELECT 1 FROM teachers t WHERE t.email = u.email);

UPDATE workshops
SET teacher_id = NULL
WHERE teacher_id IN (SELECT orphaned_user_id FROM lr072_unmigrated_workshop_teachers);

-- Step 3: now safe to repoint the FK — every remaining non-null
-- teacher_id is guaranteed to reference a real teachers(id) row.
ALTER TABLE workshops
    DROP CONSTRAINT workshops_teacher_id_fkey;
ALTER TABLE workshops
    ADD CONSTRAINT workshops_teacher_id_fkey FOREIGN KEY (teacher_id) REFERENCES teachers (id);
