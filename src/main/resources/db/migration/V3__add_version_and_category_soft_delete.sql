-- =============================================================
-- V3__add_version_and_category_soft_delete.sql
-- Adds optimistic-locking version column to financial_records
-- and soft-delete support to categories.
-- =============================================================

-- ── Optimistic locking on financial_records ───────────────────
ALTER TABLE financial_records
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;

-- ── Soft-delete support for categories ───────────────────────
ALTER TABLE categories
    ADD COLUMN IF NOT EXISTS deleted_at TIMESTAMPTZ NULL;

-- Partial index: only active (non-deleted) categories
CREATE INDEX IF NOT EXISTS idx_categories_active
    ON categories (name)
    WHERE deleted_at IS NULL;
