-- =============================================================
-- V1__init.sql  –  Finance Dashboard initial schema
-- =============================================================

-- ── Extensions ───────────────────────────────────────────────
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ── users ────────────────────────────────────────────────────
CREATE TABLE users (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE'
                                CHECK (status IN ('ACTIVE','INACTIVE')),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ── roles ─────────────────────────────────────────────────────
CREATE TABLE roles (
    id   SMALLSERIAL  PRIMARY KEY,
    name VARCHAR(50)  NOT NULL UNIQUE
);

-- ── user_roles ────────────────────────────────────────────────
CREATE TABLE user_roles (
    user_id UUID     NOT NULL REFERENCES users(id)  ON DELETE CASCADE,
    role_id SMALLINT NOT NULL REFERENCES roles(id)  ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- ── categories ───────────────────────────────────────────────
CREATE TABLE categories (
    id         UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name       VARCHAR(100) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- ── financial_records ─────────────────────────────────────────
CREATE TABLE financial_records (
    id          UUID           PRIMARY KEY DEFAULT gen_random_uuid(),
    record_date DATE           NOT NULL,
    type        VARCHAR(10)    NOT NULL CHECK (type IN ('INCOME','EXPENSE')),
    category_id UUID           REFERENCES categories(id) ON DELETE SET NULL,
    amount      NUMERIC(19,2)  NOT NULL CHECK (amount > 0),
    currency    VARCHAR(3)     NOT NULL DEFAULT 'USD',
    notes       TEXT,
    created_by  UUID           NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ    NOT NULL DEFAULT now(),
    deleted_at  TIMESTAMPTZ    NULL
);

-- ── audit_log ────────────────────────────────────────────────
CREATE TABLE audit_log (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id UUID        NOT NULL REFERENCES users(id),
    action        VARCHAR(100) NOT NULL,
    entity_type   VARCHAR(100) NOT NULL,
    entity_id     UUID        NULL,
    metadata      JSONB       NULL,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ── Indexes ───────────────────────────────────────────────────
CREATE INDEX idx_records_record_date
    ON financial_records (record_date);

CREATE INDEX idx_records_type_date
    ON financial_records (type, record_date);

CREATE INDEX idx_records_category_date
    ON financial_records (category_id, record_date);

-- Partial index: queries on non-deleted records
CREATE INDEX idx_records_active_date
    ON financial_records (record_date)
    WHERE deleted_at IS NULL;

CREATE INDEX idx_audit_actor
    ON audit_log (actor_user_id, created_at DESC);

CREATE INDEX idx_audit_entity
    ON audit_log (entity_type, entity_id);
