-- =============================================================================
-- Migration V5: Dados de Teste e Seed (Desenvolvimento)
-- =============================================================================
-- Descrição: Insere dados de exemplo para facilitar testes em desenvolvimento
--            (Comentar/remover em produção)
-- Autor: Fabio Faria

-- =============================================================================

-- NOTA: Esta migration é opcional e deve ser executada apenas em ambiente de desenvolvimento
-- Em produção, comentar ou não executar esta migration

-- ============================================
-- Dados de exemplo: Pagamentos
-- ============================================

-- Pagamento AUTORIZADO à vista
INSERT INTO pagamento.pagamento (
    id_transacao, 
    status, 
    valor, 
    moeda, 
    data_hora, 
    estabelecimento, 
    tipo_pagamento, 
    parcelas, 
    nsu, 
    codigo_autorizacao, 
    cartao_mascarado,
    snowflake_id
) VALUES (
    'TXN-001-2025-AVISTA',
    'AUTORIZADO',
    150.50,
    'BRL',
    CURRENT_TIMESTAMP - INTERVAL '2 hours',
    'Supermercado Exemplo LTDA',
    'AVISTA',
    1,
    '1234567890',
    '987654321',
    '4444********1234',
    123456789012345
);

-- Pagamento AUTORIZADO parcelado loja (3x)
INSERT INTO pagamento.pagamento (
    id_transacao, 
    status, 
    valor, 
    moeda, 
    data_hora, 
    estabelecimento, 
    tipo_pagamento, 
    parcelas, 
    nsu, 
    codigo_autorizacao, 
    cartao_mascarado,
    snowflake_id
) VALUES (
    'TXN-002-2025-PARCELADO',
    'AUTORIZADO',
    900.00,
    'BRL',
    CURRENT_TIMESTAMP - INTERVAL '1 hour',
    'Loja de Eletrônicos XYZ',
    'PARCELADO_LOJA',
    3,
    '2345678901',
    '876543210',
    '5555********6789',
    123456789012346
);

-- Pagamento PENDENTE (aguardando processamento)
INSERT INTO pagamento.pagamento (
    id_transacao, 
    status, 
    valor, 
    moeda, 
    data_hora, 
    estabelecimento, 
    tipo_pagamento, 
    parcelas, 
    cartao_mascarado,
    snowflake_id
) VALUES (
    'TXN-003-2025-PENDENTE',
    'PENDENTE',
    75.00,
    'BRL',
    CURRENT_TIMESTAMP - INTERVAL '30 minutes',
    'Restaurante ABC',
    'AVISTA',
    1,
    '6011********9876',
    123456789012347
);

-- Pagamento NEGADO
INSERT INTO pagamento.pagamento (
    id_transacao, 
    status, 
    valor, 
    moeda, 
    data_hora, 
    estabelecimento, 
    tipo_pagamento, 
    parcelas, 
    cartao_mascarado,
    snowflake_id
) VALUES (
    'TXN-004-2025-NEGADO',
    'NEGADO',
    1200.00,
    'BRL',
    CURRENT_TIMESTAMP - INTERVAL '15 minutes',
    'Loja de Móveis DEF',
    'PARCELADO_EMISSOR',
    6,
    '3782********5678',
    123456789012348
);

-- ============================================
-- Dados de exemplo: Estorno
-- ============================================

-- Estorno CANCELADO (bem-sucedido) do primeiro pagamento
INSERT INTO estorno.estorno (
    id_transacao,
    id_estorno,
    status,
    valor,
    data_hora,
    nsu,
    codigo_autorizacao,
    motivo,
    snowflake_id
) VALUES (
    'TXN-001-2025-AVISTA',
    'EST-001-2025',
    'CANCELADO',
    150.50,
    CURRENT_TIMESTAMP - INTERVAL '30 minutes',
    '9876543210',
    '123456789',
    'Solicitação do cliente - produto com defeito',
    987654321012345
);

-- Estorno PENDENTE
INSERT INTO estorno.estorno (
    id_transacao,
    id_estorno,
    status,
    valor,
    data_hora,
    motivo,
    snowflake_id
) VALUES (
    'TXN-002-2025-PARCELADO',
    'EST-002-2025',
    'PENDENTE',
    900.00,
    CURRENT_TIMESTAMP - INTERVAL '5 minutes',
    'Cliente desistiu da compra',
    987654321012346
);

-- ============================================
-- Dados de exemplo: Outbox (eventos pendentes)
-- ============================================

INSERT INTO infra.outbox (
    agregado_id,
    agregado_tipo,
    evento_tipo,
    payload,
    topico_kafka,
    status
) VALUES (
    'TXN-001-2025-AVISTA',
    'Pagamento',
    'PagamentoAutorizado',
    '{"idTransacao":"TXN-001-2025-AVISTA","valor":150.50,"status":"AUTORIZADO","nsu":"1234567890"}'::jsonb,
    'pagamento.eventos',
    'PENDENTE'
);

INSERT INTO infra.outbox (
    agregado_id,
    agregado_tipo,
    evento_tipo,
    payload,
    topico_kafka,
    status
) VALUES (
    'EST-001-2025',
    'Estorno',
    'EstornoCancelado',
    '{"idEstorno":"EST-001-2025","idTransacao":"TXN-001-2025-AVISTA","valor":150.50,"status":"CANCELADO"}'::jsonb,
    'estorno.eventos',
    'PENDENTE'
);

-- ============================================
-- Dados de exemplo: Idempotência
-- ============================================

INSERT INTO infra.idempotencia (
    chave,
    id_transacao,
    endpoint,
    status_http,
    response_body,
    expira_em
) VALUES (
    'IDMP-12345-67890-ABCDEF',
    'TXN-001-2025-AVISTA',
    'POST /pagamentos',
    201,
    '{"idTransacao":"TXN-001-2025-AVISTA","status":"AUTORIZADO","nsu":"1234567890"}'::jsonb,
    CURRENT_TIMESTAMP + INTERVAL '24 hours'
);

-- ============================================
-- Dados de exemplo: Auditoria
-- ============================================

INSERT INTO infra.evento_auditoria (
    evento_tipo,
    agregado_tipo,
    agregado_id,
    usuario,
    dados,
    metadados
) VALUES (
    'PagamentoCriado',
    'Pagamento',
    'TXN-001-2025-AVISTA',
    'sistema',
    '{"valor":150.50,"estabelecimento":"Supermercado Exemplo LTDA"}'::jsonb,
    '{"ip":"192.168.1.100","userAgent":"PostmanRuntime/7.32.0"}'::jsonb
);

INSERT INTO infra.evento_auditoria (
    evento_tipo,
    agregado_tipo,
    agregado_id,
    usuario,
    dados,
    metadados
) VALUES (
    'EstornoSolicitado',
    'Estorno',
    'EST-001-2025',
    'operador@exemplo.com',
    '{"idTransacao":"TXN-001-2025-AVISTA","valor":150.50,"motivo":"Solicitação do cliente"}'::jsonb,
    '{"ip":"192.168.1.101","userAgent":"Mozilla/5.0"}'::jsonb
);

-- Log de criação
DO $$
BEGIN
    RAISE NOTICE 'Dados de exemplo inseridos:';
    RAISE NOTICE '  - 4 pagamentos (AUTORIZADO, PENDENTE, NEGADO)';
    RAISE NOTICE '  - 2 estornos (CANCELADO, PENDENTE)';
    RAISE NOTICE '  - 2 eventos no outbox';
    RAISE NOTICE '  - 1 chave de idempotência';
    RAISE NOTICE '  - 2 eventos de auditoria';
    RAISE NOTICE '';
    RAISE NOTICE '⚠️  ATENÇÃO: Remover esta migration em produção!';
END $$;
