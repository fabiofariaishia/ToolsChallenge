# 🎯 Cheat Sheet - Observabilidade ToolsChallenge

> **Referência rápida** para usar Grafana, Prometheus, Jaeger e Actuator

---

## 🌐 URLs de Acesso Rápido

```bash
# Grafana (Dashboards)
http://localhost:3000
Credenciais: admin / admin123

# Prometheus (Queries)
http://localhost:9090

# Jaeger (Traces)
http://localhost:16686

# Actuator (Health)
http://localhost:8080/atuador/health

# Swagger (API Docs)
http://localhost:8080/swagger-ui.html

# Kafka UI
http://localhost:8081
```

---

## 📊 Dashboards Grafana (5)

### Community (3)
1. **JVM Micrometer** → Memory, GC, Threads, CPU
2. **Spring Boot Statistics** → HTTP, Logs, Tomcat, JVM
3. **Resilience4j** → Circuit Breaker, Retry, Bulkhead

### Custom (2)
4. **HTTP Metrics** → Latência, Throughput, Erros
5. **Business Metrics** → Pagamentos, Estornos, DLQ, CB State

---

## 🔥 Queries PromQL - Top 10

### 1. Estado do Circuit Breaker
```promql
circuit_breaker_adquirente_state
```
*Resultado: 0=CLOSED, 1=OPEN, 2=HALF_OPEN*

### 2. Taxa de Requisições HTTP (req/s)
```promql
sum(rate(http_server_requests_seconds_count[5m])) by (uri)
```

### 3. Latência p99 por Endpoint
```promql
histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri))
```

### 4. Taxa de Erro 5xx (%)
```promql
100 * (
  sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
  /
  sum(rate(http_server_requests_seconds_count[5m]))
)
```

### 5. Pagamentos Criados (últimos 5min)
```promql
sum(rate(pagamento_criados_total[5m])) by (status)
```

### 6. Estornos Criados (últimos 5min)
```promql
sum(rate(estorno_criados_total[5m])) by (status)
```

### 7. Taxa de Aprovação de Pagamentos (%)
```promql
100 * (
  sum(rate(pagamento_criados_total{status="AUTORIZADO"}[5m]))
  /
  sum(rate(pagamento_criados_total[5m]))
)
```

### 8. DLQ - Total Última Hora
```promql
sum(increase(reprocessamento_dlq_total[1h])) by (tipo)
```

### 9. Heap JVM Usado (MB)
```promql
jvm_memory_used_bytes{area="heap"} / 1024 / 1024
```

### 10. Conexões DB Ativas
```promql
hikaricp_connections_active{pool="HikariPool-1"}
```

---

## 🩺 Actuator - Endpoints Úteis

```bash
# Health geral
curl http://localhost:8080/atuador/health

# Circuit Breakers
curl http://localhost:8080/atuador/circuitbreakers

# Métricas (lista)
curl http://localhost:8080/atuador/metrics

# Métrica específica
curl http://localhost:8080/atuador/metrics/http.server.requests

# Prometheus format
curl http://localhost:8080/atuador/prometheus
```

---

## 🔍 Jaeger - Buscar Traces

### Buscar traces recentes
1. Service: `toolschallenge`
2. Lookback: `Last 1 Hour`
3. **Find Traces**

### Buscar traces lentos
1. Service: `toolschallenge`
2. Min Duration: `500ms`
3. **Find Traces**

### Buscar por Correlation ID
1. Tags: `correlationId=<UUID>`
2. **Find Traces**

### Buscar erros 5xx
1. Tags: `http.status_code=500`
2. **Find Traces**

---

## 🎯 Casos de Uso Rápidos

### Problema: "API está lenta"
1. **Grafana** → HTTP Metrics → Ver p99
2. **Prometheus** → Query latência específica
3. **Jaeger** → Buscar traces lentos (Min Duration: 1000ms)
4. **Análise**: Identificar span mais demorado

### Problema: "Muitos erros 500"
1. **Grafana** → HTTP Metrics → Ver Error Rate
2. **Prometheus** → `sum(rate(http_server_requests_seconds_count{status="500"}[5m]))`
3. **Logs** → `grep "ERROR" application.log`
4. **Jaeger** → Tags: `http.status_code=500`

### Problema: "Circuit Breaker abrindo"
1. **Grafana** → Resilience4j Dashboard → Ver estado
2. **Actuator** → `curl .../circuitbreakers` → Ver failure rate
3. **Prometheus** → `circuit_breaker_adquirente_state`
4. **Logs** → `grep "Circuit Breaker" application.log`

### Problema: "DLQ crescendo"
1. **Grafana** → Business Metrics → Ver DLQ panels
2. **Prometheus** → `sum(reprocessamento_dlq_total) by (tipo)`
3. **Database** → `SELECT * FROM pagamento WHERE tentativas_processamento >= 3`
4. **Logs** → `grep "DLQ" application.log`

---

## 📈 Interpretação de Métricas

### HTTP Latency (p95/p99)
```
🟢 < 300ms   → Excelente
🟡 300-800ms → Aceitável
🔴 > 800ms   → Ruim (investigar)
```

### Taxa de Erro 5xx
```
🟢 < 0.1%  → Perfeito (4 noves)
🟡 0.1-1%  → Aceitável (3 noves)
🔴 > 1%    → Crítico (investigar)
```

### Circuit Breaker State
```
🟢 0 (CLOSED)     → Sistema externo OK
🟡 2 (HALF_OPEN)  → Testando recuperação
🔴 1 (OPEN)       → Sistema externo DOWN
```

### Heap JVM
```
🟢 < 70%   → OK
🟡 70-85%  → Atenção
🔴 > 90%   → Crítico (risco OOM)
```

### DLQ
```
🟢 0 itens       → Perfeito
🟡 1-10 itens/h  → Atenção
🔴 > 10 itens/h  → Investigar urgente
```

---

## 🛠️ Comandos PowerShell Úteis

### Verificar serviços rodando
```powershell
docker ps --format "table {{.Names}}\t{{.Status}}" | Select-String "grafana|prometheus|jaeger"
```

### Health check rápido
```powershell
Invoke-RestMethod http://localhost:8080/atuador/health | ConvertTo-Json -Depth 3
```

### Query Prometheus via API
```powershell
$query = "circuit_breaker_adquirente_state"
Invoke-RestMethod "http://localhost:9090/api/v1/query?query=$query" | ConvertTo-Json -Depth 3
```

### Verificar Circuit Breaker
```powershell
Invoke-RestMethod http://localhost:8080/atuador/circuitbreakers | ConvertTo-Json
```

### Criar pagamento de teste
```powershell
$body = @{
    descricao = "Teste Observabilidade"
    valor = 100.00
    tipoPagamento = "CARTAO_CREDITO"
} | ConvertTo-Json

Invoke-RestMethod -Method POST -Uri "http://localhost:8080/pagamentos" `
  -Headers @{
    "Content-Type" = "application/json"
    "Idempotency-Key" = [guid]::NewGuid().ToString()
  } `
  -Body $body
```

---

## 📚 Funções PromQL Essenciais

### Funções de Taxa (Counters)
```promql
rate(metric[5m])       # Taxa por segundo (últimos 5min)
increase(metric[1h])   # Incremento total (última hora)
irate(metric[5m])      # Taxa instantânea (últimos 2 pontos)
```

### Funções de Agregação (Gauges)
```promql
avg_over_time(metric[5m])   # Média nos últimos 5min
max_over_time(metric[5m])   # Máximo nos últimos 5min
min_over_time(metric[5m])   # Mínimo nos últimos 5min
```

### Agregações Multi-Séries
```promql
sum(metric) by (label)      # Soma agrupada por label
avg(metric) by (label)      # Média agrupada
max(metric)                 # Máximo global
count(metric)               # Contar séries
topk(5, metric)             # Top 5 valores
```

### Percentis (Histograms)
```promql
histogram_quantile(0.50, sum(rate(metric_bucket[5m])) by (le))  # p50
histogram_quantile(0.95, sum(rate(metric_bucket[5m])) by (le))  # p95
histogram_quantile(0.99, sum(rate(metric_bucket[5m])) by (le))  # p99
```

---

## 🎓 Fluxo de Investigação

```
1. Alerta/Problema reportado
        ↓
2. GRAFANA (visão geral)
   → Dashboard relevante
   → Identificar anomalia
        ↓
3. PROMETHEUS (confirmar)
   → Query específica
   → Ver histórico
        ↓
4. JAEGER (debugar)
   → Buscar trace específico
   → Analisar timeline
        ↓
5. LOGS (detalhes)
   → Ver stacktrace
   → Correlation ID
        ↓
6. ACTUATOR (validar fix)
   → Health check
   → Circuit Breaker status
```

---

## 🚨 Troubleshooting Rápido

### "Não vejo métricas no Grafana"
```bash
# 1. App rodando?
curl http://localhost:8080/atuador/health

# 2. Prometheus coletando?
curl http://localhost:9090/api/v1/targets

# 3. Métricas sendo exportadas?
curl http://localhost:8080/atuador/prometheus

# 4. Dashboard com dados?
# → Verificar time range
# → Verificar filtro $application
```

### "Não vejo traces no Jaeger"
```bash
# 1. Jaeger rodando?
curl http://localhost:16686/api/services

# 2. App enviando traces?
# → Ver logs: "Micrometer Tracing enabled"

# 3. Sampling configurado?
# → application.yml: sampling.probability: 1.0

# 4. Criar request e buscar
# → POST /pagamentos
# → Buscar no Jaeger (Lookback: 5min)
```

### "Circuit Breaker não abre"
```bash
# 1. Verificar configuração
# → minimum-number-of-calls: 5
# → failure-rate-threshold: 50

# 2. Gerar falhas
# → Fazer 10 requests (5+ devem falhar)

# 3. Ver estado
curl http://localhost:8080/atuador/circuitbreakers

# 4. Verificar gauge
# → Prometheus: circuit_breaker_adquirente_state
```

---

## 📖 Documentação Completa

Para guia detalhado: **GUIA_OBSERVABILIDADE.md**

Para arquitetura do projeto: **README.md**

Para regras de desenvolvimento: **.github/instructions/copilot-instructions.md**

---

**Autor**: ToolsChallenge Team  
**Última Atualização**: 03/11/2025  
**Versão**: 1.0
