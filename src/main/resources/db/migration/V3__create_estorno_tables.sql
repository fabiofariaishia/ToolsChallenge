-- =============================================================================
-- Migration V3: Tabelas do Bounded Context Estorno
-- =============================================================================
-- Descrição: Cria as tabelas principais do contexto de Estorno
-- Autor: Fabio Faria

-- =============================================================================

-- Tabela principal de Estornos
CREATE TABLE estorno.estorno (
    -- Chave primária técnica (autoincrement)
    id BIGSERIAL PRIMARY KEY,
    
    -- ID da transação original (referencia o pagamento)
    id_transacao VARCHAR(50) NOT NULL,
    
    -- ID único do estorno
    id_estorno VARCHAR(50) NOT NULL UNIQUE,
    
    -- Status do estorno
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE' CHECK (status IN ('PENDENTE', 'CANCELADO', 'NEGADO')),
    
    -- Valor a ser estornado (sempre valor total do pagamento)
    valor DECIMAL(15,2) NOT NULL CHECK (valor > 0),
    
    -- Data/hora da solicitação do estorno
    data_hora TIMESTAMP WITH TIME ZONE NOT NULL,
    
    -- NSU e Código de Autorização do estorno
    nsu VARCHAR(10) UNIQUE,
    codigo_autorizacao VARCHAR(9) UNIQUE,
    
    -- Motivo do estorno (opcional)
    motivo TEXT,
    
    -- Snowflake ID para geração de NSU do estorno
    snowflake_id BIGINT UNIQUE,
    
    -- Auditoria
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints de validação
    CONSTRAINT chk_estorno_valor_positivo CHECK (valor > 0),
    
    -- Foreign key para a tabela de pagamentos
    -- NOTA: Como os schemas são separados, usamos referência qualificada
    CONSTRAINT fk_estorno_pagamento 
        FOREIGN KEY (id_transacao) 
        REFERENCES pagamento.pagamento(id_transacao)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);

-- Comentários na tabela e colunas
COMMENT ON TABLE estorno.estorno IS 'Estornos de pagamentos autorizados (janela 24h, valor total)';
COMMENT ON COLUMN estorno.estorno.id_transacao IS 'ID da transação original do pagamento';
COMMENT ON COLUMN estorno.estorno.id_estorno IS 'Identificador único do estorno';
COMMENT ON COLUMN estorno.estorno.status IS 'Status do estorno: PENDENTE, CANCELADO, NEGADO';
COMMENT ON COLUMN estorno.estorno.valor IS 'Valor do estorno (sempre valor total do pagamento)';
COMMENT ON COLUMN estorno.estorno.nsu IS 'NSU do estorno (gerado pelo adquirente)';
COMMENT ON COLUMN estorno.estorno.motivo IS 'Motivo/justificativa do estorno (opcional)';

-- Índices para performance
CREATE INDEX idx_estorno_id_transacao ON estorno.estorno(id_transacao);
CREATE INDEX idx_estorno_id_estorno ON estorno.estorno(id_estorno);
CREATE INDEX idx_estorno_status ON estorno.estorno(status);
CREATE INDEX idx_estorno_data_hora ON estorno.estorno(data_hora DESC);
CREATE INDEX idx_estorno_criado_em ON estorno.estorno(criado_em DESC);

-- Índice para busca de estornos por pagamento
CREATE INDEX idx_estorno_por_pagamento ON estorno.estorno(id_transacao, status);

-- Trigger para atualizar automaticamente a coluna atualizado_em
CREATE OR REPLACE FUNCTION estorno.atualizar_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.atualizado_em = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_estorno_atualizar_timestamp
    BEFORE UPDATE ON estorno.estorno
    FOR EACH ROW
    EXECUTE FUNCTION estorno.atualizar_timestamp();

-- Constraint: Apenas 1 estorno bem-sucedido por pagamento
-- (permite múltiplas tentativas NEGADAS, mas apenas 1 CANCELADO)
CREATE UNIQUE INDEX idx_estorno_unico_cancelado 
    ON estorno.estorno(id_transacao) 
    WHERE status = 'CANCELADO';

COMMENT ON INDEX estorno.idx_estorno_unico_cancelado IS 'Garante apenas 1 estorno CANCELADO por pagamento';

-- Log de criação
DO $$
BEGIN
    RAISE NOTICE 'Tabela estorno.estorno criada com sucesso';
    RAISE NOTICE 'Foreign key configurada: estorno -> pagamento';
    RAISE NOTICE 'Constraint única: apenas 1 estorno CANCELADO por pagamento';
    RAISE NOTICE 'Índices criados para: id_transacao, id_estorno, status, data_hora';
END $$;
