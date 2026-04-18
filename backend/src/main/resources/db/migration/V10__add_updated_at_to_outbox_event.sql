-- V10: Adiciona coluna updated_at à tabela outbox_event
-- Problema: OutboxEvent JPA entity declara campo updatedAt mapeado para updated_at,
-- mas a coluna não existe no schema (V5). Isso causa falha no Hibernate ddl-auto=validate.
-- Solução: adicionar a coluna com valor default para não quebrar dados existentes.

ALTER TABLE outbox_event
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Preenche updated_at com created_at para registros existentes (sem dado real disponível)
UPDATE outbox_event SET updated_at = created_at WHERE updated_at = CURRENT_TIMESTAMP AND created_at < CURRENT_TIMESTAMP;

COMMENT ON COLUMN outbox_event.updated_at IS
'Timestamp da última atualização do evento (normalmente quando published_at é setado).
Adicionado em V10 para alinhar schema com entidade JPA OutboxEvent.';
