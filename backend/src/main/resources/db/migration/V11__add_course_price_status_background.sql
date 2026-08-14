-- Urgent ticket 2026-08-14 (Olena): Course price + price comment + background
-- image URL + publish status (mirrors Workshop's status, own enum values are
-- identical: DRAFT/PUBLISHED/ARCHIVED/CANCELLED).
ALTER TABLE courses
    ADD COLUMN price NUMERIC(10, 2),
    ADD COLUMN price_description VARCHAR(1000),
    ADD COLUMN background_image_url VARCHAR(2048),
    ADD COLUMN status VARCHAR(50);

UPDATE courses SET status = 'DRAFT' WHERE status IS NULL;

ALTER TABLE courses
    ALTER COLUMN status SET NOT NULL;
