-- V2__simplify_email_query_and_index.sql
-- User Service — Simplificação de query e índice de email
-- Date: 2026-04-20
--
-- CONTEXTO:
--   Sprint 4a normalizou emails no Value Object (Email VO) antes de persistir:
--   emails são sempre armazenados em lowercase. A query com LOWER() em
--   JpaUserSpringRepository.findByEmailIgnoreCase() se tornou redundante —
--   o banco só recebe emails já normalizados.
--
-- PROBLEMA ANTERIOR:
--   findByEmailIgnoreCase usava: WHERE LOWER(email) = LOWER(:param)
--   O índice B-tree em email (idx_users_email) não cobria essa expressão,
--   causando Seq Scan na tabela users a cada login.
--
-- SOLUÇÃO:
--   - Email normalizado no VO elimina a necessidade de LOWER() na query.
--   - findByEmail (WHERE email = ?) usa o índice B-tree diretamente.
--   - idx_users_email existente já é adequado — não há necessidade de recriar.
--   - A constraint UNIQUE em email (definida em V1) já garante unicidade.
--
-- RESULTADO:
--   Login e lookup de email passam de Seq Scan para Index Scan via idx_users_email.
--
-- AÇÃO:
--   Esta migration não altera DDL — documenta a mudança de comportamento e
--   atualiza o comentário da coluna para refletir a normalização upstream.

COMMENT ON COLUMN users.email IS
    'Email address normalizado em lowercase pelo Email VO antes de persistir. '
    'Lookup via findByEmail usa índice B-tree (idx_users_email) sem LOWER().';
