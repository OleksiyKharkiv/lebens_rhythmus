-- V2__widen_encrypted_pii_columns.sql
--
-- users.{first_name,last_name,phone,address,city,zip_code},
-- teachers.{first_name,last_name,phone}, participants.{first_name,last_name,phone}
-- are now encrypted at rest (EncryptedStringConverter, AES-256-GCM,
-- base64-encoded). Ciphertext is always longer than the original plaintext
-- (12-byte IV + 16-byte GCM tag, then base64's ~4/3 inflation) — a plaintext
-- value close to VARCHAR(255)'s limit would silently fail to fit once
-- encrypted. `address` in particular has no application-level length cap
-- today, unlike firstName/lastName which are bounded by @Size(max = 50).
--
-- TEXT is functionally identical to an unbounded VARCHAR in Postgres (same
-- storage, same performance) — switching removes the need to ever
-- recompute a safe byte budget again for these columns, rather than
-- picking a new fixed VARCHAR(n) that could eventually hit the same wall.
--
-- iban/tax_id are NOT touched here: both are bounded by real-world format
-- ceilings (longest IBAN is 34 chars) that comfortably fit VARCHAR(255)
-- even after encryption — no equivalent risk.

ALTER TABLE users
    ALTER COLUMN first_name TYPE TEXT,
    ALTER COLUMN last_name  TYPE TEXT,
    ALTER COLUMN phone      TYPE TEXT,
    ALTER COLUMN address    TYPE TEXT,
    ALTER COLUMN city       TYPE TEXT,
    ALTER COLUMN zip_code   TYPE TEXT;

ALTER TABLE teachers
    ALTER COLUMN first_name TYPE TEXT,
    ALTER COLUMN last_name  TYPE TEXT,
    ALTER COLUMN phone      TYPE TEXT;

ALTER TABLE participants
    ALTER COLUMN first_name TYPE TEXT,
    ALTER COLUMN last_name  TYPE TEXT,
    ALTER COLUMN phone      TYPE TEXT;
