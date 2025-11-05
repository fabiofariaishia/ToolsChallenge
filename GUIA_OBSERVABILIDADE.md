# 🔍 Guia Prático de Observabilidade - ToolsChallenge

> **Guia hands-on** para usar Grafana, Prometheus, Jaeger e Actuator no dia a dia do desenvolvimento

---

## 📋 Índice Rápido

1. [Stack de Observabilidade](#-stack-de-observabilidade)
2. [Grafana - Dashboards Visuais](#-grafana---dashboards-visuais)
3. [Prometheus - Queries e Métricas](#-prometheus---queries-e-métricas)
4. [Jaeger - Distributed Tracing](#-jaeger---distributed-tracing)
5. [Actuator - Health Checks](#-actuator---health-checks)
6. [Casos de Uso Práticos](#-casos-de-uso-práticos)
7. [Troubleshooting com Observabilidade](#-troubleshooting-com-observabilidade)

---

## 🎯 Stack de Observabilidade

### **URLs de Acesso Rápido**

| Ferramenta | URL | Credenciais | Propósito |
|------------|-----|-------------|-----------|
| **Grafana** | http://localhost:3000 | `admin` / `admin123` | Dashboards visuais, alertas |
| **Prometheus** | http://localhost:9090 | - | Queries PromQL, métricas raw |
| **Jaeger** | http://localhost:16686 | - | Traces distribuídos, debugging |
| **Actuator** | http://localhost:8080/atuador | - | Health checks, métricas API |
| **Swagger** | http://localhost:8080/swagger-ui.html | - | Documentação interativa API |
| **Kafka UI** | http://localhost:8081 | - | Monitorar tópicos Kafka |

### **Quando usar cada ferramenta?**

| Situação | Ferramenta | Por quê? |
|----------|-----------|----------|
| "A API está lenta" | **Grafana** → HTTP Metrics | Latência p95/p99 por endpoint |
| "Quantos pagamentos foram criados hoje?" | **Grafana** → Business Metrics | Counters de negócio |
| "Circuit Breaker está abrindo muito" | **Grafana** → Resilience4j Dashboard | Estados, taxas de falha |
| "Preciso de uma query customizada" | **Prometheus** | PromQL queries ad-hoc |
| "Onde está travando a requisição?" | **Jaeger** | Traces com timings por span |
| "A aplicação está saudável?" | **Actuator** `/health` | Status de DB, Redis, Kafka |
| "Quanto de memória JVM está usando?" | **Grafana** → JVM Micrometer | Heap, GC, threads |

---

## 📊 Grafana - Dashboards Visuais

### **1. Acessar Grafana**

```bash
# Abrir no navegador
http://localhost:3000

# Login
Usuário: admin
Senha: admin123
```

**Primeira vez**: Grafana pedirá para trocar a senha (pode pular clicando "Skip").

---

### **2. Navegar pelos Dashboards**

**Menu lateral esquerdo** → **Dashboards** (ícone de 4 quadrados)

Você verá **5 dashboards provisionados**:

#### **📈 Dashboards Community (3)**

**1. JVM Micrometer** (`jvm_micrometer_dashboard`)

**Para que serve**: Monitorar saúde da JVM (memória, GC, threads)

**Principais painéis**:
- **JVM Memory Pools**: Heap vs Non-Heap, Eden, Survivor, Old Gen
- **Garbage Collection**: Contagem de GC, tempo de pausa
- **Threads**: Threads ativas, daemon, pico
- **CPU Usage**: Uso de CPU do processo

**Quando usar**:
- ✅ Investigar OutOfMemoryError
- ✅ Analisar performance de GC
- ✅ Detectar memory leaks (heap crescendo sem parar)
- ✅ Verificar se threads estão aumentando (pode indicar leak)

**Como interpretar**:
```
🟢 Heap usado < 70% do máximo → OK
🟡 Heap usado 70-85% → Atenção (pode precisar mais memória)
🔴 Heap usado > 90% → Crítico (risco de OOM)

🟢 GC pause < 100ms → OK
🟡 GC pause 100-500ms → Atenção (pode afetar latência)
🔴 GC pause > 500ms → Crítico (usuário vai perceber)
```

---

**2. Spring Boot Statistics** (`spring_boot_21`)

**Para que serve**: Visão geral da aplicação Spring Boot

**Principais painéis**:
- **HTTP Requests**: Total, taxa por segundo
- **Logback**: Logs por nível (INFO, WARN, ERROR)
- **Tomcat**: Sessions, threads do servidor
- **JVM Quick Stats**: CPU, memória, threads

**Quando usar**:
- ✅ Monitorar carga de requisições HTTP
- ✅ Detectar picos de erro (Logback ERROR aumentando)
- ✅ Verificar se Tomcat está saturado (threads no máximo)

**Como interpretar**:
```
🟢 HTTP 5xx < 1% → OK
🟡 HTTP 5xx 1-5% → Atenção (investigar causas)
🔴 HTTP 5xx > 5% → Crítico (serviço degradado)

🟢 Tomcat threads < 80% do máximo → OK
🟡 Tomcat threads 80-95% → Atenção (pode precisar escalar)
🔴 Tomcat threads = 100% → Crítico (requisições sendo rejeitadas)
```

---

**3. Resilience4j** (`resilience4j_dashboard`)

**Para que serve**: Monitorar Circuit Breaker, Retry, Bulkhead

**Principais painéis**:
- **Circuit Breaker State**: CLOSED (verde), OPEN (vermelho), HALF_OPEN (amarelo)
- **Circuit Breaker Calls**: Successful vs Failed vs Not Permitted
- **Retry Attempts**: Quantas vezes retentou
- **Bulkhead Usage**: Capacidade do thread pool

**Quando usar**:
- ✅ Investigar falhas de comunicação com adquirente
- ✅ Verificar se Circuit Breaker está protegendo corretamente
- ✅ Analisar se retries estão funcionando

**Como interpretar**:
```
🟢 CB State = CLOSED → Sistema externo saudável
🟡 CB State = HALF_OPEN → Testando recuperação (normal após falhas)
🔴 CB State = OPEN → Sistema externo down (fallback ativo)

🟢 CB Failure Rate < 20% → OK
🟡 CB Failure Rate 20-50% → Atenção (instabilidade)
🔴 CB Failure Rate > 50% → Crítico (vai abrir)
```

---

#### **📊 Dashboards Customizados (2)**

**4. HTTP Metrics** (`http_metrics_toolschallenge`)

**Para que serve**: Analisar performance dos endpoints HTTP

**Principais painéis** (7):

1. **Request Rate by Endpoint**
   - O que mostra: Requisições/segundo por URI
   - Como usar: Identificar endpoints mais chamados
   - Exemplo: `/pagamentos` com 50 req/s vs `/estornos` com 5 req/s

2. **Latency Percentiles**
   - O que mostra: p50, p95, p99 de cada endpoint
   - Como usar: Detectar lentidão
   - Exemplo:
     ```
     /pagamentos:
       p50: 120ms → 50% das requests < 120ms
       p95: 350ms → 95% das requests < 350ms
       p99: 800ms → 99% das requests < 800ms (pior caso)
     ```
   - **Interpretação**:
     - p50 é a "experiência típica"
     - p95 é o "SLA" (95% dos usuários têm boa experiência)
     - p99 detecta "outliers" (casos ruins que afetam poucos usuários)

3. **Error Rates (4xx vs 5xx)**
   - O que mostra: Taxa de erros separada por tipo
   - Como usar:
     - **4xx (vermelho)**: Erro do cliente (validação, not found) → Normal em pequena quantidade
     - **5xx (laranja)**: Erro do servidor → **NUNCA** deve ser alto
   - Exemplo:
     ```
     🟢 5xx = 0% → Perfeito
     🔴 5xx = 5% → CRÍTICO (investigar logs)
     ```

4. **Throughput by Endpoint**
   - O que mostra: Requests/segundo por endpoint E método (GET, POST, etc)
   - Como usar: Entender padrão de uso da API

5. **Success Rate Gauge**
   - O que mostra: % de requisições sem erro 5xx
   - Como usar: Meta = **>99%** (SLA típico)
   - Exemplo:
     ```
     🟢 99.9% → Excelente (3 noves)
     🟡 99% → Aceitável (2 noves)
     🔴 95% → Ruim (1 em cada 20 falha)
     ```

6. **Overall p99 Latency Gauge**
   - O que mostra: Latência p99 global
   - Como usar: Meta = **< 1 segundo** para APIs REST
   - Exemplo:
     ```
     🟢 p99 = 300ms → Ótimo
     🟡 p99 = 800ms → Aceitável
     🔴 p99 = 2s → Ruim (usuário percebe)
     ```

7. **Status Code Distribution (Pie Chart)**
   - O que mostra: Proporção de 200, 201, 400, 404, 500, etc
   - Como usar: Visão geral de saúde da API
   - Exemplo esperado:
     ```
     200 OK: 70%
     201 Created: 20%
     400 Bad Request: 8%
     404 Not Found: 1.5%
     500 Error: 0.5% (DEVE ser < 1%)
     ```

**Quando usar este dashboard**:
- ✅ Monitoramento diário de performance
- ✅ Investigar lentidão de API
- ✅ Validar deploy (antes vs depois)
- ✅ Análise de SLA (cumprir 99% de sucesso?)

---

**5. Business Metrics** (`business_metrics_toolschallenge`)

**Para que serve**: Métricas de negócio e sistema

**Principais painéis** (11):

1. **Pagamentos Rate by Status** (verde/vermelho/amarelo)
   - O que mostra: Taxa de criação de pagamentos por status
   - Cores:
     - 🟢 Verde: `AUTORIZADO` (bom)
     - 🔴 Vermelho: `NEGADO` (esperado, mas monitorar)
     - 🟡 Amarelo: `PENDENTE` (processando)
   - Como usar: Detectar se taxa de negação aumentou (problema no adquirente?)

2. **Estornos Rate by Status**
   - Similar ao anterior, para estornos
   - 🟢 `CANCELADO` (sucesso)
   - 🔴 `NEGADO` (problema)
   - 🟡 `PENDENTE`

3. **Circuit Breaker State Gauge** (0/1/2)
   - O que mostra:
     - **0 = CLOSED** (verde) → Adquirente OK
     - **1 = OPEN** (vermelho) → Adquirente DOWN
     - **2 = HALF_OPEN** (amarelo) → Testando recuperação
   - Como usar: Alertar equipe quando virar 1 (OPEN)

4. **DLQ Rate by Type**
   - O que mostra: Taxa de envio para Dead Letter Queue
   - Tags: `tipo=pagamento` ou `tipo=estorno`
   - Como usar:
     - DLQ crescendo = problemas de processamento
     - Investigar logs dos itens na DLQ

5. **DLQ Total Last Hour**
   - O que mostra: Total enviado para DLQ na última hora
   - Como usar: Meta = **0** (ideal), < 10 aceitável
   - Exemplo:
     ```
     🟢 0 itens → Perfeito
     🟡 1-10 itens → Atenção (validar se é transiente)
     🔴 > 10 itens → Investigar urgente
     ```

6-7. **Pagamento/Estorno Latency Percentiles**
   - O que mostra: p50/p95/p99 de criação de pagamento/estorno
   - Como usar: Detectar lentidão na lógica de negócio

8-9. **Pagamentos/Estornos Last Hour (Stat Panels)**
   - O que mostra: Total criado na última hora
   - Como usar: Monitorar volume de transações

10. **Pagamento - Taxa de Aprovação (%)**
   - O que mostra: % de pagamentos autorizados (não negados)
   - Como usar:
     - Meta típica: **> 70%** (depende do negócio)
     - Se cair muito: problema no adquirente ou fraudes?

11. **Estorno - Taxa de Sucesso (%)**
   - O que mostra: % de estornos cancelados com sucesso
   - Como usar: Meta = **> 95%** (estornos raramente devem falhar)

**Quando usar este dashboard**:
- ✅ Monitoramento de negócio (KPIs)
- ✅ Detectar anomalias (taxa de aprovação caiu?)
- ✅ Investigar DLQ (itens falhando?)
- ✅ Validar resiliência (Circuit Breaker funcionando?)

---

### **3. Dicas de Uso do Grafana**

#### **Time Range (canto superior direito)**

```
Last 5 minutes   → Monitoramento em tempo real
Last 1 hour      → Investigar problema recente
Last 24 hours    → Análise diária
Last 7 days      → Tendências semanais
Custom range     → Análise específica (ex: horário do deploy)
```

#### **Auto-refresh**

- Dashboards configurados para **refresh a cada 5 segundos**
- Ícone de refresh ao lado do time range
- Útil para deixar em monitor durante operação

#### **Variables (template variables)**

- `$application` = "toolschallenge" (filtrar métricas da nossa app)
- Dropdown no topo do dashboard

#### **Zoom e Pan**

- **Zoom**: Arrastar no gráfico
- **Pan**: Shift + Arrastar
- **Reset zoom**: Clicar duas vezes no gráfico

#### **Inspect Panel**

- **Clicar no título do painel** → **Inspect** → **Data**
- Ver dados raw da query
- Copiar query PromQL para usar no Prometheus

---

## 🔥 Prometheus - Queries e Métricas

### **1. Acessar Prometheus**

```bash
http://localhost:9090
```

**Interface simples**: Campo de query PromQL + botão "Execute"

---

### **2. PromQL Básico**

#### **Sintaxe de Query**

```promql
# Métrica simples (último valor)
http_server_requests_seconds_count

# Filtrar por label
http_server_requests_seconds_count{uri="/pagamentos"}

# Filtrar múltiplos labels (AND)
http_server_requests_seconds_count{uri="/pagamentos", status="200"}

# Regex
http_server_requests_seconds_count{uri=~"/pagamentos.*"}

# Negar
http_server_requests_seconds_count{status!="200"}
```

---

### **3. Queries Úteis - Copia e Cola**

#### **📊 HTTP Metrics**

**1. Total de requisições por endpoint**
```promql
sum(http_server_requests_seconds_count{application="toolschallenge"}) by (uri)
```

**2. Taxa de requisições (req/segundo) últimos 5min**
```promql
sum(rate(http_server_requests_seconds_count{application="toolschallenge"}[5m])) by (uri)
```

**3. Latência p95 por endpoint**
```promql
histogram_quantile(0.95, 
  sum(rate(http_server_requests_seconds_bucket{application="toolschallenge"}[5m])) by (le, uri)
)
```

**4. Taxa de erro 5xx**
```promql
sum(rate(http_server_requests_seconds_count{application="toolschallenge", status=~"5.."}[5m]))
```

**5. Taxa de sucesso (%)**
```promql
100 * (
  sum(rate(http_server_requests_seconds_count{application="toolschallenge", status!~"5.."}[5m])) 
  / 
  sum(rate(http_server_requests_seconds_count{application="toolschallenge"}[5m]))
)
```

---

#### **💼 Business Metrics**

**6. Pagamentos criados (últimos 5min)**
```promql
sum(rate(pagamento_criados_total{application="toolschallenge"}[5m])) by (status)
```

**7. Estornos criados (últimos 5min)**
```promql
sum(rate(estorno_criados_total{application="toolschallenge"}[5m])) by (status)
```

**8. Taxa de aprovação de pagamentos (%)**
```promql
100 * (
  sum(rate(pagamento_criados_total{application="toolschallenge", status="AUTORIZADO"}[5m])) 
  / 
  sum(rate(pagamento_criados_total{application="toolschallenge"}[5m]))
)
```

**9. DLQ rate por tipo**
```promql
sum(rate(reprocessamento_dlq_total{application="toolschallenge"}[5m])) by (tipo)
```

**10. Total na DLQ (última hora)**
```promql
sum(increase(reprocessamento_dlq_total{application="toolschallenge"}[1h])) by (tipo)
```

---

#### **🛡️ Resiliência**

**11. Estado do Circuit Breaker**
```promql
circuit_breaker_adquirente_state{application="toolschallenge"}
```
*Resultado: 0 = CLOSED, 1 = OPEN, 2 = HALF_OPEN*

**12. Taxa de falhas do Circuit Breaker**
```promql
resilience4j_circuitbreaker_failure_rate{name="adquirente"}
```

**13. Chamadas bloqueadas pelo CB**
```promql
sum(rate(resilience4j_circuitbreaker_calls_seconds_count{name="adquirente", kind="not_permitted"}[5m]))
```

**14. Retries executados**
```promql
sum(rate(resilience4j_retry_calls_seconds_count{name="adquirente"}[5m])) by (kind)
```

---

#### **🖥️ JVM Metrics**

**15. Heap usado (MB)**
```promql
jvm_memory_used_bytes{application="toolschallenge", area="heap"} / 1024 / 1024
```

**16. Taxa de GC (collections/segundo)**
```promql
rate(jvm_gc_pause_seconds_count{application="toolschallenge"}[5m])
```

**17. Threads ativas**
```promql
jvm_threads_live_threads{application="toolschallenge"}
```

**18. Conexões DB ativas**
```promql
hikaricp_connections_active{pool="HikariPool-1"}
```

---

### **4. Dicas PromQL**

#### **Funções de Tempo**

```promql
rate(metric[5m])      # Taxa por segundo (para Counters)
increase(metric[1h])  # Incremento total (para Counters)
avg_over_time(metric[5m])  # Média (para Gauges)
max_over_time(metric[5m])  # Máximo (para Gauges)
```

#### **Agregações**

```promql
sum(metric) by (label)     # Somar agrupando por label
avg(metric) by (label)     # Média
max(metric)                # Máximo global
count(metric)              # Contar séries
```

#### **Matemática**

```promql
metric1 / metric2          # Divisão (para calcular %)
metric * 100               # Multiplicação
metric1 - metric2          # Subtração
```

#### **Percentis (Histograms)**

```promql
histogram_quantile(0.50, ...)  # p50 (mediana)
histogram_quantile(0.95, ...)  # p95
histogram_quantile(0.99, ...)  # p99
```

---

### **5. Targets (Status de Scraping)**

**Menu**: Status → Targets

**O que ver**:
- `toolschallenge-api` deve estar **UP** (verde)
- Last Scrape: < 30 segundos atrás
- Scrape Duration: < 100ms (normal)
- Errors: Vazio (se tiver erro, scraping falhou)

**Validar via API**:
```bash
curl http://localhost:9090/api/v1/targets
```

---

## 🔍 Jaeger - Distributed Tracing

### **1. Acessar Jaeger**

```bash
http://localhost:16686
```

---

### **2. Interface Jaeger**

**Tela principal**: Search Traces

**Campos**:
- **Service**: Selecionar `toolschallenge`
- **Operation**: Selecionar operação específica (ex: `POST /pagamentos`)
- **Tags**: Filtros customizados (ex: `http.status_code=500`)
- **Lookback**: Quanto tempo atrás buscar (Last Hour, Last 24h, etc)

---

### **3. Buscar Traces**

#### **Caso 1: Ver todas as requisições recentes**

1. Service: `toolschallenge`
2. Lookback: **Last 1 Hour**
3. Clicar **Find Traces**

**Resultado**: Lista de traces com:
- TraceID (identificador único)
- Duração total (ex: 350ms)
- Número de spans (ex: 5 spans)
- Timestamp

---

#### **Caso 2: Buscar requisições lentas**

1. Service: `toolschallenge`
2. **Min Duration**: 500ms (só traces > 500ms)
3. Clicar **Find Traces**

**Resultado**: Só traces lentos (útil para investigar lentidão)

---

#### **Caso 3: Buscar por Correlation ID**

1. **Tags**: `correlationId=d4c062ef-77ba-489f-9a05-86850c76fc90`
2. Clicar **Find Traces**

**Resultado**: Trace exato daquela requisição

**Quando usar**: Cliente reportou erro e você tem o Correlation ID do log

---

### **4. Analisar um Trace**

**Clicar em um trace da lista** → Abre detalhes

**O que você vê**:

#### **Timeline (Gantt Chart)**

Exemplo de trace de `POST /pagamentos`:

```
┌─────────────────────────────────────────────────────────┐
│ http-server-span (350ms)                                │  ← Requisição HTTP total
│   ├─ PagamentoService.criar (280ms)                     │  ← Lógica de negócio
│   │   ├─ AdquirenteService.autorizarPagamento (200ms)   │  ← Chamada externa
│   │   └─ Repository.save (50ms)                         │  ← Salvar no DB
│   └─ OutboxService.salvar (30ms)                        │  ← Publicar evento
└─────────────────────────────────────────────────────────┘
```

**Interpretação**:
- **Total**: 350ms
- **Gargalo**: `AdquirenteService.autorizarPagamento` (200ms = 57% do tempo)
- **Conclusão**: Lentidão vem do adquirente externo

---

#### **Span Details**

**Clicar em um span** → Abre detalhes:

**Tags**:
- `http.method`: POST
- `http.url`: /pagamentos
- `http.status_code`: 201
- `correlationId`: d4c062ef-...

**Logs** (se houver):
- Eventos internos do span (ex: "Circuit Breaker ativado")

**Process** (metadata):
- `service.name`: toolschallenge
- `host.name`: fabio-pc
- `ip`: 192.168.1.10

---

### **5. Casos de Uso Jaeger**

#### **Caso 1: Requisição lenta - onde está o gargalo?**

**Problema**: Cliente reportou que POST /pagamentos demorou 5 segundos

**Solução**:
1. Buscar trace com Min Duration: 4000ms
2. Encontrar o trace de 5s
3. Analisar timeline:
   - Se `AdquirenteService` demorou 4.8s → Problema no adquirente
   - Se `Repository.save` demorou 4.5s → Problema no DB (query lenta?)
   - Se `OutboxService` demorou 4s → Problema no Kafka?

---

#### **Caso 2: Erro 500 - qual método quebrou?**

**Problema**: Cliente recebeu 500 Internal Server Error

**Solução**:
1. Tags: `http.status_code=500`
2. Encontrar trace
3. Ver span com `error=true` (marcado em vermelho)
4. Ver logs do span para stacktrace

---

#### **Caso 3: Circuit Breaker aberto - quantas requests afetadas?**

**Problema**: Circuit Breaker abriu, quantas requests usaram fallback?

**Solução**:
1. Tags: `resilience4j.circuitbreaker.name=adquirente`
2. Ver quantos traces têm span de fallback
3. Analisar duração (fallback deve ser rápido, ex: 50ms)

---

### **6. Correlation ID - Rastreabilidade E2E**

#### **Como funciona**:

Toda requisição HTTP gera um **Correlation ID único** que:
- Aparece nos **logs** (facilita busca)
- Aparece no **Jaeger** como tag (permite buscar trace)
- É propagado para **chamadas externas** (microserviços)

#### **Exemplo prático**:

**1. Cliente faz requisição**:
```bash
curl -X POST http://localhost:8080/pagamentos \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000" \
  -d '{"descricao":"Test","valor":100.00,"tipoPagamento":"CARTAO_CREDITO"}'
```

**2. Aplicação gera Correlation ID**: `d4c062ef-77ba-489f-9a05-86850c76fc90`

**3. Logs mostram o ID**:
```
2025-11-03 14:30:15 INFO [d4c062ef] PagamentoService - Criando pagamento
2025-11-03 14:30:15 INFO [d4c062ef] AdquirenteService - Autorizando pagamento
2025-11-03 14:30:16 ERROR [d4c062ef] AdquirenteService - Timeout ao chamar adquirente
```

**4. Buscar no Jaeger**: `correlationId=d4c062ef-77ba-489f-9a05-86850c76fc90`

**Benefício**: Rastreabilidade completa de uma requisição (logs + traces + métricas)

---

## 🩺 Actuator - Health Checks

### **1. Acessar Actuator**

**Base URL**: http://localhost:8080/atuador

**Endpoints principais**:

```bash
# Status geral
http://localhost:8080/atuador/health

# Métricas disponíveis
http://localhost:8080/atuador/metrics

# Métrica específica
http://localhost:8080/atuador/metrics/http.server.requests

# Métricas Prometheus
http://localhost:8080/atuador/prometheus

# Circuit Breakers
http://localhost:8080/atuador/circuitbreakers

# Histórico de eventos CB
http://localhost:8080/atuador/circuitbreakerevents
```

---

### **2. Health Endpoint**

```bash
curl http://localhost:8080/atuador/health | jq
```

**Resposta exemplo**:
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "redis": {
      "status": "UP",
      "details": {
        "version": "7.4.6"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500000000000,
        "free": 250000000000,
        "threshold": 10485760,
        "exists": true
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

**Interpretação**:
- `status: UP` → Aplicação saudável
- `status: DOWN` → Algum componente falhou (DB, Redis, etc)

**Quando usar**:
- ✅ Health checks de load balancer
- ✅ Monitoramento de uptime
- ✅ Validar que DB/Redis estão acessíveis

---

### **3. Circuit Breakers Endpoint**

```bash
curl http://localhost:8080/atuador/circuitbreakers | jq
```

**Resposta exemplo**:
```json
{
  "circuitBreakers": {
    "adquirente": {
      "state": "HALF_OPEN",
      "failureRate": "45.5%",
      "slowCallRate": "10.2%",
      "bufferedCalls": 10,
      "failedCalls": 5,
      "slowCalls": 1,
      "notPermittedCalls": 3
    }
  }
}
```

**Interpretação**:
- `state: CLOSED` → Tudo OK
- `state: OPEN` → Sistema externo down, fallback ativo
- `state: HALF_OPEN` → Testando recuperação
- `failureRate`: Taxa de falhas (> 50% abre o CB)
- `notPermittedCalls`: Chamadas bloqueadas (CB OPEN)

---

### **4. Metrics Endpoint**

**Listar todas as métricas**:
```bash
curl http://localhost:8080/atuador/metrics | jq '.names'
```

**Ver métrica específica**:
```bash
curl http://localhost:8080/atuador/metrics/http.server.requests | jq
```

**Resposta** (truncada):
```json
{
  "name": "http.server.requests",
  "measurements": [
    {"statistic": "COUNT", "value": 1523.0},
    {"statistic": "TOTAL_TIME", "value": 45.234},
    {"statistic": "MAX", "value": 2.1}
  ],
  "availableTags": [
    {"tag": "uri", "values": ["/pagamentos", "/estornos", "/atuador/health"]},
    {"tag": "status", "values": ["200", "201", "400", "404", "500"]},
    {"tag": "method", "values": ["GET", "POST"]}
  ]
}
```

---

## 🎯 Casos de Uso Práticos

### **Caso 1: "A API está lenta!"**

**Objetivo**: Identificar qual endpoint está lento e por quê

**Passo a Passo**:

1. **Grafana** → **HTTP Metrics Dashboard**
   - Ver painel **Latency Percentiles**
   - Identificar endpoint com p99 > 1s

2. **Prometheus** → Query específica:
   ```promql
   histogram_quantile(0.99, 
     sum(rate(http_server_requests_seconds_bucket{uri="/pagamentos"}[5m])) by (le)
   )
   ```
   - Confirmar latência alta (ex: 2.5s)

3. **Jaeger** → Buscar traces lentos:
   - Service: `toolschallenge`
   - Operation: `POST /pagamentos`
   - Min Duration: `2000ms`
   - Analisar timeline para identificar gargalo

4. **Resultado**: Descobrir que `AdquirenteService` está demorando 2.3s (92% do tempo)

5. **Ação**: Investigar logs do adquirente, verificar Circuit Breaker, considerar timeout mais curto

---

### **Caso 2: "Muitos pagamentos negados!"**

**Objetivo**: Analisar por que taxa de aprovação caiu de 80% para 50%

**Passo a Passo**:

1. **Grafana** → **Business Metrics Dashboard**
   - Ver painel **Pagamento - Taxa de Aprovação**
   - Confirmar queda (ex: de 80% para 52%)
   - Ver **Pagamentos Rate by Status**:
     - AUTORIZADO: 10/s (normal)
     - NEGADO: 15/s (dobrou!) ← Problema aqui

2. **Prometheus** → Query para confirmar:
   ```promql
   sum(rate(pagamento_criados_total{status="NEGADO"}[5m]))
   ```
   - Resultado: 0.25/s (normal era 0.10/s)

3. **Logs** → Buscar logs de negação:
   ```bash
   grep "NEGADO" application.log | tail -50
   ```
   - Ver padrão: "Saldo insuficiente" vs "Cartão bloqueado"

4. **Jaeger** → Buscar traces de pagamentos negados:
   - Tags: `http.url=/pagamentos` AND `status=NEGADO`
   - Analisar response do adquirente

5. **Resultado**: Adquirente retornando "fraude detectada" em massa (possível falso positivo)

6. **Ação**: Contatar adquirente, ajustar regras de fraude

---

### **Caso 3: "Circuit Breaker está abrindo muito!"**

**Objetivo**: Entender por que Circuit Breaker está abrindo e se está protegendo corretamente

**Passo a Passo**:

1. **Grafana** → **Resilience4j Dashboard**
   - Ver **Circuit Breaker State** (gauge): Estado = 1 (OPEN) ← Confirmado
   - Ver **Circuit Breaker Calls**:
     - Failed: 80%
     - Successful: 10%
     - Not Permitted: 10% (bloqueadas pelo CB)

2. **Actuator** → Status do CB:
   ```bash
   curl http://localhost:8080/atuador/circuitbreakers
   ```
   - `failureRate: 82.5%` (threshold é 50%, por isso abriu)
   - `state: OPEN`

3. **Prometheus** → Query histórica:
   ```promql
   circuit_breaker_adquirente_state
   ```
   - Graph → Ver quando mudou de 0 (CLOSED) para 1 (OPEN)
   - Exemplo: Abriu às 14:35, ficou em OPEN por 10s, foi para HALF_OPEN

4. **Logs** → Ver quando CB abriu:
   ```bash
   grep "Circuit Breaker" application.log
   ```
   - Logs: "Circuit Breaker OPEN - Adquirente indisponível"

5. **Jaeger** → Ver traces com fallback:
   - Tags: `resilience4j.circuitbreaker.name=adquirente`
   - Ver quanto tempo durou fallback (deve ser rápido, ex: 50ms)

6. **Resultado**: Adquirente teve instabilidade às 14:35 (82% de falhas), CB protegeu corretamente

7. **Ação**: Verificar se adquirente se recuperou, ajustar threshold se necessário

---

### **Caso 4: "DLQ está crescendo!"**

**Objetivo**: Investigar por que itens estão indo para Dead Letter Queue

**Passo a Passo**:

1. **Grafana** → **Business Metrics Dashboard**
   - Ver **DLQ Total Last Hour**: 45 itens (anormal, normal é 0-5)
   - Ver **DLQ Rate by Type**:
     - `tipo=pagamento`: 0.5/s
     - `tipo=estorno`: 0.1/s

2. **Prometheus** → Query:
   ```promql
   sum(increase(reprocessamento_dlq_total[1h])) by (tipo)
   ```
   - Resultado: pagamento=30, estorno=15

3. **Logs** → Ver por que foram para DLQ:
   ```bash
   grep "Enviando para DLQ" application.log
   ```
   - Logs: "Tentativas esgotadas (3/3): Timeout ao chamar adquirente"

4. **Database** → Query na tabela `pagamento`:
   ```sql
   SELECT id, status, tentativas_processamento 
   FROM pagamento 
   WHERE status = 'PENDENTE' AND tentativas_processamento >= 3;
   ```
   - Ver IDs específicos que falharam

5. **Jaeger** → Buscar trace de um item da DLQ:
   - TraceID do log
   - Ver onde quebrou (timeout no AdquirenteService)

6. **Resultado**: Adquirente teve timeout em todas as 3 tentativas de retry

7. **Ação**:
   - Verificar se adquirente está saudável
   - Considerar aumentar timeout (atual 5s → 10s?)
   - Reprocessar manualmente itens da DLQ após fix

---

### **Caso 5: "JVM com OutOfMemoryError!"**

**Objetivo**: Identificar memory leak antes do OOM

**Passo a Passo**:

1. **Grafana** → **JVM Micrometer Dashboard**
   - Ver **JVM Memory Pools**:
     - Heap Used: 1.8GB / 2GB (90% usado!) ← Problema
     - Old Gen: 1.6GB (crescendo continuamente)
   - Ver **Garbage Collection**:
     - GC Count: 50/min (muito alto!)
     - GC Pause Time: 800ms (usuário vai sentir)

2. **Prometheus** → Query:
   ```promql
   jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}
   ```
   - Resultado: 0.92 (92% de heap usado)

3. **Actuator** → Forçar GC (teste):
   ```bash
   curl -X POST http://localhost:8080/atuador/gc
   ```
   - Se heap não diminuir significativamente = memory leak

4. **Heap Dump** (para análise profunda):
   ```bash
   jmap -dump:live,format=b,file=heap.bin <PID>
   ```
   - Analisar com Eclipse MAT ou VisualVM

5. **Resultado**: Encontrar classes com muitas instâncias (ex: 100k objetos `PagamentoDTO`)

6. **Ação**:
   - Investigar código (cache infinito?)
   - Aumentar heap temporariamente (-Xmx4g)
   - Fixar leak

---

## 🛠️ Troubleshooting com Observabilidade

### **Problema 1: "Não vejo métricas no Grafana"**

**Checklist**:

1. **Aplicação está rodando?**
   ```bash
   curl http://localhost:8080/atuador/health
   ```
   - Se erro → Aplicação down

2. **Prometheus está coletando?**
   - Acessar http://localhost:9090/targets
   - `toolschallenge-api` deve estar UP (verde)
   - Se DOWN → Verificar firewall, endereço (host.docker.internal)

3. **Métricas estão sendo exportadas?**
   ```bash
   curl http://localhost:8080/atuador/prometheus
   ```
   - Deve retornar texto com métricas (ex: `http_server_requests_seconds_count{...}`)

4. **Grafana está conectado ao Prometheus?**
   - Grafana → Configuration → Data Sources → Prometheus
   - URL deve ser `http://prometheus:9090`
   - Clicar "Test" → Deve aparecer "Data source is working"

5. **Dashboard tem dados?**
   - Verificar time range (últimas 5min? última 1h?)
   - Verificar filtro `$application` = "toolschallenge"

---

### **Problema 2: "Não vejo traces no Jaeger"**

**Checklist**:

1. **Aplicação está enviando traces?**
   - Ver logs de startup:
     ```
     Micrometer Tracing enabled
     Jaeger exporter configured
     ```

2. **Jaeger está rodando?**
   ```bash
   curl http://localhost:16686/api/services
   ```
   - Deve retornar lista de services

3. **Service `toolschallenge` aparece?**
   - Se não aparecer → Aplicação não está enviando traces
   - Verificar configuração:
     ```yaml
     management:
       tracing:
         sampling:
           probability: 1.0  # 100% de sampling
     ```

4. **Fazer requisição e buscar trace**:
   ```bash
   curl -X POST http://localhost:8080/pagamentos \
     -H "Idempotency-Key: $(uuidgen)" \
     -H "Content-Type: application/json" \
     -d '{"descricao":"Test","valor":100,"tipoPagamento":"CARTAO_CREDITO"}'
   ```
   - Buscar no Jaeger (Lookback: Last 5 minutes)

---

### **Problema 3: "Circuit Breaker não abre mesmo com falhas"**

**Checklist**:

1. **Verificar configuração**:
   ```yaml
   resilience4j:
     circuitbreaker:
       instances:
         adquirente:
           failure-rate-threshold: 50  # 50% de falhas
           minimum-number-of-calls: 5   # Precisa de 5 chamadas antes de calcular
   ```

2. **Gerar falhas suficientes**:
   - CB só calcula taxa após `minimum-number-of-calls` (5)
   - Fazer pelo menos 5 requisições
   - Pelo menos 3 devem falhar (50%)

3. **Ver estado no Actuator**:
   ```bash
   curl http://localhost:8080/atuador/circuitbreakers
   ```

4. **Ver gauge no Prometheus**:
   ```promql
   circuit_breaker_adquirente_state
   ```
   - 0 = CLOSED, 1 = OPEN, 2 = HALF_OPEN

---

## 📚 Resumo - Cheat Sheet

### **Acesso Rápido**

```bash
# Grafana (dashboards visuais)
http://localhost:3000
admin / admin123

# Prometheus (queries PromQL)
http://localhost:9090

# Jaeger (distributed tracing)
http://localhost:16686

# Actuator (health checks)
http://localhost:8080/atuador/health

# Swagger (documentação API)
http://localhost:8080/swagger-ui.html
```

### **Queries PromQL Top 5**

```promql
# 1. Taxa de requisições HTTP
sum(rate(http_server_requests_seconds_count[5m])) by (uri)

# 2. Latência p99
histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, uri))

# 3. Taxa de erro 5xx
sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m]))

# 4. Estado do Circuit Breaker
circuit_breaker_adquirente_state

# 5. Pagamentos criados
sum(rate(pagamento_criados_total[5m])) by (status)
```

### **Dashboards Essenciais**

1. **HTTP Metrics** → Monitoramento diário de performance
2. **Business Metrics** → KPIs de negócio e resiliência
3. **JVM Micrometer** → Saúde da JVM (memory, GC, threads)
4. **Resilience4j** → Circuit Breaker, Retry, Bulkhead

### **Fluxo de Investigação**

```
Problema reportado
    ↓
1. Grafana (visão geral - dashboard relevante)
    ↓
2. Prometheus (query específica para confirmar)
    ↓
3. Jaeger (trace individual para debugar)
    ↓
4. Logs (detalhes finais - stacktrace, mensagens)
```

---

## 🎓 Próximos Passos

### **Para Aprender Mais**

1. **PromQL**:
   - [Prometheus Query Basics](https://prometheus.io/docs/prometheus/latest/querying/basics/)
   - [PromQL Examples](https://prometheus.io/docs/prometheus/latest/querying/examples/)

2. **Grafana**:
   - [Grafana Dashboards Best Practices](https://grafana.com/docs/grafana/latest/dashboards/)
   - [Grafana Variables](https://grafana.com/docs/grafana/latest/dashboards/variables/)

3. **Jaeger**:
   - [Jaeger Architecture](https://www.jaegertracing.io/docs/1.50/architecture/)
   - [OpenTelemetry Tracing](https://opentelemetry.io/docs/concepts/observability-primer/#distributed-traces)

4. **Resilience4j**:
   - [Circuit Breaker Pattern](https://resilience4j.readme.io/docs/circuitbreaker)
   - [Metrics Integration](https://resilience4j.readme.io/docs/micrometer)

---

**Autor**: ToolsChallenge Team  
**Data**: 03/11/2025  
**Versão**: 1.0
