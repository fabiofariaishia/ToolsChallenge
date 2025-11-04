# 📘 ToolsChallenge - API de Pagamentos Sicredi#  ToolsChallenge - API de Pagamentos Sicredi



> **Documentação Técnica Completa** | *Para regras de desenvolvimento, consulte [copilot-instructions.md](.github/instructions/copilot-instructions.md)*> **Documentação Técnica Completa** | *Para regras de desenvolvimento, consulte [copilot-instructions.md](.github/instructions/copilot-instructions.md)*



## 📋 Índice##  Índice



1. [Visão Geral](#-visão-geral)1. [Visão Geral](#-visão-geral)

2. [Stack Tecnológico](#%EF%B8%8F-stack-tecnológico)2. [Stack Tecnológico](#-stack-tecnológico)

3. [Estrutura de Pastas](#-estrutura-de-pastas)3. [Estrutura de Pastas](#-estrutura-de-pastas)

4. [Banco de Dados](#%EF%B8%8F-banco-de-dados)4. [Banco de Dados](#-banco-de-dados)

5. [Mensageria (Kafka)](#-mensageria-kafka)5. [Mensageria (Kafka)](#-mensageria-kafka)

6. [Cache e Locks Distribuídos](#-cache-e-locks-distribuídos)6. [Cache e Locks Distribuídos](#-cache-e-locks-distribuídos)

7. [Resiliência (Resilience4j)](#%EF%B8%8F-resiliência-resilience4j)7. [Resiliência (Resilience4j)](#-resiliência-resilience4j)

8. [Observabilidade](#-observabilidade)8. [Observabilidade](#-observabilidade)

9. [APIs e Endpoints](#-apis-e-endpoints)9. [APIs e Endpoints](#-apis-e-endpoints)

10. [Configuração e Ambiente](#%EF%B8%8F-configuração-e-ambiente)10. [Configuração e Ambiente](#-configuração-e-ambiente)

11. [Testes](#-testes)11. [Testes](#-testes)

12. [Deploy e CI/CD](#-deploy-e-cicd)12. [Deploy e CI/CD](#-deploy-e-cicd)

13. [Monitoramento](#-monitoramento)13. [Monitoramento](#-monitoramento)

14. [Troubleshooting](#-troubleshooting)14. [Troubleshooting](#-troubleshooting)

15. [FAQ](#-faq)15. [FAQ](#-faq)

16. [Referências](#-referências)16. [Referências](#-referências)



------



## 🎯 Visão Geral##  Visão Geral



**ToolsChallenge** é uma API REST de processamento de pagamentos desenvolvida para o **Sicredi**, implementando padrões de arquitetura moderna, resiliente e escalável baseada em **Monolito Modular** com preparação para evolução para **Microserviços**.**ToolsChallenge** é uma API REST de processamento de pagamentos desenvolvida para o **Sicredi**, implementando padrões de arquitetura moderna, resiliente e escalável baseada em **Monolito Modular** com preparação para evolução para **Microserviços**.



### Características Principais### Características Principais



- 🔐 **Idempotência**: Chaves idempotentes em todos os endpoints mutáveis-  **Idempotência**: Chaves idempotentes em todos os endpoints mutáveis

- 🔄 **Outbox Pattern**: Garantia de entrega de eventos via transactional outbox-  **Outbox Pattern**: Garantia de entrega de eventos via transactional outbox

- 🔒 **Locks Distribuídos**: Prevenção de race conditions com Redisson-  **Locks Distribuídos**: Prevenção de race conditions com Redisson

- 🛡️ **Resiliência**: Circuit Breaker, Retry e Bulkhead com Resilience4j-  **Resiliência**: Circuit Breaker, Retry e Bulkhead com Resilience4j

- 📊 **Auditoria**: Registro completo de todos os eventos de negócio-  **Auditoria**: Registro completo de todos os eventos de negócio

- 🚀 **Performance**: Cache Redis e processamento assíncrono via Kafka-  **Performance**: Cache Redis e processamento assíncrono via Kafka



> **Regras de Desenvolvimento**: Para instruções completas sobre como desenvolver seguindo os padrões do projeto, consulte [.github/instructions/copilot-instructions.md](.github/instructions/copilot-instructions.md)> **Regras de Desenvolvimento**: Para instruções completas sobre como desenvolver seguindo os padrões do projeto, consulte [.github/instructions/copilot-instructions.md](.github/instructions/copilot-instructions.md)



------

## 🛠️ Stack Tecnológico

### Backend

| Tecnologia | Versão | Propósito |
|-----------|--------|-----------|
| **Java** | 17 | Linguagem base |
| **Spring Boot** | 3.5.7 | Framework principal |
| **Spring Data JPA** | 3.5.7 | Persistência ORM |
| **Spring Kafka** | 3.5.7 | Mensageria |
| **Spring Actuator** | 3.5.7 | Monitoramento |

### Persistência

| Tecnologia | Versão | Propósito |
|-----------|--------|-----------|
| **PostgreSQL** | 16 | Banco de dados principal |
| **Flyway** | 10.x | Migrações de schema |
| **Redis** | 7.x | Cache e locks distribuídos |

### Mensageria

| Tecnologia | Versão | Propósito |
|-----------|--------|-----------|
| **Apache Kafka** | 3.6.x | Event streaming |
| **Spring Kafka** | 3.5.7 | Integração com Kafka |

### Resiliência

| Tecnologia | Versão | Propósito |
|-----------|--------|-----------|
| **Resilience4j** | 2.2.0 | Circuit Breaker, Retry, Bulkhead |
| **Redisson** | 3.35.0 | Locks distribuídos |

### Observabilidade

| Tecnologia | Versão | Propósito |
|-----------|--------|-----------|
| **Micrometer** | 1.13.x | Métricas |
| **Prometheus** | 2.x | Coleta de métricas |
| **Springdoc OpenAPI** | 2.6.0 | Documentação Swagger |

### Build e Testes

| Tecnologia | Versão | Propósito |
|-----------|--------|-----------|
| **Maven** | 3.9.x | Build tool |
| **JUnit 5** | 5.10.x | Testes unitários |
| **Testcontainers** | 1.19.x | Testes de integração |
| **Lombok** | 1.18.x | Redução de boilerplate |

---

## 📁 Estrutura de Pastas

```
ToolsChallenge/
│
├── .github/
│   └── instructions/
│       └── copilot-instructions.md    # Regras de desenvolvimento
│
├── docker/
│   ├── postgres/init.sql              # Scripts iniciais PostgreSQL
│   ├── kafka/                         # Configurações Kafka
│   └── redis/                         # Configurações Redis
│
├── docs/
│   ├── AUDITORIA.md                   # Sistema de auditoria
│   ├── LOCK_DISTRIBUIDO.md            # Locks distribuídos
│   ├── TESTES_IDEMPOTENCIA.md         # Testes idempotência
│   └── TESTES_OUTBOX_PATTERN.md       # Testes Outbox Pattern
│
├── src/
│   ├── main/
│   │   ├── java/br/com/sicredi/toolschallenge/
│   │   │   ├── adquirente/            # Módulo Adquirente
│   │   │   ├── pagamento/             # Módulo Pagamento
│   │   │   ├── estorno/               # Módulo Estorno
│   │   │   ├── infra/                 # Infraestrutura
│   │   │   └── shared/                # Compartilhado
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/          # Flyway migrations
│   └── test/
│       └── java/.../toolschallenge/
│           ├── integration/
│           └── unit/
│
├── docker-compose.yml
├── pom.xml
├── README.md                          # Este arquivo
├── EXEMPLOS_API_PAGAMENTO.md
├── EXEMPLOS_API_ESTORNO.md
└── QUICKSTART.md
```

---

## 🗄️ Banco de Dados

### Schema PostgreSQL

#### Tabela: `pagamento`

```sql
CREATE TABLE pagamento (
    id BIGSERIAL PRIMARY KEY,
    descricao VARCHAR(255) NOT NULL,
    valor DECIMAL(19,2) NOT NULL,
    tipo_pagamento VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    nsu VARCHAR(50),
    codigo_autorizacao VARCHAR(50),
    data_criacao TIMESTAMP NOT NULL,
    data_atualizacao TIMESTAMP
);

CREATE INDEX idx_pagamento_status ON pagamento(status);
CREATE INDEX idx_pagamento_nsu ON pagamento(nsu);
```

#### Tabela: `estorno`

```sql
CREATE TABLE estorno (
    id BIGSERIAL PRIMARY KEY,
    pagamento_id BIGINT NOT NULL,
    valor DECIMAL(19,2) NOT NULL,
    motivo VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    data_criacao TIMESTAMP NOT NULL,
    data_atualizacao TIMESTAMP,
    FOREIGN KEY (pagamento_id) REFERENCES pagamento(id)
);

CREATE INDEX idx_estorno_pagamento_id ON estorno(pagamento_id);
CREATE INDEX idx_estorno_status ON estorno(status);
```

#### Tabela: `idempotencia`

```sql
CREATE TABLE idempotencia (
    id BIGSERIAL PRIMARY KEY,
    chave VARCHAR(255) NOT NULL UNIQUE,
    resposta TEXT,
    status_code INTEGER,
    timestamp TIMESTAMP NOT NULL,
    expira_em TIMESTAMP NOT NULL
);

CREATE INDEX idx_idempotencia_expira_em ON idempotencia(expira_em);
```

#### Tabela: `outbox_evento`

```sql
CREATE TABLE outbox_evento (
    id BIGSERIAL PRIMARY KEY,
    agregado_tipo VARCHAR(50) NOT NULL,
    agregado_id BIGINT NOT NULL,
    tipo_evento VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    data_criacao TIMESTAMP NOT NULL,
    data_publicacao TIMESTAMP
);

CREATE INDEX idx_outbox_status ON outbox_evento(status);
CREATE INDEX idx_outbox_data_criacao ON outbox_evento(data_criacao);
```

#### Tabela: `evento_auditoria`

```sql
CREATE TABLE evento_auditoria (
    id BIGSERIAL PRIMARY KEY,
    tipo_evento VARCHAR(100) NOT NULL,
    agregado_tipo VARCHAR(50) NOT NULL,
    agregado_id BIGINT NOT NULL,
    payload TEXT NOT NULL,
    data_evento TIMESTAMP NOT NULL
);

CREATE INDEX idx_auditoria_agregado ON evento_auditoria(agregado_tipo, agregado_id);
CREATE INDEX idx_auditoria_tipo_evento ON evento_auditoria(tipo_evento);
CREATE INDEX idx_auditoria_data_evento ON evento_auditoria(data_evento);
```

### Flyway Migrations

Migrações localizadas em `src/main/resources/db/migration/`:

1. **V1**: Criar tabela `pagamento`
2. **V2**: Criar tabela `estorno`
3. **V3**: Criar tabela `idempotencia`
4. **V4**: Criar tabela `outbox_evento`
5. **V5**: Criar tabela `evento_auditoria`

**Execução**: Automática no startup via `spring.flyway.enabled=true`

---

## 📨 Mensageria (Kafka)

### Tópicos Kafka

| Tópico | Eventos | Consumidores |
|--------|---------|--------------|
| `pagamentos` | `PagamentoCriadoEvento`, `PagamentoStatusAlteradoEvento` | `PagamentoEventListener` (Auditoria) |
| `estornos` | `EstornoCriadoEvento`, `EstornoStatusAlteradoEvento` | `EstornoEventListener` (Auditoria) |

### Estrutura de Evento

```json
{
  "tipoEvento": "PAGAMENTO_CRIADO",
  "timestamp": "2025-11-02T10:30:00Z",
  "agregadoId": 123,
  "dados": {
    "id": 123,
    "descricao": "Compra na Loja X",
    "valor": 150.50,
    "status": "PROCESSADO",
    "nsu": "123456789",
    "codigoAutorizacao": "AUTH987654"
  }
}
```

### Configuração Kafka

**Producer**:

```yaml
spring:
  kafka:
    producer:
      key-serializer: StringSerializer
      value-serializer: JsonSerializer
      acks: all                    # Garantia de escrita
      retries: 3                   # Retry automático
```

**Consumer**:

```yaml
spring:
  kafka:
    consumer:
      group-id: pagamentos-group
      auto-offset-reset: earliest  # Processa desde início
      enable-auto-commit: false    # Controle manual de offset
      key-deserializer: StringDeserializer
      value-deserializer: JsonDeserializer
      properties:
        spring.json.trusted.packages: br.com.sicredi.toolschallenge
```

---

## 🔴 Cache e Locks Distribuídos

### Redis - Idempotência

**TTL**: 24 horas  
**Estrutura de Chave**: `idempotencia:{UUID}`

```redis
SET idempotencia:550e8400-e29b-41d4-a716-446655440000 
    '{"resposta":"{...}","statusCode":201,"timestamp":"..."}'
    EX 86400
```

### Redisson - Locks Distribuídos

**Configuração**:

```java
@Configuration
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedissonConfig {
    
    @Bean
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
              .setAddress("redis://localhost:6379")
              .setPassword("redis123")
              .setConnectionPoolSize(10)
              .setConnectionMinimumIdleSize(5);
        return Redisson.create(config);
    }
}
```

**Uso de Lock**:

```java
String lockKey = "estorno:pagamento:" + pagamentoId;
RLock lock = redissonClient.getLock(lockKey);

try {
    if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
        try {
            // Operação crítica protegida
            processarEstorno(pagamentoId);
        } finally {
            lock.unlock();
        }
    } else {
        throw new NegocioException("Operação já em andamento");
    }
} catch (InterruptedException e) {
    Thread.currentThread().interrupt();
    throw new NegocioException("Lock interrompido");
}
```

**Watchdog**: Redisson renova automaticamente locks enquanto thread está viva.

---

## 🛡️ Resiliência (Resilience4j)

### Circuit Breaker

**Configuração** (application.yml):

```yaml
resilience4j:
  circuitbreaker:
    instances:
      adquirente:
        failure-rate-threshold: 50               # 50% falhas → OPEN
        sliding-window-size: 10                  # Janela de 10 chamadas
        minimum-number-of-calls: 5               # Mínimo para calcular taxa
        wait-duration-in-open-state: 10s         # 10s em OPEN
        permitted-number-of-calls-in-half-open-state: 3
        automatic-transition-from-open-to-half-open-enabled: true
        register-health-indicator: true          # Expor em /actuator/health
```

**Estados**:

```
CLOSED → OPEN (50% failures) → HALF_OPEN (10s) → CLOSED (3/3 success)
                                              ↘ OPEN (1+ failure)
```

### Retry

**Configuração**:

```yaml
resilience4j:
  retry:
    instances:
      adquirente:
        max-attempts: 3                          # 1 original + 2 retries
        wait-duration: 500ms                     # 500ms entre tentativas
        retry-exceptions:
          - AdquirenteIndisponivelException
          - java.net.ConnectException
          - java.net.SocketTimeoutException
```

### Bulkhead (Thread Pool)

**Configuração**:

```yaml
resilience4j:
  bulkhead:
    instances:
      adquirente:
        max-thread-pool-size: 10                 # Máximo 10 threads
        core-thread-pool-size: 5                 # 5 threads core
        queue-capacity: 20                       # Fila de 20 requisições
        keep-alive-duration: 20ms
```

---

## 📊 Observabilidade

### URLs de Acesso

| Serviço | URL | Credenciais |
|---------|-----|-------------|
| **Grafana** | http://localhost:3000 | admin / admin123 |
| **Jaeger** | http://localhost:16686 | - |
| **Prometheus** | http://localhost:9090 | - |
| **Actuator** | http://localhost:8080/atuador | - |
| **Swagger** | http://localhost:8080/swagger-ui.html | - |

### Dashboards Grafana

#### Dashboards Community (3)

1. **JVM Micrometer** (UID: `jvm_micrometer_dashboard`)
   - Memory pools (heap, non-heap, eden, survivor, old gen)
   - Garbage collection (count, pause time)
   - Threads (live, daemon, peak)
   - CPU usage

2. **Spring Boot Statistics** (UID: `spring_boot_21`)
   - HTTP metrics (requests, latency, errors)
   - Logback logs by level
   - JVM stats (memory, GC, threads)
   - Tomcat metrics (sessions, threads)

3. **Resilience4j** (UID: `resilience4j_dashboard`)
   - Circuit Breaker states/calls
   - Retry attempts/failures
   - Bulkhead capacity/usage
   - Rate Limiter metrics

#### Dashboards Customizados (2)

**1. HTTP Metrics** (UID: `http_metrics_toolschallenge`)

Painéis (7):
- **Request Rate by Endpoint**: Taxa de requisições por URI
- **Latency Percentiles**: p50, p95, p99 por endpoint
- **Error Rates**: 4xx vs 5xx separados
- **Throughput by Endpoint**: Requests/segundo por URI e método
- **Success Rate Gauge**: % requisições bem-sucedidas (não 5xx)
- **Overall p99 Latency Gauge**: Latência p99 global
- **Status Code Distribution**: Pie chart de status codes

**2. Business Metrics** (UID: `business_metrics_toolschallenge`)

Painéis (11):
- **Pagamentos Rate by Status**: Rate criação por status (color-coded)
- **Estornos Rate by Status**: Rate criação por status (color-coded)
- **Circuit Breaker State Gauge**: 0=CLOSED, 1=OPEN, 2=HALF_OPEN
- **DLQ Rate by Type**: Rate envio para DLQ (pagamento vs estorno)
- **DLQ Total Last Hour**: Total enviado para DLQ na última hora
- **Pagamento Latency Percentiles**: p50/p95/p99 de criação
- **Estorno Latency Percentiles**: p50/p95/p99 de criação
- **Pagamentos Last Hour**: Stat panel com total última hora
- **Estornos Last Hour**: Stat panel com total última hora
- **Pagamento Approval Rate**: % autorizados (gauge com thresholds)
- **Estorno Success Rate**: % cancelados (gauge com thresholds)

**Features**:
- Auto-refresh: 5 segundos
- Template variable: `$application`
- Color coding: Verde=sucesso, Vermelho=erro, Amarelo=pendente

### Métricas Customizadas

| Métrica | Tipo | Descrição | Tags |
|---------|------|-----------|------|
| `pagamento_criados_total` | Counter | Total de pagamentos criados | `status` (AUTORIZADO, NEGADO, PENDENTE) |
| `estorno_criados_total` | Counter | Total de estornos criados | `status` (CANCELADO, NEGADO, PENDENTE) |
| `circuit_breaker_adquirente_state` | Gauge | Estado do Circuit Breaker | - (0=CLOSED, 1=OPEN, 2=HALF_OPEN) |
| `reprocessamento_dlq_total` | Counter | Total enviado para DLQ | `tipo` (pagamento, estorno) |
| `pagamento_criar_latency_seconds` | Histogram | Latência criação pagamento | - |
| `estorno_criar_latency_seconds` | Histogram | Latência criação estorno | - |

### Exemplos de Queries PromQL

**Taxa de Criação de Pagamentos (últimos 5min)**:
```promql
sum(rate(pagamento_criados_total{application="toolschallenge"}[5m])) by (status)
```

**Latência p99 de Pagamentos**:
```promql
histogram_quantile(0.99, sum(rate(pagamento_criar_latency_seconds_bucket{application="toolschallenge"}[5m])) by (le))
```

**Taxa de Aprovação de Pagamentos**:
```promql
sum(rate(pagamento_criados_total{application="toolschallenge", status="AUTORIZADO"}[5m])) / sum(rate(pagamento_criados_total{application="toolschallenge"}[5m]))
```

**Estado do Circuit Breaker**:
```promql
circuit_breaker_adquirente_state{application="toolschallenge"}
```

**DLQ Rate por Tipo**:
```promql
sum(rate(reprocessamento_dlq_total{application="toolschallenge"}[5m])) by (tipo)
```

**Latência HTTP p95**:
```promql
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket{application="toolschallenge"}[5m])) by (le, uri))
```

### Actuator Endpoints

**Configuração**:

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus,circuitbreakers,circuitbreakerevents
      base-path: /atuador
  endpoint:
    health:
      show-details: always
```

**Endpoints Disponíveis**:

| Endpoint | Descrição |
|----------|-----------|
| `/atuador/health` | Status de saúde (DB, Redis, Kafka) |
| `/atuador/metrics` | Métricas gerais |
| `/atuador/prometheus` | Métricas formato Prometheus |
| `/atuador/circuitbreakers` | Estado dos Circuit Breakers |
| `/atuador/circuitbreakerevents` | Histórico de eventos CB |
| `/atuador/info` | Informações da aplicação |

### Prometheus Target Status

**Validação**:

```bash
curl http://localhost:9090/api/v1/targets
```

**Target esperado**:
- Job: `toolschallenge-api`
- Instance: `host.docker.internal:8080`
- Health: `up`
- Scrape interval: `10s`
- Endpoint: `/atuador/prometheus`

### Jaeger Distributed Tracing

**Validação**:

1. Acessar http://localhost:16686
2. Selecionar service: `toolschallenge` (quando aplicação estiver rodando)
3. Buscar traces recentes
4. Verificar spans:
   - HTTP request span (entry point)
   - PagamentoService.criar span
   - AdquirenteService.autorizarPagamento span
5. Correlation ID aparece em baggage

**Exemplo de Trace**:
- TraceID: `a288846ddd700e050fba89e5de93c326`
- SpanID: `844e1ea47a07d098`
- CorrelationID: `d4c062ef-77ba-489f-9a05-86850c76fc90`

### Swagger UI

**URL**: `http://localhost:8080/swagger-ui.html`

Documentação interativa de todas as APIs com:

- Schemas de request/response
- Validações
- Códigos de erro
- Exemplos de uso

---

## 🌐 APIs e Endpoints

### Pagamentos

#### `POST /pagamentos`

Cria novo pagamento (idempotente).

**Headers**:

- `Idempotency-Key` (obrigatório): UUID único
- `Content-Type: application/json`

**Request**:

```json
{
  "descricao": "Compra na Loja X",
  "valor": 150.50,
  "tipoPagamento": "CARTAO_CREDITO"
}
```

**Response 201**:

```json
{
  "id": 123,
  "descricao": "Compra na Loja X",
  "valor": 150.50,
  "tipoPagamento": "CARTAO_CREDITO",
  "status": "PROCESSADO",
  "nsu": "123456789",
  "codigoAutorizacao": "AUTH987654",
  "dataCriacao": "2025-11-02T10:30:00Z"
}
```

#### `GET /pagamentos/{id}`

Consulta pagamento por ID.

#### `GET /pagamentos`

Lista todos os pagamentos.

### Estornos

#### `POST /pagamentos/{id}/estornos`

Solicita estorno de pagamento (idempotente).

**Headers**:

- `Idempotency-Key` (obrigatório)

**Request**:

```json
{
  "motivo": "Cliente solicitou cancelamento"
}
```

**Response 201**:

```json
{
  "id": 456,
  "pagamentoId": 123,
  "valor": 150.50,
  "motivo": "Cliente solicitou cancelamento",
  "status": "PROCESSADO",
  "dataCriacao": "2025-11-02T11:00:00Z"
}
```

#### `GET /pagamentos/{id}/estornos`

Lista estornos de um pagamento.

#### `GET /estornos/{id}`

Consulta estorno específico.

### Códigos de Erro

| Código | Descrição |
|--------|-----------|
| `400 Bad Request` | Validação falhou |
| `404 Not Found` | Recurso não encontrado |
| `409 Conflict` | Chave idempotente duplicada |
| `422 Unprocessable Entity` | Regra de negócio violada |
| `500 Internal Server Error` | Erro inesperado |
| `503 Service Unavailable` | Circuit Breaker OPEN |

**Mais exemplos**: Ver [EXEMPLOS_API_PAGAMENTO.md](EXEMPLOS_API_PAGAMENTO.md) e [EXEMPLOS_API_ESTORNO.md](EXEMPLOS_API_ESTORNO.md)

---

## ⚙️ Configuração e Ambiente

### Pré-requisitos

- Java 17+
- Docker e Docker Compose
- Maven 3.9+

### Variáveis de Ambiente

```bash
# Database
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/pagamentos
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres

# Redis
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379
SPRING_DATA_REDIS_PASSWORD=redis123

# Kafka
SPRING_KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Resilience4j Chaos Engineering
ADQUIRENTE_SIMULADO_FAILURE_RATE=0.2
ADQUIRENTE_SIMULADO_LATENCY_MS=100
ADQUIRENTE_SIMULADO_TIMEOUT_RATE=0.1
```

### Iniciar Infraestrutura

```bash
# Subir PostgreSQL, Redis e Kafka
docker-compose up -d

# Verificar status
docker-compose ps

# Ver logs
docker-compose logs -f
```

### Compilar e Executar

```bash
# Compilar
mvn clean package

# Executar
mvn spring-boot:run

# Ou via JAR
java -jar target/toolschallenge-0.0.1-SNAPSHOT.jar
```

### Acessar Serviços

- **API**: http://localhost:8080
- **Swagger**: http://localhost:8080/swagger-ui.html
- **Actuator**: http://localhost:8080/atuador
- **Prometheus Metrics**: http://localhost:8080/atuador/prometheus

---

## 🧪 Testes

### Estrutura de Testes

```
src/test/java/
├── integration/
│   ├── PagamentoIntegrationTest.java
│   ├── EstornoIntegrationTest.java
│   └── IdempotenciaIntegrationTest.java
└── unit/
    ├── PagamentoServiceTest.java
    ├── EstornoServiceTest.java
    └── AdquirenteServiceTest.java
```

### Testcontainers

Testes de integração usam containers Docker:

- PostgreSQL (via Testcontainers)
- Kafka (via Testcontainers)
- Redis (via Testcontainers)

**Exemplo**:

```java
@SpringBootTest
@Testcontainers
class PagamentoIntegrationTest {
    
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");
    
    @Container
    static KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));
    
    @Test
    void deveCriarPagamentoComSucesso() {
        // ...
    }
}
```

### Executar Testes

```bash
# Todos os testes
mvn test

# Apenas testes unitários
mvn test -Dtest=*Test

# Apenas testes de integração
mvn test -Dtest=*IntegrationTest

# Com cobertura
mvn test jacoco:report
```

**Mais informações**: Ver [docs/TESTES_IDEMPOTENCIA.md](docs/TESTES_IDEMPOTENCIA.md) e [docs/TESTES_OUTBOX_PATTERN.md](docs/TESTES_OUTBOX_PATTERN.md)

---

## 🚀 Deploy e CI/CD

### Pipeline GitHub Actions (Proposto)

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [ main, develop ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '17'
      - run: mvn clean verify
      - run: docker build -t toolschallenge:${{ github.sha }} .
```

### Dockerfile

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 📈 Monitoramento

### Stack Proposta

```
Aplicação → Micrometer → Prometheus → Grafana
                            ↓
                         Alertmanager
```

### Dashboards Grafana

**Painéis Principais**:

1. **HTTP Metrics**: Latência, throughput, erros por endpoint
2. **Circuit Breaker**: Estado, taxa de falhas, fallbacks
3. **Database**: Conexões ativas, latência de queries
4. **JVM**: Memory, GC, threads
5. **Kafka**: Offset lag, mensagens/s

### Alertas Propostos

| Alerta | Condição | Severidade |
|--------|----------|------------|
| Circuit Breaker OPEN | Estado = OPEN por > 1min | Critical |
| Alta Taxa de Erro | 5xx > 5% por 5min | High |
| Latência Alta | p95 > 1s por 5min | Medium |
| Database Pool Cheio | Connections = max por 2min | High |

---

## 🐛 Troubleshooting

### Problema: 409 Conflict em requisição nova

**Causa**: Chave idempotente duplicada ou não expirada no Redis.

**Solução**:

```bash
# Limpar chave específica
redis-cli -a redis123 DEL "idempotencia:550e8400-..."

# Limpar todas (CUIDADO!)
redis-cli -a redis123 FLUSHDB
```

### Problema: Circuit Breaker sempre OPEN

**Causa**: Taxa de falhas do adquirente simulado muito alta.

**Solução**: Reduzir `failure-rate` em `application.yml`:

```yaml
adquirente:
  simulado:
    failure-rate: 0.1  # 10% em vez de 20%
```

### Problema: Eventos não chegam no Kafka

**Verificações**:

1. Kafka rodando: `docker-compose ps kafka`
2. Tópico existe: `docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092`
3. Outbox pendente: `SELECT * FROM outbox_evento WHERE status = 'PENDENTE';`
4. Logs do `OutboxPublisher`: Procurar por erros

### Problema: Lock Distribuído não funciona

**Verificações**:

1. Redis rodando: `redis-cli -a redis123 ping`
2. RedissonClient injetado: Verificar logs de startup
3. Lock key correto: `redis-cli -a redis123 KEYS "estorno:*"`

---

## ❓ FAQ

### 1. Por que Monolito Modular em vez de Microserviços?

Microserviços trazem complexidade operacional desde o dia 1. Monolito Modular permite:

- ✅ Começar simples (1 deploy)
- ✅ Evoluir a arquitetura conforme necessidade
- ✅ Migrar módulos específicos quando justificável

### 2. Como saber quando migrar um módulo para microserviço?

**Sinais**:

- Módulo tem carga muito maior que outros
- Time cresceu e precisa de autonomia de deploy
- Tecnologia diferente seria melhor
- Necessidade de deploy em regiões diferentes

### 3. Posso ter transações entre módulos?

**No monolito**: Sim, `@Transactional` funciona entre módulos.

**Em microserviços**: Não, cada serviço tem seu próprio banco. Use **Saga Pattern** ou **Outbox Pattern**.

### 4. Shared/Infra não vai gerar acoplamento?

**Correto**: `shared/` tem apenas utilitários genéricos (configs, exceptions, annotations).

**Errado**: `shared/` NÃO deve ter lógica de negócio, entidades JPA compartilhadas ou services que orquestram módulos.

### 5. Como evitar over-engineering?

- Use recursos nativos do Spring/Java antes de criar código customizado
- Siga o Princípio YAGNI (You Aren't Gonna Need It)
- Use a "Regra dos 3": Só crie abstração após 3º uso repetido
- Prefira Bean Validation padrão a annotations customizadas

Ver seção completa de FAQ em [.github/instructions/copilot-instructions.md](.github/instructions/copilot-instructions.md)

---

## 📚 Referências

### Documentos Internos

- [Regras de Desenvolvimento](.github/instructions/copilot-instructions.md)
- [Sistema de Auditoria](docs/AUDITORIA.md)
- [Locks Distribuídos](docs/LOCK_DISTRIBUIDO.md)
- [Testes de Idempotência](docs/TESTES_IDEMPOTENCIA.md)
- [Testes do Outbox Pattern](docs/TESTES_OUTBOX_PATTERN.md)
- [Exemplos API Pagamento](EXEMPLOS_API_PAGAMENTO.md)
- [Exemplos API Estorno](EXEMPLOS_API_ESTORNO.md)

### Tecnologias

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Resilience4j](https://resilience4j.readme.io/)
- [Redisson](https://github.com/redisson/redisson)
- [Apache Kafka](https://kafka.apache.org/)
- [PostgreSQL](https://www.postgresql.org/)

### Padrões

- [Outbox Pattern](https://microservices.io/patterns/data/transactional-outbox.html)
- [Circuit Breaker](https://martinfowler.com/bliki/CircuitBreaker.html)
- [Idempotency](https://stripe.com/docs/api/idempotent_requests)
- [Modular Monolith](https://www.kamilgrzybek.com/blog/posts/modular-monolith-primer)
- [Saga Pattern](https://microservices.io/patterns/data/saga.html)

### Princípios de Design

- [KISS Principle](https://en.wikipedia.org/wiki/KISS_principle)
- [YAGNI](https://martinfowler.com/bliki/Yagni.html)
- [Occam's Razor](https://fs.blog/occams-razor/)

---

## 📄 Licença

Projeto desenvolvido para desafio técnico Sicredi - Uso Interno.

---

**Última Atualização**: 02/11/2025  
**Versão**: 0.0.1-SNAPSHOT  
