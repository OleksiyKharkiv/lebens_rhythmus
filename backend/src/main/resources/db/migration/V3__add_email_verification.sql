-- V3__add_email_verification.sql
--
-- Backs the new registration email-verification flow: registering no
-- longer logs the user in immediately (User.emailVerified existed since
-- V1 but nothing ever set it true or sent any email — this closes that
-- gap). Login is blocked until the user clicks the link in their
-- verification email.
--
-- Stores a HASH of the verification token, never the plaintext token
-- itself — same reasoning as password hashing: a DB/backup leak must not
-- hand out working verification links. The plaintext token only ever
-- exists in the email sent to the user and in-memory while verifying.

ALTER TABLE users
    ADD COLUMN verification_token_hash    VARCHAR(255),
    ADD COLUMN verification_token_expires_at TIMESTAMP;

-- Critical: email_verified has defaulted to false since V1 and NOTHING has
-- ever set it true (the whole verification flow didn't exist until this
-- migration) — every account created before this deploy has
-- email_verified = false right now. Login now REQUIRES email_verified,
-- so without this backfill, deploying this migration would instantly lock
-- every existing user out of their own account, with no email ever sent
-- to them to verify in the first place (they registered before this
-- feature existed). Every row that exists at the moment this migration
-- runs is, by definition, a pre-existing account — Flyway runs this
-- exactly once, before any new registration under the new code can have
-- happened yet. Verification is mandatory only for accounts created AFTER
-- this point.
UPDATE users SET email_verified = true WHERE email_verified = false;
