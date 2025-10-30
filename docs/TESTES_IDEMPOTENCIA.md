# Testes de Idempotência

Este documento contém exemplos para testar a funcionalidade de idempotência.

## 📋 Pré-requisitos

- Aplicação rodando em `http://localhost:8080`
- Redis rodando em `localhost:6379`
- PostgreSQL rodando em `localhost:5432`

## 🧪 Cenários de Teste

### 1️⃣ Criar Pagamento COM Idempotency-Key (Sucesso)

```bash
curl -X POST http://localhost:8080/pagamentos \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: pagamento-12345" \
  -d '{
    "descricao": "Pagamento de teste - Idempotência",
    "metodoPagamento": "CARTAO_CREDITO",
    "valor": 150.00,
    "formaPagamento": "BOLETO",
    "status": "PENDENTE"
  }'
```

**Resultado Esperado:**
- Status: `201 Created`
- Corpo: Dados do pagamento criado
- Header `X-Idempotency-Replayed`: Não presente

---

### 2️⃣ Repetir Mesma Requisição (Deve Retornar Resposta Cacheada)

```bash
curl -X POST http://localhost:8080/pagamentos \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: pagamento-12345" \
  -d '{
    "descricao": "Pagamento de teste - Idempotência",
    "metodoPagamento": "CARTAO_CREDITO",
    "valor": 150.00,
    "formaPagamento": "BOLETO",
    "status": "PENDENTE"
  }'
```

**Resultado Esperado:**
- Status: `201 Created` (mesmo status da primeira requisição)
- Corpo: **Mesma resposta da primeira requisição**
- Header `X-Idempotency-Replayed: true`
- ⚠️ **Nenhum novo registro criado no banco**

---

### 3️⃣ Criar Pagamento SEM Idempotency-Key (Deve Retornar Erro 400)

```bash
curl -X POST http://localhost:8080/pagamentos \
  -H "Content-Type: application/json" \
  -d '{
    "descricao": "Pagamento sem idempotency key",
    "metodoPagamento": "PIX",
    "valor": 100.00,
    "formaPagamento": "PIX",
    "status": "PENDENTE"
  }'
```

**Resultado Esperado:**
- Status: `400 Bad Request`
- Corpo:
```json
{
  "erro": "Header 'Idempotency-Key' ausente",
  "timestamp": "2025-01-29T12:00:00Z",
  "traceId": "abc123"
}
```

---

### 4️⃣ Criar Estorno COM Idempotency-Key (Sucesso)

**Primeiro, crie um pagamento para estornar:**

```bash
curl -X POST http://localhost:8080/pagamentos \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: pagamento-para-estorno-001" \
  -d '{
    "descricao": "Pagamento para estorno",
    "metodoPagamento": "CARTAO_CREDITO",
    "valor": 200.00,
    "formaPagamento": "BOLETO",
    "status": "PENDENTE"
  }'
```

**Depois, crie o estorno:**

```bash
curl -X POST http://localhost:8080/estornos \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: estorno-001" \
  -d '{
    "idPagamento": 1,
    "valorEstorno": 200.00,
    "motivo": "Teste de idempotência de estorno",
    "status": "PENDENTE"
  }'
```

**Resultado Esperado:**
- Status: `201 Created`
- Corpo: Dados do estorno criado
- Header `X-Idempotency-Replayed`: Não presente

---

### 5️⃣ Repetir Criação de Estorno (Deve Retornar Resposta Cacheada)

```bash
curl -X POST http://localhost:8080/estornos \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: estorno-001" \
  -d '{
    "idPagamento": 1,
    "valorEstorno": 200.00,
    "motivo": "Teste de idempotência de estorno",
    "status": "PENDENTE"
  }'
```

**Resultado Esperado:**
- Status: `201 Created`
- Corpo: **Mesma resposta da primeira requisição**
- Header `X-Idempotency-Replayed: true`
- ⚠️ **Nenhum novo estorno criado**

---

### 6️⃣ Criar Estorno SEM Idempotency-Key (Deve Retornar Erro 400)

```bash
curl -X POST http://localhost:8080/estornos \
  -H "Content-Type: application/json" \
  -d '{
    "idPagamento": 1,
    "valorEstorno": 100.00,
    "motivo": "Estorno sem idempotency key",
    "status": "PENDENTE"
  }'
```

**Resultado Esperado:**
- Status: `400 Bad Request`
- Corpo:
```json
{
  "erro": "Header 'Idempotency-Key' ausente",
  "timestamp": "2025-01-29T12:00:00Z",
  "traceId": "abc123"
}
```

---

## 🔍 Verificar Armazenamento no Redis

```bash
# Conectar no Redis via docker
docker exec -it toolschallenge-redis-1 redis-cli

# Listar todas as chaves de idempotência
KEYS idempotencia:*

# Ver conteúdo de uma chave específica
GET idempotencia:pagamento-12345

# Ver TTL de uma chave
TTL idempotencia:pagamento-12345
```

**Resultado Esperado:**
- Chaves: `idempotencia:pagamento-12345`, `idempotencia:estorno-001`
- Conteúdo: JSON com `corpo` e `statusHttp`
- TTL: ~86400 segundos (24 horas)

---

## 🗄️ Verificar Armazenamento no PostgreSQL

```bash
# Conectar no PostgreSQL via docker
docker exec -it toolschallenge-postgres-1 psql -U sicredi -d toolschallenge

# Listar registros de idempotência
SELECT 
  chave_idempotencia,
  metodo_http,
  caminho,
  status_http,
  criado_em,
  expira_em,
  (expira_em > NOW()) AS ativo
FROM infra.idempotencia
ORDER BY criado_em DESC;

# Ver conteúdo da resposta de uma chave específica
SELECT 
  chave_idempotencia,
  resposta 
FROM infra.idempotencia 
WHERE chave_idempotencia = 'pagamento-12345';
```

**Resultado Esperado:**
- Registros para `pagamento-12345` e `estorno-001`
- `metodo_http`: `POST`
- `caminho`: `/pagamentos` ou `/estornos`
- `status_http`: `201`
- `criado_em`: timestamp atual
- `expira_em`: criado_em + 24 horas
- `ativo`: `true`

---

## 🧹 Testar Job de Limpeza

O job de limpeza executa **a cada 1 hora** (`@Scheduled(fixedRate = 3600000)`).

### Forçar Limpeza Manualmente (Simulação)

**1. Criar registro expirado no PostgreSQL:**

```sql
-- Conectar no PostgreSQL
docker exec -it toolschallenge-postgres-1 psql -U sicredi -d toolschallenge

-- Inserir registro expirado (expira_em no passado)
INSERT INTO infra.idempotencia (
  chave_idempotencia,
  metodo_http,
  caminho,
  resposta,
  status_http,
  criado_em,
  expira_em
) VALUES (
  'teste-expirado-001',
  'POST',
  '/pagamentos',
  '{"id": 999, "valor": 100.00}',
  201,
  NOW() - INTERVAL '25 hours',
  NOW() - INTERVAL '1 hour'
);

-- Verificar que foi inserido
SELECT chave_idempotencia, expira_em < NOW() AS expirado 
FROM infra.idempotencia 
WHERE chave_idempotencia = 'teste-expirado-001';
```

**2. Aguardar 1 hora ou reiniciar aplicação**

Após 1 hora, verificar logs:

```
[IdempotenciaLimpezaJob] Iniciando limpeza de registros expirados de idempotência
[IdempotenciaService] Removidos 1 registros expirados de idempotência
[IdempotenciaLimpezaJob] Limpeza de idempotência concluída: 1 registros removidos
```

**3. Verificar remoção no banco:**

```sql
SELECT COUNT(*) FROM infra.idempotencia WHERE chave_idempotencia = 'teste-expirado-001';
-- Resultado esperado: 0
```

---

## ⏱️ Testar Expiração de TTL

**1. Criar pagamento com TTL curto (modificar anotação temporariamente):**

Editar `PagamentoController.java`:

```java
@Idempotente(ttl = 1, unidadeTempo = TimeUnit.MINUTES) // 1 minuto ao invés de 24 horas
```

**2. Criar pagamento:**

```bash
curl -X POST http://localhost:8080/pagamentos \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: pagamento-ttl-teste" \
  -d '{
    "descricao": "Teste de TTL",
    "metodoPagamento": "PIX",
    "valor": 50.00,
    "formaPagamento": "PIX",
    "status": "PENDENTE"
  }'
```

**3. Repetir imediatamente (deve retornar cache):**

```bash
curl -X POST http://localhost:8080/pagamentos \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: pagamento-ttl-teste" \
  -d '{ ... }'
```

Resultado: `X-Idempotency-Replayed: true`

**4. Aguardar 2 minutos e repetir:**

```bash
curl -X POST http://localhost:8080/pagamentos \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: pagamento-ttl-teste" \
  -d '{
    "descricao": "Teste de TTL - Após expiração",
    "metodoPagamento": "PIX",
    "valor": 50.00,
    "formaPagamento": "PIX",
    "status": "PENDENTE"
  }'
```

**Resultado Esperado:**
- Status: `201 Created`
- Header `X-Idempotency-Replayed`: **Não presente** (chave expirada, nova requisição processada)
- Novo registro criado no banco

---

## 🔄 Testar Failover Redis → PostgreSQL

**1. Parar Redis:**

```bash
docker stop toolschallenge-redis-1
```

**2. Criar pagamento:**

```bash
curl -X POST http://localhost:8080/pagamentos \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: pagamento-failover-001" \
  -d '{
    "descricao": "Teste de failover",
    "metodoPagamento": "CARTAO_CREDITO",
    "valor": 100.00,
    "formaPagamento": "BOLETO",
    "status": "PENDENTE"
  }'
```

**Resultado Esperado:**
- Status: `201 Created` (funciona normalmente)
- Log: `[WARN] Erro ao salvar no Redis, mas salvo no PostgreSQL`

**3. Repetir requisição:**

```bash
curl -X POST http://localhost:8080/pagamentos \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: pagamento-failover-001" \
  -d '{ ... }'
```

**Resultado Esperado:**
- Status: `201 Created`
- Header `X-Idempotency-Replayed: true`
- Log: `[INFO] Resposta encontrada no PostgreSQL`
- ⚠️ **Resposta vem do PostgreSQL (L2)**

**4. Religar Redis:**

```bash
docker start toolschallenge-redis-1
```

---

## ✅ Checklist de Validação

- [ ] Criar pagamento com Idempotency-Key → Sucesso (201)
- [ ] Repetir requisição → Resposta cacheada (201 + header replayed)
- [ ] Criar pagamento sem header → Erro 400
- [ ] Criar estorno com Idempotency-Key → Sucesso (201)
- [ ] Repetir estorno → Resposta cacheada (201 + header replayed)
- [ ] Criar estorno sem header → Erro 400
- [ ] Verificar Redis: Chaves criadas com TTL correto
- [ ] Verificar PostgreSQL: Registros criados com expira_em
- [ ] Job de limpeza: Remove registros expirados
- [ ] TTL expirado: Nova requisição processa normalmente
- [ ] Failover Redis: Funciona apenas com PostgreSQL
- [ ] Failover Redis: Resposta vem do PostgreSQL

---

## 📊 Logs Importantes

```
# Idempotência detectada
[IdempotenciaInterceptor] Requisição duplicada detectada para chave: pagamento-12345

# Resposta salva
[IdempotenciaService] Resposta salva para chave de idempotência: pagamento-12345

# Resposta encontrada no Redis (L1)
[IdempotenciaService] Resposta encontrada no Redis para chave: pagamento-12345

# Resposta encontrada no PostgreSQL (L2)
[IdempotenciaService] Resposta encontrada no PostgreSQL para chave: pagamento-12345

# Limpeza executada
[IdempotenciaLimpezaJob] Limpeza de idempotência concluída: 5 registros removidos

# Erro no Redis (failover)
[IdempotenciaService] Erro ao salvar no Redis, mas salvo no PostgreSQL com sucesso
```

---

## 🎯 Conclusão

A implementação de idempotência garante:

1. ✅ **Proteção contra duplicação**: Mesma chave = mesma resposta
2. ✅ **Performance**: Redis (L1) para leituras rápidas
3. ✅ **Durabilidade**: PostgreSQL (L2) para persistência
4. ✅ **Expiração**: TTL configurável (default 24h)
5. ✅ **Limpeza automática**: Job a cada 1 hora
6. ✅ **Resiliência**: Failover Redis → PostgreSQL
7. ✅ **Auditoria**: Header `X-Idempotency-Replayed` indica cache
