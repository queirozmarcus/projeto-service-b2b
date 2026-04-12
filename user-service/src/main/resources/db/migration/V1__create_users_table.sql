-- V1__create_users_table.sql
-- User Service — Schema Próprio (DB-per-service migration)
-- Date: 2026-04-12
--
-- CONTEXTO:
--   Esta migration cria o schema do user-service em seu banco exclusivo (scopeflow_users).
--   A estrutura é idêntica à tabela users do monólito (V2 + V4 do monólito), garantindo
--   compatibilidade total durante o período de cut-over (dual-write / data sync).
--
-- BASELINE:
--   Quando aplicada sobre banco que já contém dados migrados via pg_dump/restore,
--   usar baseline-on-migrate: true com baseline-version: 0. O Flyway marcará esta
--   migration como aplicada sem re-executá-la.
--
-- REFERÊNCIAS NO MONÓLITO:
--   - V2__user_workspace_domain_schema.sql: DDL original da tabela users + índices
--   - V4__proposal_domain_schema.sql: ALTER TABLE users ADD COLUMN version BIGINT

-- ============================================================================
-- USERS TABLE
-- ============================================================================
CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    email         VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name     VARCHAR(255) NOT NULL,
    phone         VARCHAR(20),
    status        VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE'
                      CHECK (status IN ('ACTIVE', 'INACTIVE', 'DELETED')),
    created_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    -- Optimistic locking counter (JPA @Version). Added by monolith V4 migration.
    version       BIGINT       NOT NULL DEFAULT 0
);

-- ============================================================================
-- INDEXES
-- ============================================================================
-- Lookup por email (path crítico: login, registro, deduplicação)
CREATE INDEX idx_users_email      ON users(email);

-- Filtros administrativos e soft-delete queries
CREATE INDEX idx_users_status     ON users(status);

-- Paginação e auditoria por data de criação
CREATE INDEX idx_users_created_at ON users(created_at);

-- ============================================================================
-- COMMENTS
-- ============================================================================
COMMENT ON TABLE users IS
    'User accounts with authentication credentials. Owner: user-service (source of truth post-cutover).';

COMMENT ON COLUMN users.email IS
    'Unique email address (case-insensitive lookup via normalized form)';

COMMENT ON COLUMN users.password_hash IS
    'BCrypt hashed password (never stored plaintext)';

COMMENT ON COLUMN users.status IS
    'User state: ACTIVE (can login), INACTIVE (invited, not confirmed), DELETED (soft-deleted, GDPR)';

COMMENT ON COLUMN users.version IS
    'Optimistic locking counter (JPA @Version). Prevents lost updates under concurrent edits.';

-- ============================================================================
-- FIM DA MIGRATION V1
-- ============================================================================
