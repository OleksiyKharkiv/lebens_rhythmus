-- V1__baseline.sql — Lebens Rhythmus honest schema baseline
-- Per LR-ADR-003 (docs/architecture/decisions.md): recreated from scratch,
-- not migrated from the pre-Flyway ddl-auto=update database. Reflects the
-- CURRENT entity model (backend/src/main/java/com/be/domain/entity/) as of
-- 2026-07-21, transcribed by hand column-by-column against every @Entity —
-- not generated against a live DB (Docker unavailable in this environment).
-- Run `./gradlew test` (Testcontainers) or a real deploy before trusting
-- this blindly — Hibernate ddl-auto=validate will fail loudly at startup
-- if anything here doesn't match the entities exactly.
--
-- Known smell carried over faithfully, not silently fixed (see
-- IMPLEMENTATION-PROTOCOL-2026-07.md / Wave 2 Faculty work):
-- `participants.participant_id` is a second, oddly-named FK to
-- workshop_groups — comes from Group.java's vestigial unidirectional
-- `@OneToMany @JoinColumn("participant_id")` mapping, actively read by
-- GroupService (capacity checks) even though Participant.group already
-- expresses the same relationship correctly via `group_id`. Not removed
-- here — changing it means editing GroupService, out of scope for a DB
-- migration.

CREATE TABLE users (
    id                          BIGSERIAL PRIMARY KEY,
    email                       VARCHAR(255) NOT NULL UNIQUE,
    password                    VARCHAR(255) NOT NULL,
    first_name                  VARCHAR(255),
    last_name                   VARCHAR(255),
    phone                       VARCHAR(255),
    birth_date                  DATE,
    failed_login_attempts       INT NOT NULL DEFAULT 0,
    lock_until                  TIMESTAMP,
    enabled                     BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified              BOOLEAN NOT NULL DEFAULT FALSE,
    accepted_terms              BOOLEAN NOT NULL DEFAULT FALSE,
    privacy_policy_accepted     BOOLEAN NOT NULL DEFAULT FALSE,
    terms_accepted_at           TIMESTAMP,
    privacy_policy_accepted_at  TIMESTAMP,
    role                        VARCHAR(255),
    address                     VARCHAR(255),
    city                        VARCHAR(255),
    zip_code                    VARCHAR(255),
    country                     VARCHAR(255),
    title                       VARCHAR(255),
    bio                         VARCHAR(255),
    iban                        VARCHAR(255), -- encrypted at rest, see EncryptedStringConverter
    tax_id                      VARCHAR(255), -- encrypted at rest, see EncryptedStringConverter
    created_at                  TIMESTAMP,
    updated_at                  TIMESTAMP
);
CREATE INDEX idx_user_email ON users (email);
CREATE INDEX idx_user_role ON users (role);
CREATE INDEX idx_user_lock_until ON users (lock_until);

CREATE TABLE teachers (
    id           BIGSERIAL PRIMARY KEY,
    first_name   VARCHAR(255) NOT NULL,
    last_name    VARCHAR(255) NOT NULL,
    email        VARCHAR(255) NOT NULL UNIQUE,
    phone        VARCHAR(255) NOT NULL,
    title        VARCHAR(255) NOT NULL,
    approved     BOOLEAN NOT NULL DEFAULT FALSE,
    bio_de       TEXT,
    bio_en       TEXT,
    bio_ua       TEXT,
    active       BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE languages (
    id      BIGSERIAL PRIMARY KEY,
    name_de VARCHAR(255) NOT NULL,
    name_en VARCHAR(255) NOT NULL,
    name_ua VARCHAR(255) NOT NULL,
    code    VARCHAR(2) NOT NULL,
    active  BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE age_groups (
    id       BIGSERIAL PRIMARY KEY,
    title_de VARCHAR(255) NOT NULL,
    title_en VARCHAR(255) NOT NULL,
    title_ua VARCHAR(255) NOT NULL,
    min_age  INT NOT NULL,
    max_age  INT NOT NULL
);

CREATE TABLE activities (
    id               BIGSERIAL PRIMARY KEY,
    title_de         VARCHAR(255) NOT NULL,
    title_en         VARCHAR(255) NOT NULL,
    title_ua         VARCHAR(255) NOT NULL,
    description_de   VARCHAR(255) NOT NULL,
    description_en   VARCHAR(255) NOT NULL,
    description_ua   VARCHAR(255) NOT NULL,
    price            NUMERIC(12,2) NOT NULL,
    duration_minutes INT NOT NULL,
    active           BOOLEAN NOT NULL
);

CREATE TABLE venues (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    address       VARCHAR(255),
    city          VARCHAR(100),
    postal_code   VARCHAR(20),
    country       VARCHAR(100),
    capacity      INT,
    description   TEXT,
    contact_phone VARCHAR(50),
    contact_email VARCHAR(100),
    created_at    TIMESTAMP NOT NULL,
    updated_at    TIMESTAMP NOT NULL
);

CREATE TABLE workshops (
    workshop_id      BIGSERIAL PRIMARY KEY,
    workshop_name    VARCHAR(255) NOT NULL,
    description      TEXT,
    start_date       DATE,
    end_date         DATE,
    max_participants INT,
    price            NUMERIC(12,2),
    status           VARCHAR(50),
    venue_id         BIGINT REFERENCES venues (id),
    teacher_id       BIGINT REFERENCES users (id), -- NOTE: Workshop.teacher is typed as User, not Teacher — inconsistent with Group.teacher (see Wave 2 Faculty work)
    language_id      BIGINT REFERENCES languages (id),
    age_group_id     BIGINT REFERENCES age_groups (id),
    created_at       TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP
);
CREATE INDEX idx_workshop_start ON workshops (start_date);
CREATE INDEX idx_workshop_status ON workshops (status);

CREATE TABLE workshop_groups (
    id              BIGSERIAL PRIMARY KEY,
    title_de        VARCHAR(255) NOT NULL,
    title_en        VARCHAR(255) NOT NULL,
    title_ua        VARCHAR(255) NOT NULL,
    capacity        INT NOT NULL,
    capacity_left   INT NOT NULL,
    start_date_time TIMESTAMP NOT NULL,
    end_date_time   TIMESTAMP,
    activity_id     BIGINT REFERENCES activities (id),
    age_group_id    BIGINT REFERENCES age_groups (id),
    language_id     BIGINT REFERENCES languages (id),
    workshop_id     BIGINT REFERENCES workshops (workshop_id),
    teacher_id      BIGINT REFERENCES teachers (id),
    active          BOOLEAN NOT NULL DEFAULT TRUE
);
CREATE INDEX idx_group_teacher ON workshop_groups (teacher_id);
CREATE INDEX idx_group_start ON workshop_groups (start_date_time);
CREATE INDEX idx_group_active ON workshop_groups (active);

CREATE TABLE participants (
    id             BIGSERIAL PRIMARY KEY,
    first_name     VARCHAR(255) NOT NULL,
    last_name      VARCHAR(255) NOT NULL,
    email          VARCHAR(255) NOT NULL UNIQUE,
    phone          VARCHAR(255) NOT NULL,
    birth_date     DATE NOT NULL,
    group_id       BIGINT NOT NULL REFERENCES workshop_groups (id),
    participant_id BIGINT REFERENCES workshop_groups (id), -- see file header note
    active         BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE contracts (
    id              BIGSERIAL PRIMARY KEY,
    contract_number VARCHAR(120) NOT NULL UNIQUE,
    title           VARCHAR(255),
    party_name      VARCHAR(255),
    contact         VARCHAR(255),
    start_date      DATE,
    end_date        DATE,
    amount          NUMERIC(12,2),
    currency        VARCHAR(8),
    status          VARCHAR(40),
    contract_url    VARCHAR(255),
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP
);
CREATE INDEX idx_contract_number ON contracts (contract_number);

CREATE TABLE events (
    id              BIGSERIAL PRIMARY KEY,
    title           VARCHAR(255),
    description     VARCHAR(2000),
    start_date_time TIMESTAMP,
    end_date_time   TIMESTAMP,
    venue_id        BIGINT REFERENCES venues (id),
    workshop_id     BIGINT REFERENCES workshops (workshop_id),
    contract_id     BIGINT REFERENCES contracts (id),
    price           NUMERIC(12,2),
    currency        VARCHAR(8),
    capacity        INT,
    status          VARCHAR(40),
    created_at      TIMESTAMP,
    updated_at      TIMESTAMP
);
CREATE INDEX idx_event_start ON events (start_date_time);
CREATE INDEX idx_event_venue ON events (venue_id);

CREATE TABLE orders (
    id             BIGSERIAL PRIMARY KEY,
    order_number   VARCHAR(255) NOT NULL UNIQUE,
    user_id        BIGINT REFERENCES users (id),
    participant_id BIGINT REFERENCES participants (id),
    workshop_id    BIGINT REFERENCES workshops (workshop_id),
    event_id       BIGINT REFERENCES events (id),
    amount         NUMERIC(12,2),
    currency       VARCHAR(8),
    quantity       INT,
    status         VARCHAR(32),
    note           VARCHAR(2000),
    contract_id    BIGINT REFERENCES contracts (id),
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP
);
CREATE INDEX idx_order_number ON orders (order_number);
CREATE INDEX idx_order_user ON orders (user_id);

CREATE TABLE payments (
    id             BIGSERIAL PRIMARY KEY,
    order_id       BIGINT REFERENCES orders (id),
    user_id        BIGINT REFERENCES users (id),
    amount         NUMERIC(12,2) NOT NULL,
    currency       VARCHAR(8),
    provider       VARCHAR(255),
    method_name    VARCHAR(255),
    transaction_id VARCHAR(200),
    status         VARCHAR(40),
    paid_at        TIMESTAMP,
    note           VARCHAR(1000),
    created_at     TIMESTAMP,
    updated_at     TIMESTAMP
);
CREATE INDEX idx_payment_tx ON payments (transaction_id);
CREATE INDEX idx_payment_order ON payments (order_id);

CREATE TABLE enrollments (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users (id),
    workshop_id BIGINT NOT NULL REFERENCES workshops (workshop_id),
    group_id    BIGINT REFERENCES workshop_groups (id),
    status      VARCHAR(30) NOT NULL,
    created_at  TIMESTAMP NOT NULL,
    CONSTRAINT uk_user_workshop UNIQUE (user_id, workshop_id)
);

CREATE TABLE performances (
    id               BIGSERIAL PRIMARY KEY,
    workshop_id      BIGINT REFERENCES workshops (workshop_id),
    title            VARCHAR(200) NOT NULL,
    description      TEXT,
    performance_date TIMESTAMP NOT NULL,
    venue            VARCHAR(300),
    max_attendees    INT,
    status           VARCHAR(50) NOT NULL,
    created_at       TIMESTAMP NOT NULL,
    updated_at       TIMESTAMP
);

CREATE TABLE feedbacks (
    feedback_id BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL REFERENCES users (id),
    content     TEXT NOT NULL,
    rating      INT,
    created_at  TIMESTAMP NOT NULL
);

CREATE TABLE files (
    id          BIGSERIAL PRIMARY KEY,
    file_name   VARCHAR(255) NOT NULL,
    file_path   VARCHAR(255) NOT NULL,
    file_type   VARCHAR(255),
    upload_date TIMESTAMP NOT NULL,
    file_size   BIGINT,
    workshop_id BIGINT REFERENCES workshops (workshop_id)
);

CREATE TABLE workshop_files (
    id           BIGSERIAL PRIMARY KEY,
    workshop_id  BIGINT NOT NULL REFERENCES workshops (workshop_id),
    filename     VARCHAR(255) NOT NULL,
    file_url     VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    file_size    BIGINT,
    uploaded_at  TIMESTAMP NOT NULL
);

CREATE TABLE notifications (
    notification_id  BIGSERIAL PRIMARY KEY,
    title             VARCHAR(255) NOT NULL,
    message           TEXT NOT NULL,
    notification_type VARCHAR(255),
    created_at        TIMESTAMP NOT NULL
);

CREATE TABLE user_notifications (
    user_notification_id BIGSERIAL PRIMARY KEY,
    user_id               BIGINT NOT NULL REFERENCES users (id),
    notification_id       BIGINT NOT NULL REFERENCES notifications (notification_id),
    is_read               BOOLEAN DEFAULT FALSE,
    read_at               TIMESTAMP
);
