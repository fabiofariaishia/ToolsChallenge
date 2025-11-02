-- ============================================================================
-- Migration: V6__add_tentativas_reprocessamento.sql
-- Descrição: Adiciona campo de controle de tentativas de reprocessamento
--            nas tabelas de Pagamento e Estorno
-- Autor: ToolsChallenge Team
-- Data: 2025-11-02
-- ============================================================================

-- Adiciona coluna tentativas_reprocessamento na tabela pagamento
-- Usado pelo scheduler para implementar Dead Letter Queue (DLQ)
ALTER TABLE pagamento.pagamento 
ADD COLUMN tentativas_reprocessamento INTEGER NOT NULL DEFAULT 0;

-- Adiciona comentário para documentação
COMMENT ON COLUMN pagamento.pagamento.tentativas_reprocessamento IS 
'Contador de tentativas de reprocessamento para transações PENDENTE. Incrementado a cada execução do scheduler. Usado para DLQ após atingir max_tentativas configurado.';

-- Adiciona coluna tentativas_reprocessamento na tabela estorno
-- Usado pelo scheduler para implementar Dead Letter Queue (DLQ)
ALTER TABLE estorno.estorno 
ADD COLUMN tentativas_reprocessamento INTEGER NOT NULL DEFAULT 0;

-- Adiciona comentário para documentação
COMMENT ON COLUMN estorno.estorno.tentativas_reprocessamento IS 
'Contador de tentativas de reprocessamento para estornos PENDENTE. Incrementado a cada execução do scheduler. Usado para DLQ após atingir max_tentativas configurado.';

-- Adiciona índice composto para otimizar query do scheduler
-- Query: SELECT * FROM pagamento WHERE status = 'PENDENTE' AND tentativas_reprocessamento < max ORDER BY criado_em
CREATE INDEX idx_pagamento_reprocessamento 
ON pagamento.pagamento(status, tentativas_reprocessamento, criado_em) 
WHERE status = 'PENDENTE';

-- Adiciona índice composto para otimizar query do scheduler
-- Query: SELECT * FROM estorno WHERE status = 'PENDENTE' AND tentativas_reprocessamento < max ORDER BY criado_em
CREATE INDEX idx_estorno_reprocessamento 
ON estorno.estorno(status, tentativas_reprocessamento, criado_em) 
WHERE status = 'PENDENTE';

-- ============================================================================
-- Fim da Migration
-- ============================================================================
