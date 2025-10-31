# Testes do Outbox Pattern

Este documento contém exemplos para testar o Outbox Pattern integrado com Kafka.

## 📋 Pré-requisitos

- Aplicação rodando em `http://localhost:8080`
- PostgreSQL rodando em `localhost:5432`
- Redis rodando em `localhost:6379`
- **Kafka rodando em `localhost:9092`**

## 🎯 Arquitetura Implementada

### Outbox Pattern:
1. **Evento de domínio** ocorre (ex: Pagamento criado)
2. **Na mesma transação**: Salva no banco + Cria registro na tabela `outbox`
3. **OutboxProcessor** (job a cada 500ms) busca eventos PENDENTE
4. **KafkaPublisherService** publica no Kafka
5. **OutboxService** marca como PROCESSADO ou ERRO

### Tópicos Kafka:
- `pagamento.eventos` - Eventos de pagamento
- `estorno.eventos` - Eventos de estorno

### Eventos Publicados:
- **PagamentoCriado**: Quando um pagamento é criado
- **PagamentoStatusAlterado**: Quando status muda (PENDENTE → AUTORIZADO/NEGADO)
- **EstornoCriado**: Quando um estorno é solicitado
- **EstornoStatusAlterado**: Quando status do estorno muda

## 🧪 Cenários de Teste

### 1️⃣ Criar Pagamento e Verificar Eventos no Outbox

**Criar pagamento:**

```bash
curl -X POST http://localhost:8080/pagamentos \
  -H "Content-Type: application/json" \
  -H "Chave-Idempotencia: pag-outbox-001" \
  -d '{
    "estabelecimento": "Loja ABC",
    "valor": 150.00,
    "tipoPagamento": "AVISTA",
    "parcelas": 1,
    "cartaoMascarado": "4444********1234"
  }'
```

**Verificar eventos criados no outbox:**

```sql
-- Conectar no PostgreSQL
docker exec -it toolschallenge-postgres-1 psql -U sicredi -d pagamentos

-- Buscar eventos criados
SELECT 
  id,
  agregado_tipo,
  evento_tipo,
  status,
  criado_em,
  processado_em
FROM infra.outbox
ORDER BY criado_em DESC
LIMIT 10;
```

**Resultado Esperado:**
- 2 eventos criados:
  1. `agregado_tipo='Pagamento'`, `evento_tipo='PagamentoCriado'`, `status='PROCESSADO'`
  2. `agregado_tipo='Pagamento'`, `evento_tipo='PagamentoStatusAlterado'`, `status='PROCESSADO'`

**Ver payload do evento:**

```sql
SELECT 
  evento_tipo,
  payload
FROM infra.outbox
WHERE agregado_tipo = 'Pagamento'
ORDER BY criado_em DESC
LIMIT 1;
```

---

### 2️⃣ Criar Estorno e Verificar Eventos

**Criar estorno:**

```bash
curl -X POST http://localhost:8080/estornos \
  -H "Content-Type: application/json" \
  -H "Chave-Idempotencia: estorno-outbox-001" \
  -d '{
    "idTransacao": "TXN-XXX-AVISTA",
    "valor": 150.00,
    "motivo": "Teste de Outbox Pattern"
  }'
```

**Verificar eventos no outbox:**

```sql
SELECT 
  id,
  agregado_tipo,
  evento_tipo,
  status,
  criado_em
FROM infra.outbox
WHERE agregado_tipo = 'Estorno'
ORDER BY criado_em DESC;
```

**Resultado Esperado:**
- 2 eventos:
  1. `evento_tipo='EstornoCriado'`, `status='PROCESSADO'`
  2. `evento_tipo='EstornoStatusAlterado'`, `status='PROCESSADO'`

---

### 3️⃣ Verificar Publicação no Kafka

**Consumir eventos do tópico `pagamento.eventos`:**

```bash
docker exec -it toolschallenge-kafka-1 kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic pagamento.eventos \
  --from-beginning
```

**Consumir eventos do tópico `estorno.eventos`:**

```bash
docker exec -it toolschallenge-kafka-1 kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic estorno.eventos \
  --from-beginning
```

**Formato da Mensagem Kafka:**

```json
{
  "eventoId": 1,
  "eventoTipo": "PagamentoCriado",
  "agregadoId": "TXN-XXX-AVISTA",
  "agregadoTipo": "Pagamento",
  "timestamp": "2025-10-30T20:30:00Z",
  "payload": {
    "idPagamento": 1,
    "idTransacao": "TXN-XXX-AVISTA",
    "descricao": "Loja ABC",
    "valor": 150.00,
    "metodoPagamento": "AVISTA",
    "formaPagamento": "AVISTA",
    "status": "PENDENTE",
    "criadoEm": "2025-10-30T20:30:00Z"
  }
}
```

---

### 4️⃣ Testar Retry Automático (Simular Kafka Offline)

**1. Parar Kafka:**

```bash
docker stop toolschallenge-kafka-1
```

**2. Criar pagamento (eventos ficarão ERRO):**

```bash
curl -X POST http://localhost:8080/pagamentos \
  -H "Content-Type: application/json" \
  -H "Chave-Idempotencia: pag-retry-001" \
  -d '{
    "estabelecimento": "Loja XYZ",
    "valor": 200.00,
    "tipoPagamento": "AVISTA",
    "parcelas": 1,
    "cartaoMascarado": "5555********6789"
  }'
```

**3. Verificar eventos com ERRO:**

```sql
SELECT 
  id,
  evento_tipo,
  status,
  tentativas,
  ultimo_erro
FROM infra.outbox
WHERE status = 'ERRO'
ORDER BY criado_em DESC;
```

**Resultado Esperado:**
- `status='ERRO'`
- `tentativas=1` (ou 2, 3... até 3)
- `ultimo_erro` com mensagem do Kafka

**4. Religar Kafka:**

```bash
docker start toolschallenge-kafka-1
```

**5. Aguardar 5 segundos (job de retry)**

**6. Verificar eventos processados:**

```sql
SELECT 
  id,
  evento_tipo,
  status,
  tentativas,
  processado_em
FROM infra.outbox
WHERE evento_tipo = 'PagamentoCriado'
ORDER BY criado_em DESC
LIMIT 5;
```

**Resultado Esperado:**
- `status='PROCESSADO'`
- `processado_em` preenchido
- Eventos publicados no Kafka

---

### 5️⃣ Verificar Dead Letter Queue (DLQ)

**Eventos com 3 tentativas falhadas vão para DLQ:**

```sql
SELECT 
  id,
  evento_tipo,
  status,
  tentativas,
  ultimo_erro,
  criado_em
FROM infra.outbox
WHERE status = 'ERRO' AND tentativas >= 3
ORDER BY criado_em DESC;
```

**Estes eventos precisam de intervenção manual.**

---

### 6️⃣ Monitorar Métricas do Outbox

**Job logAmetricas executa a cada 1 minuto:**

```
Logs esperados:
Métricas Outbox - Pendentes: 5, Com Erro: 2
```

**Se muitos eventos com erro:**

```
ALERTA: 15 eventos com erro no outbox! Verificar logs e DLQ.
```

**Contar eventos por status:**

```sql
SELECT 
  status,
  COUNT(*) as quantidade
FROM infra.outbox
GROUP BY status;
```

---

### 7️⃣ Limpeza de Eventos Processados

**Job limparEventosProcessadosAntigos executa diariamente à meia-noite:**

- Remove eventos `PROCESSADO` há mais de 7 dias
- Mantém eventos `PENDENTE` e `ERRO`

**Forçar limpeza manualmente:**

```sql
DELETE FROM infra.outbox
WHERE status = 'PROCESSADO'
  AND processado_em < NOW() - INTERVAL '7 days';
```

---

### 8️⃣ Verificar Performance do Outbox

**Tempo médio de processamento:**

```sql
SELECT 
  AVG(EXTRACT(EPOCH FROM (processado_em - criado_em))) as tempo_medio_segundos,
  MIN(EXTRACT(EPOCH FROM (processado_em - criado_em))) as tempo_minimo,
  MAX(EXTRACT(EPOCH FROM (processado_em - criado_em))) as tempo_maximo
FROM infra.outbox
WHERE status = 'PROCESSADO'
  AND processado_em IS NOT NULL;
```

**Resultado Esperado:**
- Tempo médio: < 1 segundo (processamento rápido)

**Taxa de sucesso:**

```sql
SELECT 
  status,
  COUNT(*) as quantidade,
  ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER(), 2) as percentual
FROM infra.outbox
GROUP BY status;
```

---

### 9️⃣ Auditoria de Eventos

**Buscar todos eventos de um pagamento:**

```sql
SELECT 
  id,
  evento_tipo,
  status,
  criado_em,
  processado_em,
  payload
FROM infra.outbox
WHERE agregado_id = 'TXN-XXX-AVISTA'
ORDER BY criado_em ASC;
```

**Eventos processados nas últimas 24h:**

```sql
SELECT 
  agregado_tipo,
  evento_tipo,
  COUNT(*) as quantidade
FROM infra.outbox
WHERE status = 'PROCESSADO'
  AND processado_em >= NOW() - INTERVAL '24 hours'
GROUP BY agregado_tipo, evento_tipo
ORDER BY quantidade DESC;
```

---

## 📊 Logs Importantes

```
# Evento criado no outbox
[OutboxService] Evento criado no outbox: id=1, tipo=PagamentoCriado, agregado=TXN-XXX

# Processando eventos
[OutboxProcessor] Processando 5 eventos pendentes do outbox

# Evento publicado no Kafka
[KafkaPublisherService] Evento publicado com sucesso no Kafka: id=1, tipo=PagamentoCriado, topico=pagamento.eventos, partition=0, offset=123

# Evento marcado como processado
[OutboxService] Evento marcado como processado: id=1, tipo=PagamentoCriado

# Retry
[OutboxProcessor] Reprocessando 3 eventos com erro do outbox
[OutboxService] Evento marcado como erro: id=2, tipo=EstornoCriado, tentativas=1, erro=Connection refused

# Limpeza
[OutboxProcessor] Limpeza de eventos concluída: 150 eventos removidos

# Métricas
[OutboxProcessor] Métricas Outbox - Pendentes: 0, Com Erro: 2
```

---

## ✅ Checklist de Validação

- [ ] Criar pagamento → 2 eventos no outbox (PagamentoCriado + StatusAlterado)
- [ ] Criar estorno → 2 eventos no outbox (EstornoCriado + StatusAlterado)
- [ ] Eventos publicados no Kafka (consumidor recebe)
- [ ] Eventos marcados como PROCESSADO
- [ ] Kafka offline → Eventos marcam ERRO
- [ ] Kafka online → Retry processa eventos
- [ ] DLQ: Eventos com 3 tentativas permanecem ERRO
- [ ] Limpeza: Eventos processados > 7 dias são removidos
- [ ] Métricas: Log a cada 1 minuto
- [ ] Performance: Processamento < 1 segundo

---

## 🎯 Conclusão

O Outbox Pattern garante:

1. ✅ **At-least-once delivery**: Eventos sempre publicados (eventual)
2. ✅ **Transacionalidade**: Evento salvo na mesma transação
3. ✅ **Retry automático**: Até 3 tentativas
4. ✅ **Dead Letter Queue**: Eventos com falha permanente
5. ✅ **Auditoria**: Histórico completo de eventos
6. ✅ **Performance**: Processamento assíncrono (500ms)
7. ✅ **Limpeza**: Remove eventos antigos (7 dias)
