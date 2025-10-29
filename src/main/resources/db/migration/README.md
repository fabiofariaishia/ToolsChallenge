# Database Migrations - Flyway

Documentação das migrations do banco de dados PostgreSQL.

## 📋 Estrutura das Migrations

As migrations seguem a convenção do Flyway: `V{numero}__{descricao}.sql`

### Migrations Criadas

| Versão | Arquivo | Descrição |
|--------|---------|-----------|
| V1 | `create_schemas_and_extensions.sql` | Schemas (pagamento, estorno, infra) e extensões |
| V2 | `create_pagamento_tables.sql` | Tabela de pagamentos com índices e triggers |
| V3 | `create_estorno_tables.sql` | Tabela de estornos com relacionamento |
| V4 | `create_infrastructure_tables.sql` | Outbox, idempotência e auditoria |
| V5 | `insert_sample_data.sql` | Dados de exemplo (apenas dev) |

## 🏗️ Arquitetura de Schemas

### Schema: `pagamento`
Bounded context de Pagamento
- Tabela: `pagamento`
- ENUMs: `status_pagamento`, `tipo_pagamento`
- Triggers: auditoria de timestamp

### Schema: `estorno`
Bounded context de Estorno
- Tabela: `estorno`
- ENUMs: `status_estorno`
- Constraints: apenas 1 estorno CANCELADO por pagamento
- Foreign Key: `id_transacao -> pagamento.pagamento`

### Schema: `infra`
Infraestrutura compartilhada
- Tabela: `outbox` - Transactional Outbox Pattern
- Tabela: `idempotencia` - Fallback do Redis (TTL 24h)
- Tabela: `evento_auditoria` - Log de eventos
- Funções: limpeza de dados expirados

## 📊 Diagrama Entidade-Relacionamento

```
┌─────────────────────────────────────────┐
│ pagamento.pagamento                     │
├─────────────────────────────────────────┤
│ PK id (BIGSERIAL)                       │
│ UK id_transacao (VARCHAR 50)            │
│    status (ENUM)                        │
│    valor (DECIMAL 15,2)                 │
│    moeda (CHAR 3)                       │
│    data_hora (TIMESTAMPTZ)              │
│    estabelecimento (VARCHAR 255)        │
│    tipo_pagamento (ENUM)                │
│    parcelas (INTEGER)                   │
│ UK nsu (VARCHAR 10)                     │
│ UK codigo_autorizacao (VARCHAR 9)       │
│    cartao_mascarado (VARCHAR 20)        │
│ UK snowflake_id (BIGINT)                │
│    criado_em, atualizado_em             │
└─────────────────────────────────────────┘
                 △
                 │ FK (id_transacao)
                 │
┌─────────────────────────────────────────┐
│ estorno.estorno                         │
├─────────────────────────────────────────┤
│ PK id (BIGSERIAL)                       │
│ FK id_transacao (VARCHAR 50)            │
│ UK id_estorno (VARCHAR 50)              │
│    status (ENUM)                        │
│    valor (DECIMAL 15,2)                 │
│    data_hora (TIMESTAMPTZ)              │
│ UK nsu (VARCHAR 10)                     │
│ UK codigo_autorizacao (VARCHAR 9)       │
│    motivo (TEXT)                        │
│ UK snowflake_id (BIGINT)                │
│    criado_em, atualizado_em             │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ infra.outbox                            │
├─────────────────────────────────────────┤
│ PK id (BIGSERIAL)                       │
│    agregado_id (VARCHAR 50)             │
│    agregado_tipo (VARCHAR 50)           │
│    evento_tipo (VARCHAR 100)            │
│    payload (JSONB)                      │
│    topico_kafka (VARCHAR 100)           │
│    status (VARCHAR 20)                  │
│    tentativas (INTEGER)                 │
│    ultimo_erro (TEXT)                   │
│    criado_em, processado_em             │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ infra.idempotencia                      │
├─────────────────────────────────────────┤
│ PK chave (VARCHAR 100)                  │
│    id_transacao (VARCHAR 50)            │
│    endpoint (VARCHAR 100)               │
│    status_http (INTEGER)                │
│    response_body (JSONB)                │
│    criado_em, expira_em                 │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ infra.evento_auditoria                  │
├─────────────────────────────────────────┤
│ PK id (BIGSERIAL)                       │
│    evento_tipo (VARCHAR 100)            │
│    agregado_tipo, agregado_id           │
│    usuario (VARCHAR 100)                │
│    dados (JSONB)                        │
│    metadados (JSONB)                    │
│    criado_em                            │
└─────────────────────────────────────────┘
```

## 🔑 Constraints e Regras

### Tabela `pagamento.pagamento`

1. **Valores financeiros**
   - `valor > 0` (CHECK)
   - `moeda` deve seguir ISO-4217 (3 letras maiúsculas)

2. **Parcelas**
   - AVISTA: `parcelas = 1`
   - PARCELADO_LOJA ou PARCELADO_EMISSOR: `parcelas >= 2`
   - Máximo: `parcelas <= 12`

3. **Cartão mascarado**
   - Formato: `^\d{4}\*+\d{4}$` (ex: `4444********1234`)
   - PAN completo NUNCA é armazenado

4. **Unicidade**
   - `id_transacao` (UK)
   - `nsu` (UK)
   - `codigo_autorizacao` (UK)
   - `snowflake_id` (UK)

### Tabela `estorno.estorno`

1. **Valores**
   - `valor > 0` (CHECK)
   - Sempre valor total do pagamento (estorno parcial não permitido)

2. **Relacionamento**
   - FK para `pagamento.pagamento(id_transacao)`
   - ON DELETE RESTRICT (não permite deletar pagamento com estorno)
   - ON UPDATE CASCADE (atualiza id_transacao em cascata)

3. **Unicidade**
   - `id_estorno` (UK)
   - Apenas 1 estorno com `status = 'CANCELADO'` por `id_transacao` (índice parcial)

### Tabela `infra.outbox`

1. **Payload**
   - Deve ser JSON válido (CHECK)
   - Tipo deve ser 'object'

2. **Status**
   - Valores: PENDENTE, PROCESSADO, ERRO

3. **Performance**
   - Índice parcial para `status = 'PENDENTE'` (job de processamento)

### Tabela `infra.idempotencia`

1. **Expiração**
   - `expira_em > criado_em` (CHECK)
   - TTL: 24 horas (mesma janela de estorno)

2. **Response**
   - Armazena `status_http` e `response_body` original
   - Retorna resposta idêntica em caso de retry

## 📈 Índices para Performance

### Pagamento
- `id_transacao` (único, buscas diretas)
- `status` (filtros)
- `data_hora DESC` (ordenação temporal)
- `estabelecimento` (filtros por loja)
- `nsu` (consultas por NSU)
- Composto: `(status, estabelecimento, data_hora DESC)` (queries complexas)

### Estorno
- `id_transacao` (relacionamento com pagamento)
- `id_estorno` (único)
- `status` (filtros)
- `data_hora DESC` (ordenação)
- Composto: `(id_transacao, status)` (estornos por pagamento)

### Outbox
- Parcial: `(status, criado_em) WHERE status = 'PENDENTE'` (job)
- `(agregado_tipo, agregado_id)` (busca de eventos)

### Idempotência
- Parcial: `expira_em WHERE expira_em < CURRENT_TIMESTAMP` (limpeza)
- `id_transacao` (relacionamento)

## 🔧 Triggers e Funções

### Triggers de Auditoria
- `trg_pagamento_atualizar_timestamp` - Atualiza `atualizado_em` em UPDATEs
- `trg_estorno_atualizar_timestamp` - Atualiza `atualizado_em` em UPDATEs

### Funções de Manutenção
- `infra.limpar_idempotencia_expirada()` - Remove registros com TTL expirado
- `infra.limpar_outbox_processados()` - Remove eventos processados há > 7 dias

## 🚀 Executar Migrations

### Automaticamente (Spring Boot)
```bash
# Flyway roda automaticamente no startup da aplicação
mvn spring-boot:run
```

### Manualmente (Flyway CLI)
```bash
# Validar migrations
flyway validate

# Executar migrations pendentes
flyway migrate

# Ver histórico
flyway info

# Limpar banco (CUIDADO!)
flyway clean
```

### Via Docker
```bash
# Conectar ao PostgreSQL
docker-compose exec postgres psql -U postgres -d pagamentos

# Ver schemas criados
\dn

# Ver tabelas de um schema
\dt pagamento.*

# Descrever uma tabela
\d pagamento.pagamento

# Ver histórico do Flyway
SELECT * FROM flyway_schema_history ORDER BY installed_rank;
```

## 📋 Checklist de Validação

Após executar as migrations, verificar:

- [ ] Schemas criados: `pagamento`, `estorno`, `infra`
- [ ] Extensões: `uuid-ossp`, `pg_trgm`
- [ ] Tabela `pagamento.pagamento` com todos os campos
- [ ] Tabela `estorno.estorno` com FK para pagamento
- [ ] Tabela `infra.outbox` para eventos
- [ ] Tabela `infra.idempotencia` para chaves
- [ ] Tabela `infra.evento_auditoria` para logs
- [ ] Todos os índices criados
- [ ] Triggers de auditoria funcionando
- [ ] Constraints de validação ativas
- [ ] Dados de exemplo (apenas dev)

## 🔍 Queries de Validação

```sql
-- Ver todos os schemas
SELECT schema_name FROM information_schema.schemata 
WHERE schema_name IN ('pagamento', 'estorno', 'infra');

-- Ver todas as tabelas
SELECT table_schema, table_name 
FROM information_schema.tables 
WHERE table_schema IN ('pagamento', 'estorno', 'infra')
ORDER BY table_schema, table_name;

-- Ver todos os índices
SELECT schemaname, tablename, indexname 
FROM pg_indexes 
WHERE schemaname IN ('pagamento', 'estorno', 'infra')
ORDER BY schemaname, tablename;

-- Ver todas as constraints
SELECT conname, contype, conrelid::regclass 
FROM pg_constraint 
WHERE connamespace IN (
    SELECT oid FROM pg_namespace 
    WHERE nspname IN ('pagamento', 'estorno', 'infra')
);

-- Ver histórico do Flyway
SELECT installed_rank, version, description, type, script, success, installed_on
FROM flyway_schema_history
ORDER BY installed_rank;

-- Testar dados de exemplo
SELECT COUNT(*) as total_pagamentos FROM pagamento.pagamento;
SELECT COUNT(*) as total_estornos FROM estorno.estorno;
SELECT COUNT(*) as total_outbox FROM infra.outbox;
```

## ⚠️ Notas Importantes

1. **Migration V5 (dados de exemplo)**
   - Apenas para desenvolvimento
   - Comentar ou não executar em produção
   - Útil para testar a aplicação sem precisar criar dados manualmente

2. **Flyway baseline**
   - Configurado no `application.yml`: `baseline-on-migrate: true`
   - Permite aplicar migrations em banco existente

3. **Rollback**
   - Flyway Community não suporta rollback automático
   - Para desfazer, criar nova migration com comandos reversos
   - Ou usar Flyway Teams/Enterprise

4. **Separação por schemas**
   - Facilita extração futura para microsserviços
   - Cada bounded context tem seu próprio schema
   - Foreign keys funcionam entre schemas

