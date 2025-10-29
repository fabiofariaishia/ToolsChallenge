-- =============================================================================
-- Migration V2: Tabelas do Bounded Context Pagamento
-- =============================================================================
-- Descrição: Cria as tabelas principais do contexto de Pagamento
-- Autor: Fabio Faria

-- =============================================================================

-- Tipo ENUM para Status do Pagamento
CREATE TYPE pagamento.status_pagamento AS ENUM ('PENDENTE', 'AUTORIZADO', 'NEGADO');

-- Tipo ENUM para Tipo de Pagamento
CREATE TYPE pagamento.tipo_pagamento AS ENUM ('AVISTA', 'PARCELADO_LOJA', 'PARCELADO_EMISSOR');

-- Tabela principal de Pagamentos
CREATE TABLE pagamento.pagamento (
    -- Chave primária técnica (autoincrement)
    id BIGSERIAL PRIMARY KEY,
    
    -- ID da transação (chave de negócio, única, imutável)
    id_transacao VARCHAR(50) NOT NULL UNIQUE,
    
    -- Status do pagamento
    status pagamento.status_pagamento NOT NULL DEFAULT 'PENDENTE',
    
    -- Dados financeiros
    valor DECIMAL(15,2) NOT NULL CHECK (valor > 0),
    moeda CHAR(3) NOT NULL DEFAULT 'BRL',
    
    -- Data/hora da transação (armazenada em UTC)
    data_hora TIMESTAMP WITH TIME ZONE NOT NULL,
    
    -- Dados do estabelecimento
    estabelecimento VARCHAR(255) NOT NULL,
    
    -- Tipo e forma de pagamento
    tipo_pagamento pagamento.tipo_pagamento NOT NULL,
    parcelas INTEGER NOT NULL CHECK (parcelas >= 1 AND parcelas <= 12),
    
    -- NSU e Código de Autorização (retornados pelo adquirente)
    nsu VARCHAR(10) UNIQUE,
    codigo_autorizacao VARCHAR(9) UNIQUE,
    
    -- Dados do cartão (SEMPRE mascarado, PAN completo NUNCA armazenado)
    cartao_mascarado VARCHAR(20) NOT NULL,
    
    -- Snowflake ID para geração de NSU (time-sortable, único)
    snowflake_id BIGINT UNIQUE,
    
    -- Auditoria
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints de validação
    CONSTRAINT chk_valor_positivo CHECK (valor > 0),
    CONSTRAINT chk_parcelas_validas CHECK (
        (tipo_pagamento = 'AVISTA' AND parcelas = 1) OR
        (tipo_pagamento IN ('PARCELADO_LOJA', 'PARCELADO_EMISSOR') AND parcelas >= 2)
    ),
    CONSTRAINT chk_moeda_iso4217 CHECK (moeda ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_cartao_mascarado CHECK (cartao_mascarado ~ '^\d{4}\*+\d{4}$')
);

-- Comentários na tabela e colunas
COMMENT ON TABLE pagamento.pagamento IS 'Transações de pagamento com cartão de crédito';
COMMENT ON COLUMN pagamento.pagamento.id_transacao IS 'Identificador único da transação (chave de negócio)';
COMMENT ON COLUMN pagamento.pagamento.status IS 'Status do pagamento: PENDENTE, AUTORIZADO, NEGADO';
COMMENT ON COLUMN pagamento.pagamento.valor IS 'Valor da transação (DECIMAL 15,2)';
COMMENT ON COLUMN pagamento.pagamento.nsu IS 'Número Sequencial Único (10 dígitos, gerado por Snowflake)';
COMMENT ON COLUMN pagamento.pagamento.codigo_autorizacao IS 'Código de autorização (9 dígitos com Luhn check)';
COMMENT ON COLUMN pagamento.pagamento.cartao_mascarado IS 'Cartão mascarado no formato: 4444********1234';
COMMENT ON COLUMN pagamento.pagamento.snowflake_id IS 'Snowflake ID para geração de NSU time-sortable';

-- Índices para performance
CREATE INDEX idx_pagamento_id_transacao ON pagamento.pagamento(id_transacao);
CREATE INDEX idx_pagamento_status ON pagamento.pagamento(status);
CREATE INDEX idx_pagamento_data_hora ON pagamento.pagamento(data_hora DESC);
CREATE INDEX idx_pagamento_estabelecimento ON pagamento.pagamento(estabelecimento);
CREATE INDEX idx_pagamento_nsu ON pagamento.pagamento(nsu) WHERE nsu IS NOT NULL;
CREATE INDEX idx_pagamento_criado_em ON pagamento.pagamento(criado_em DESC);

-- Índice composto para queries de filtro + paginação
CREATE INDEX idx_pagamento_filtros ON pagamento.pagamento(status, estabelecimento, data_hora DESC);

-- Trigger para atualizar automaticamente a coluna atualizado_em
CREATE OR REPLACE FUNCTION pagamento.atualizar_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.atualizado_em = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_pagamento_atualizar_timestamp
    BEFORE UPDATE ON pagamento.pagamento
    FOR EACH ROW
    EXECUTE FUNCTION pagamento.atualizar_timestamp();

-- Log de criação
DO $$
BEGIN
    RAISE NOTICE 'Tabela pagamento.pagamento criada com sucesso';
    RAISE NOTICE 'Índices criados para: id_transacao, status, data_hora, estabelecimento, nsu';
    RAISE NOTICE 'Trigger de auditoria configurado';
END $$;
