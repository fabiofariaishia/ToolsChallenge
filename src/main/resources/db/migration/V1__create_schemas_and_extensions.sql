-- =============================================================================
-- Migration V1: Criar schemas e extensões
-- =============================================================================
-- Descrição: Cria os schemas separados por bounded context e extensões 
--            necessárias para UUID, busca textual e timezone
-- Autor: Fabio Faria

-- =============================================================================

-- Criar extensões (já criadas no init.sql, mas garantimos aqui também)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Configurar timezone padrão
SET timezone = 'America/Sao_Paulo';

-- Schema para bounded context Pagamento
CREATE SCHEMA IF NOT EXISTS pagamento;
COMMENT ON SCHEMA pagamento IS 'Bounded Context: Pagamento - Processamento de transações de pagamento com cartão de crédito';

-- Schema para bounded context Estorno
CREATE SCHEMA IF NOT EXISTS estorno;
COMMENT ON SCHEMA estorno IS 'Bounded Context: Estorno - Processamento de cancelamentos de pagamentos autorizados';

-- Schema para infraestrutura compartilhada (outbox, idempotencia, auditoria)
CREATE SCHEMA IF NOT EXISTS infra;
COMMENT ON SCHEMA infra IS 'Infraestrutura compartilhada: Outbox pattern, idempotência, auditoria e eventos';

-- Log de criação
DO $$
BEGIN
    RAISE NOTICE 'Schemas criados: pagamento, estorno, infra';
    RAISE NOTICE 'Extensões: uuid-ossp, pg_trgm';
    RAISE NOTICE 'Timezone: America/Sao_Paulo';
END $$;
