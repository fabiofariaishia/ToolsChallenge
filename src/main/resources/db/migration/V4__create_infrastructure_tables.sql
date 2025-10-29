-- =============================================================================
-- Migration V4: Tabelas de Infraestrutura (Outbox, Idempotência, Auditoria)
-- =============================================================================
-- Descrição: Cria as tabelas de infraestrutura compartilhada
-- Autor: Fabio Faria

-- =============================================================================

-- ============================================
-- Tabela: Outbox Pattern (Transactional Outbox)
-- ============================================
-- Garante que eventos sejam publicados no Kafka de forma transacional
-- Job assíncrono processa registros PENDENTE a cada 500ms

CREATE TABLE infra.outbox (
    -- Chave primária técnica
    id BIGSERIAL PRIMARY KEY,
    
    -- ID do agregado (ex: id_transacao)
    agregado_id VARCHAR(50) NOT NULL,
    
    -- Tipo do agregado (ex: Pagamento, Estorno)
    agregado_tipo VARCHAR(50) NOT NULL,
    
    -- Tipo do evento (ex: PagamentoAutorizado, EstornoCancelado)
    evento_tipo VARCHAR(100) NOT NULL,
    
    -- Payload do evento em JSON
    payload JSONB NOT NULL,
    
    -- Tópico Kafka de destino
    topico_kafka VARCHAR(100) NOT NULL,
    
    -- Status do processamento
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE' 
        CHECK (status IN ('PENDENTE', 'PROCESSADO', 'ERRO')),
    
    -- Número de tentativas de envio
    tentativas INTEGER NOT NULL DEFAULT 0,
    
    -- Última mensagem de erro (se houver)
    ultimo_erro TEXT,
    
    -- Timestamps
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processado_em TIMESTAMP WITH TIME ZONE,
    
    -- Constraint: payload deve ser JSON válido
    CONSTRAINT chk_outbox_payload_json CHECK (jsonb_typeof(payload) = 'object')
);

COMMENT ON TABLE infra.outbox IS 'Outbox Pattern: eventos pendentes para publicação no Kafka';
COMMENT ON COLUMN infra.outbox.agregado_id IS 'ID do agregado que gerou o evento (ex: id_transacao)';
COMMENT ON COLUMN infra.outbox.evento_tipo IS 'Tipo do evento de domínio (ex: PagamentoAutorizado)';
COMMENT ON COLUMN infra.outbox.payload IS 'Payload do evento em formato JSON';
COMMENT ON COLUMN infra.outbox.tentativas IS 'Número de tentativas de publicação no Kafka';

-- Índices para performance do job de processamento
CREATE INDEX idx_outbox_status_pendente ON infra.outbox(status, criado_em) 
    WHERE status = 'PENDENTE';
CREATE INDEX idx_outbox_agregado ON infra.outbox(agregado_tipo, agregado_id);
CREATE INDEX idx_outbox_criado_em ON infra.outbox(criado_em DESC);

-- ============================================
-- Tabela: Idempotência (Chave-Idempotencia)
-- ============================================
-- Armazena chaves de idempotência para prevenir processamento duplicado
-- TTL: 24h (mesma janela de estorno)
-- Fallback para quando Redis estiver indisponível

CREATE TABLE infra.idempotencia (
    -- Chave de idempotência (fornecida no header)
    chave VARCHAR(100) PRIMARY KEY,
    
    -- ID da transação processada
    id_transacao VARCHAR(50) NOT NULL,
    
    -- Endpoint que processou (ex: POST /pagamentos)
    endpoint VARCHAR(100) NOT NULL,
    
    -- Response HTTP status code
    status_http INTEGER NOT NULL,
    
    -- Response body (para retornar idêntico em caso de retry)
    response_body JSONB,
    
    -- Timestamps
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expira_em TIMESTAMP WITH TIME ZONE NOT NULL,
    
    -- Constraint: expiração deve ser no futuro
    CONSTRAINT chk_idempotencia_expiracao CHECK (expira_em > criado_em)
);

COMMENT ON TABLE infra.idempotencia IS 'Chaves de idempotência (fallback quando Redis indisponível, TTL 24h)';
COMMENT ON COLUMN infra.idempotencia.chave IS 'Chave de idempotência fornecida no header Chave-Idempotencia';
COMMENT ON COLUMN infra.idempotencia.response_body IS 'Response original para retornar em caso de retry';

-- Índice para limpeza de registros expirados
CREATE INDEX idx_idempotencia_expiracao ON infra.idempotencia(expira_em);
CREATE INDEX idx_idempotencia_id_transacao ON infra.idempotencia(id_transacao);

-- ============================================
-- Tabela: Auditoria de Eventos
-- ============================================
-- Log de todos os eventos importantes do sistema
-- Útil para troubleshooting e compliance

CREATE TABLE infra.evento_auditoria (
    -- Chave primária técnica
    id BIGSERIAL PRIMARY KEY,
    
    -- Tipo do evento
    evento_tipo VARCHAR(100) NOT NULL,
    
    -- Agregado relacionado
    agregado_tipo VARCHAR(50),
    agregado_id VARCHAR(50),
    
    -- Usuário/sistema que executou a ação
    usuario VARCHAR(100),
    
    -- Dados do evento em JSON
    dados JSONB,
    
    -- Metadados adicionais (ex: IP, User-Agent)
    metadados JSONB,
    
    -- Timestamp
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE infra.evento_auditoria IS 'Log de auditoria de eventos do sistema';
COMMENT ON COLUMN infra.evento_auditoria.evento_tipo IS 'Tipo do evento auditado';
COMMENT ON COLUMN infra.evento_auditoria.dados IS 'Dados do evento em formato JSON';

-- Índices para consultas de auditoria
CREATE INDEX idx_evento_auditoria_tipo ON infra.evento_auditoria(evento_tipo);
CREATE INDEX idx_evento_auditoria_agregado ON infra.evento_auditoria(agregado_tipo, agregado_id);
CREATE INDEX idx_evento_auditoria_criado_em ON infra.evento_auditoria(criado_em DESC);
CREATE INDEX idx_evento_auditoria_usuario ON infra.evento_auditoria(usuario);

-- ============================================
-- Funções auxiliares
-- ============================================

-- Função para limpar registros de idempotência expirados
-- Deve ser executada periodicamente (ex: cron job diário)
CREATE OR REPLACE FUNCTION infra.limpar_idempotencia_expirada()
RETURNS INTEGER AS $$
DECLARE
    linhas_deletadas INTEGER;
BEGIN
    DELETE FROM infra.idempotencia
    WHERE expira_em < CURRENT_TIMESTAMP;
    
    GET DIAGNOSTICS linhas_deletadas = ROW_COUNT;
    
    RAISE NOTICE 'Registros de idempotência expirados removidos: %', linhas_deletadas;
    RETURN linhas_deletadas;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION infra.limpar_idempotencia_expirada() IS 'Remove registros de idempotência expirados (TTL 24h)';

-- Função para limpar eventos do outbox já processados (após 7 dias)
CREATE OR REPLACE FUNCTION infra.limpar_outbox_processados()
RETURNS INTEGER AS $$
DECLARE
    linhas_deletadas INTEGER;
BEGIN
    DELETE FROM infra.outbox
    WHERE status = 'PROCESSADO' 
    AND processado_em < CURRENT_TIMESTAMP - INTERVAL '7 days';
    
    GET DIAGNOSTICS linhas_deletadas = ROW_COUNT;
    
    RAISE NOTICE 'Eventos do outbox processados removidos: %', linhas_deletadas;
    RETURN linhas_deletadas;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION infra.limpar_outbox_processados() IS 'Remove eventos do outbox processados há mais de 7 dias';

-- Log de criação
DO $$
BEGIN
    RAISE NOTICE 'Tabelas de infraestrutura criadas:';
    RAISE NOTICE '  - infra.outbox (Transactional Outbox Pattern)';
    RAISE NOTICE '  - infra.idempotencia (fallback Redis, TTL 24h)';
    RAISE NOTICE '  - infra.evento_auditoria (log de eventos)';
    RAISE NOTICE 'Funções de limpeza criadas: limpar_idempotencia_expirada, limpar_outbox_processados';
END $$;
