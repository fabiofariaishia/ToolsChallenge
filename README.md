# 📘 ToolsChallenge - API de Pagamentos Sicredi

## 📋 Índice

1. [Visão Geral](#-visão-geral)
2. [Quick Start](#-quick-start)
3. [Stack Tecnológico](#-stack-tecnológico)
4. [Estrutura de Pastas](#-estrutura-de-pastas)
5. [Banco de Dados](#-banco-de-dados)
6. [Mensageria (Kafka)](#-mensageria-kafka)
7. [Cache e Locks Distribuídos](#-cache-e-locks-distribuídos)
8. [Resiliência (Resilience4j)](#-resiliência-resilience4j)
9. [Observabilidade](#-observabilidade)
10. [APIs e Endpoints](#-apis-e-endpoints)
11. [Configuração e Ambiente](#-configuração-e-ambiente)
12. [Testes](#-testes)
13. [Licença](#-licença)

---

## 🎯 Visão Geral

**ToolsChallenge** é uma API REST de processamento de pagamentos desenvolvida para o **Sicredi**, implementando padrões de arquitetura moderna, resiliente e escalável baseada em **Monolito Modular** com preparação para evolução para **Microserviços**.



### Características Principais

- 🔐 **Idempotência**: Chaves idempotentes em todos os endpoints mutáveis
- 🔄 **Outbox Pattern**: Garantia de entrega de eventos via transactional outbox
- 🔒 **Locks Distribuídos**: Prevenção de race conditions com Redisson
- 🛡️ **Resiliência**: Circuit Breaker, Retry e Bulkhead com Resilience4j
- 📊 **Auditoria**: Registro completo de todos os eventos de negócio
- 🚀 **Performance**: Cache Redis e processamento assíncrono via Kafka

---

## 🚀 Quick Start

### Pré-requisitos

- **Java 17+** instalado
- **Docker Desktop** rodando (para Windows)
- **Maven 3.9+** (ou use o wrapper incluído: `mvnw.cmd`)
- **Git** para clonar o repositório

### Passo 1: Clonar o Repositório

```powershell
git clone https://github.com/seu-usuario/ToolsChallenge.git
cd ToolsChallenge
```

### Passo 2: Subir Infraestrutura (Docker)

```powershell
# Subir todos os containers (PostgreSQL, Redis, Kafka, Prometheus, Grafana, Jaeger)
docker-compose up -d

# Verificar status dos containers
docker-compose ps

# Verificar logs (opcional)
docker-compose logs -f
```

**Containers iniciados:**
- **PostgreSQL** (porta 5432)
- **Redis** (porta 6379)
- **Kafka** (porta 9092)
- **Kafka UI** (porta 8081)
- **Prometheus** (porta 9090)
- **Grafana** (porta 3000)
- **Jaeger** (porta 16686)
- **Exporters** (postgres:9187, redis:9121, kafka:9308)

### Passo 3: Compilar a Aplicação

```powershell
# Usando Maven Wrapper (recomendado - não precisa ter Maven instalado)
.\mvnw.cmd clean package

# OU usando Maven instalado
mvn clean package
```

**Saída esperada:**
```
[INFO] Tests run: 125, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

### Passo 4: Executar a Aplicação

```powershell
# Opção 1: Via Maven (com hot reload)
.\mvnw.cmd spring-boot:run

# Opção 2: Via JAR compilado
java -jar target/toolschallenge-0.0.1-SNAPSHOT.jar
```

**Aguarde a mensagem:**
```
Started ToolschallengeApplication in X.XXX seconds
```

### Passo 5: Acessar os Serviços

| Serviço | URL | Credenciais |
|---------|-----|-------------|
| **API Swagger** | http://localhost:8080/swagger-ui.html | - |
| **Actuator** | http://localhost:8080/atuador/health | - |
| **Prometheus** | http://localhost:9090 | - |
| **Grafana** | http://localhost:3000 | `admin` / `admin123` |
| **Jaeger** | http://localhost:16686 | - |
| **Kafka UI** | http://localhost:8081 | - |

### Passo 6: Testar a API

**Criar um Pagamento:**

```powershell
# PowerShell (Windows)
$headers = @{
    "Content-Type" = "application/json"
    "Chave-Idempotencia" = [guid]::NewGuid().ToString()
}
$body = @{
    descricao = "Compra de Teste"
    valor = 150.50
    tipoPagamento = "CARTAO_CREDITO"
} | ConvertTo-Json

Invoke-RestMethod -Uri "http://localhost:8080/pagamentos" -Method POST -Headers $headers -Body $body
```

**Resposta esperada (201 Created):**
```json
{
  "id": 1,
  "descricao": "Compra de Teste",
  "valor": 150.50,
  "tipoPagamento": "CARTAO_CREDITO",
  "status": "AUTORIZADO",
  "nsu": "123456789",
  "codigoAutorizacao": "AUTH987654",
  "dataCriacao": "2025-11-04T10:30:00Z"
}
```

### Passo 7: Visualizar Métricas no Grafana

1. Acessar http://localhost:3000 (`admin` / `admin123`)
2. Navegar para **Dashboards**
3. Abrir dashboards disponíveis:
   - **Business Metrics** - métricas de negócio
   - **HTTP Metrics** - métricas de API
   - **JVM Micrometer** - métricas de JVM
   - **Resilience4j** - circuit breaker, retry, bulkhead

### Troubleshooting Rápido

**Container não inicia:**
```powershell
# Ver logs detalhados
docker-compose logs nome-do-container

# Reiniciar container específico
docker-compose restart nome-do-container
```

**Aplicação não conecta no banco:**
```powershell
# Verificar se PostgreSQL está rodando
docker-compose ps postgres

# Testar conexão
docker exec -it toolschallenge-postgres psql -U postgres -d pagamentos -c "\dt"
```

**Porta já em uso:**
```powershell
# Descobrir processo usando a porta (ex: 8080)
netstat -ano | findstr :8080

# Matar processo (substitua PID)
taskkill /PID <PID> /F
```

**Limpar tudo e recomeçar:**
```powershell
# Parar e remover containers + volumes
docker-compose down -v

# Subir novamente
docker-compose up -d

# Recompilar e executar
.\mvnw.cmd clean package
.\mvnw.cmd spring-boot:run
```



------

## 🛠️ Stack Tecnológico

### Backend

| Tecnologia | Versão | Propósito |
|-----------|--------|-----------|
| **Java** | 17 | Linguagem base |
| **Spring Boot** | 3.5.7 | Framework principal |
| **Spring Data JPA** | (parent) | Persistência ORM |
| **Spring Kafka** | (parent) | Mensageria |
| **Spring Actuator** | (parent) | Monitoramento |
| **Spring Security** | (parent) | Autenticação e autorização |
| **Spring Validation** | (parent) | Validação de beans |

### Segurança

| Tecnologia | Versão | Propósito |
|-----------|--------|-----------|
| **JJWT** | 0.12.6 | Geração e validação de tokens JWT |
| **Spring Security** | 3.5.7 | Framework de segurança |

### Persistência

| Tecnologia | Versão | Propósito |
|-----------|--------|-----------|
| **PostgreSQL** | 15 (driver: runtime) | Banco de dados principal |
| **Flyway** | (parent) | Migrações de schema |
| **Flyway PostgreSQL** | (runtime) | Suporte PostgreSQL para Flyway |
| **Redis** | 7 (via Lettuce) | Cache e idempotência |
| **H2 Database** | (test) | Banco em memória para testes |

### Mensageria

| Tecnologia | Versão | Propósito |
|-----------|--------|-----------|
| **Apache Kafka** | 7.5.0 (Confluent) | Event streaming (via Docker) |
| **Spring Kafka** | (parent) | Integração com Kafka |

### Resiliência

| Tecnologia | Versão | Propósito |
|-----------|--------|-----------|
| **Resilience4j** | 2.2.0 | Circuit Breaker, Retry, Bulkhead |
| **Redisson** | 3.35.0 | Locks distribuídos com Redis |

### Observabilidade

| Tecnologia | Versão | Propósito |
|-----------|--------|-----------|
| **Micrometer** | (parent) | Métricas |
| **Micrometer Prometheus** | (runtime) | Exportação para Prometheus |
| **Micrometer Tracing** | (parent) | Distributed tracing |
| **OpenTelemetry** | (parent) | Exportação OTLP para Jaeger |
| **Prometheus** | 2.x (via Docker) | Coleta de métricas |
| **Grafana** | latest (via Docker) | Visualização de métricas |
| **Jaeger** | latest (via Docker) | Distributed tracing UI |
| **Springdoc OpenAPI** | 2.7.0 | Documentação Swagger/OpenAPI |

### Build e Testes

| Tecnologia | Versão | Propósito |
|-----------|--------|-----------|
| **Maven** | 3.9.x | Build tool |
| **JUnit 5** | (parent) | Testes unitários |
| **Spring Boot Test** | (test) | Testes de integração |
| **Spring Security Test** | (test) | Testes de segurança |
| **Spring Kafka Test** | (test) | Testes com Kafka |
| **Testcontainers** | (test) | Containers Docker para testes |
| **Testcontainers PostgreSQL** | (test) | PostgreSQL em container |
| **Testcontainers Kafka** | (test) | Kafka em container |
| **Testcontainers JUnit** | (test) | Integração JUnit 5 |
| **Lombok** | 1.18.x | Redução de boilerplate |

### Cloud & Infrastructure

| Tecnologia | Versão | Propósito |
|-----------|--------|-----------|
| **Spring Cloud** | 2023.0.3 | Gestão de dependências cloud |
| **Docker Compose** | 3.8 | Orquestração de containers locais |

---

## 📁 Estrutura de Pastas

```text
ToolsChallenge/
│
├── docker/
│   ├── postgres/init.sql              # Scripts iniciais PostgreSQL
│   ├── kafka/                         # Configurações Kafka
│   ├── redis/                         # Configurações Redis
│   ├── grafana/provisioning/          # Dashboards e datasources Grafana
│   └── prometheus/prometheus.yml      # Configuração Prometheus
│
├── docs/
│   ├── AUDITORIA.md                   # Sistema de auditoria
│   ├── LOCK_DISTRIBUIDO.md            # Locks distribuídos
│   ├── TESTES_IDEMPOTENCIA.md         # Testes idempotência
│   ├── TESTES_OUTBOX_PATTERN.md       # Testes Outbox Pattern
│   ├── EXEMPLOS_API_PAGAMENTO.md      # Exemplos de uso da API de pagamentos
│   ├── EXEMPLOS_API_ESTORNO.md        # Exemplos de uso da API de estornos
│   └── QUICKSTART.md                  # Guia rápido de início
│
├── src/
│   ├── main/
│   │   ├── java/br/com/sicredi/toolschallenge/
│   │   │   ├── adquirente/            # 🏦 Módulo Adquirente
│   │   │   │   ├── domain/           # Entidades e enums
│   │   │   │   ├── dto/              # DTOs de request/response
│   │   │   │   ├── events/           # Eventos de domínio
│   │   │   │   └── service/          # Lógica de negócio
│   │   │   │
│   │   │   ├── pagamento/             # 💳 Módulo Pagamento
│   │   │   │   ├── controller/       # Endpoints REST
│   │   │   │   ├── domain/           # Entidades e enums
│   │   │   │   ├── dto/              # DTOs de request/response
│   │   │   │   ├── events/           # Eventos de domínio
│   │   │   │   ├── repository/       # Persistência JPA
│   │   │   │   └── service/          # Lógica de negócio
│   │   │   │
│   │   │   ├── estorno/               # 🔄 Módulo Estorno
│   │   │   │   ├── controller/       # Endpoints REST
│   │   │   │   ├── domain/           # Entidades e enums
│   │   │   │   ├── dto/              # DTOs de request/response
│   │   │   │   ├── events/           # Eventos de domínio
│   │   │   │   ├── repository/       # Persistência JPA
│   │   │   │   └── service/          # Lógica de negócio
│   │   │   │
│   │   │   ├── admin/                 # 🔑 Módulo Admin (geração de tokens JWT)
│   │   │   │   ├── controller/       # Endpoints administrativos
│   │   │   │   └── dto/              # DTOs de response
│   │   │   │
│   │   │   ├── infra/                 # 🏗️ Infraestrutura (cross-cutting)
│   │   │   │   ├── auditoria/        # Sistema de auditoria de eventos
│   │   │   │   ├── idempotencia/     # Mecanismo de idempotência
│   │   │   │   ├── outbox/           # Outbox Pattern (Kafka)
│   │   │   │   ├── scheduled/        # Jobs agendados (reprocessamento)
│   │   │   │   └── tracing/          # Correlation ID e tracing
│   │   │   │
│   │   │   ├── shared/                # 🔧 Compartilhado (utilitários genéricos)
│   │   │   │   ├── config/           # Configurações globais (Kafka, Redis, Redisson)
│   │   │   │   ├── exception/        # Exceções globais e @ControllerAdvice
│   │   │   │   └── security/         # JWT Service, Filters, SecurityConfig
│   │   │   │
│   │   │   └── ToolschallengeApplication.java  # Main class
│   │   │
│   │   └── resources/
│   │       ├── application.yml        # Configuração principal
│   │       ├── application-test.yml   # Configuração de testes
│   │       ├── logback-spring.xml     # Configuração de logs
│   │       └── db/migration/          # Flyway migrations (V1__, V2__, ...)
│   │
│   └── test/
│       └── java/br/com/sicredi/toolschallenge/
│           ├── adquirente/service/   # Testes unitários Adquirente
│           ├── pagamento/
│           │   ├── controller/       # Testes unitários Controller
│           │   └── service/          # Testes unitários Service
│           ├── estorno/
│           │   ├── controller/       # Testes unitários Controller
│           │   └── service/          # Testes unitários Service
│           ├── infra/
│           │   ├── auditoria/        # Testes de auditoria
│           │   ├── idempotencia/     # Testes de idempotência
│           │   ├── outbox/           # Testes do Outbox Pattern
│           │   ├── scheduled/        # Testes de reprocessamento
│           │   └── tracing/          # Testes de Correlation ID
│           └── shared/security/      # Testes de JWT
│
├── docker-compose.yml                 # Infraestrutura (PostgreSQL, Redis, Kafka, etc)
├── pom.xml                            # Dependências Maven
├── mvnw.cmd / mvnw                    # Maven Wrapper
├── docker.ps1                         # Script Docker (PowerShell)
├── Makefile                           # Comandos úteis
└── README.md                          # Este arquivo
```

---

## 🗄️ Banco de Dados

### Schemas PostgreSQL

O projeto utiliza **3 schemas separados** seguindo o padrão **DDD (Domain-Driven Design)**:

| Schema | Descrição | Tabelas |
|--------|-----------|---------|
| `pagamento` | Bounded Context de Pagamento | `pagamento` |
| `estorno` | Bounded Context de Estorno | `estorno` |
| `infra` | Infraestrutura compartilhada | `outbox`, `idempotencia`, `evento_auditoria` |

**Extensões habilitadas**:
- `uuid-ossp` - Geração de UUIDs
- `pg_trgm` - Busca textual (trigram)

**Timezone**: `America/Sao_Paulo` (UTC-3)

---

### Tabelas

#### 1. `pagamento.pagamento`

**Descrição**: Transações de pagamento com cartão de crédito.

**DDL**:
```sql
CREATE TABLE pagamento.pagamento (
    -- Chaves
    id BIGSERIAL PRIMARY KEY,
    id_transacao VARCHAR(50) NOT NULL UNIQUE,  -- Chave de negócio
    
    -- Status e Financeiro
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE' 
        CHECK (status IN ('PENDENTE', 'AUTORIZADO', 'NEGADO')),
    valor DECIMAL(15,2) NOT NULL CHECK (valor > 0),
    moeda VARCHAR(3) NOT NULL DEFAULT 'BRL',
    
    -- Data/hora
    data_hora TIMESTAMP WITH TIME ZONE NOT NULL,
    
    -- Estabelecimento
    estabelecimento VARCHAR(255) NOT NULL,
    
    -- Tipo de Pagamento
    tipo_pagamento VARCHAR(20) NOT NULL 
        CHECK (tipo_pagamento IN ('AVISTA', 'PARCELADO_LOJA', 'PARCELADO_EMISSOR')),
    parcelas INTEGER NOT NULL CHECK (parcelas >= 1 AND parcelas <= 12),
    
    -- Dados do Adquirente
    nsu VARCHAR(10) UNIQUE,                    -- NSU gerado via Snowflake
    codigo_autorizacao VARCHAR(9) UNIQUE,      -- Código com Luhn check
    
    -- Cartão (SEMPRE mascarado)
    cartao_mascarado VARCHAR(20) NOT NULL,     -- Formato: 4444********1234
    
    -- Snowflake ID (geração de NSU time-sortable)
    snowflake_id BIGINT UNIQUE,
    
    -- Reprocessamento (DLQ)
    tentativas_reprocessamento INTEGER NOT NULL DEFAULT 0,
    
    -- Auditoria
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**Índices**:
```sql
CREATE INDEX idx_pagamento_id_transacao ON pagamento.pagamento(id_transacao);
CREATE INDEX idx_pagamento_status ON pagamento.pagamento(status);
CREATE INDEX idx_pagamento_data_hora ON pagamento.pagamento(data_hora DESC);
CREATE INDEX idx_pagamento_estabelecimento ON pagamento.pagamento(estabelecimento);
CREATE INDEX idx_pagamento_nsu ON pagamento.pagamento(nsu) WHERE nsu IS NOT NULL;
CREATE INDEX idx_pagamento_filtros ON pagamento.pagamento(status, estabelecimento, data_hora DESC);
CREATE INDEX idx_pagamento_reprocessamento ON pagamento.pagamento(status, tentativas_reprocessamento, criado_em) 
    WHERE status = 'PENDENTE';
```

**Constraints**:
- `chk_valor_positivo`: Valor > 0
- `chk_parcelas_validas`: À vista = 1 parcela, Parcelado >= 2 parcelas
- `chk_moeda_iso4217`: Moeda no formato ISO 4217 (ex: BRL)
- `chk_cartao_mascarado`: Formato `^\d{4}\*+\d{4}$`

**Trigger**: `trg_pagamento_atualizar_timestamp` - Atualiza `atualizado_em` automaticamente.

---

#### 2. `estorno.estorno`

**Descrição**: Estornos de pagamentos autorizados (janela 24h, valor total).

**DDL**:
```sql
CREATE TABLE estorno.estorno (
    -- Chaves
    id BIGSERIAL PRIMARY KEY,
    id_transacao VARCHAR(50) NOT NULL,         -- Referência ao pagamento
    id_estorno VARCHAR(50) NOT NULL UNIQUE,    -- Chave única do estorno
    
    -- Status e Financeiro
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE' 
        CHECK (status IN ('PENDENTE', 'CANCELADO', 'NEGADO')),
    valor DECIMAL(15,2) NOT NULL CHECK (valor > 0),
    
    -- Data/hora
    data_hora TIMESTAMP WITH TIME ZONE NOT NULL,
    
    -- Dados do Adquirente
    nsu VARCHAR(10) UNIQUE,
    codigo_autorizacao VARCHAR(9) UNIQUE,
    
    -- Motivo (opcional)
    motivo TEXT,
    
    -- Snowflake ID
    snowflake_id BIGINT UNIQUE,
    
    -- Reprocessamento (DLQ)
    tentativas_reprocessamento INTEGER NOT NULL DEFAULT 0,
    
    -- Auditoria
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Foreign Key
    CONSTRAINT fk_estorno_pagamento 
        FOREIGN KEY (id_transacao) 
        REFERENCES pagamento.pagamento(id_transacao)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
);
```

**Índices**:
```sql
CREATE INDEX idx_estorno_id_transacao ON estorno.estorno(id_transacao);
CREATE INDEX idx_estorno_id_estorno ON estorno.estorno(id_estorno);
CREATE INDEX idx_estorno_status ON estorno.estorno(status);
CREATE INDEX idx_estorno_data_hora ON estorno.estorno(data_hora DESC);
CREATE INDEX idx_estorno_por_pagamento ON estorno.estorno(id_transacao, status);
CREATE INDEX idx_estorno_reprocessamento ON estorno.estorno(status, tentativas_reprocessamento, criado_em) 
    WHERE status = 'PENDENTE';

-- Constraint única: apenas 1 estorno CANCELADO por pagamento
CREATE UNIQUE INDEX idx_estorno_unico_cancelado 
    ON estorno.estorno(id_transacao) 
    WHERE status = 'CANCELADO';
```

**Constraints**:
- `chk_estorno_valor_positivo`: Valor > 0
- `fk_estorno_pagamento`: Referência obrigatória ao pagamento original

**Trigger**: `trg_estorno_atualizar_timestamp` - Atualiza `atualizado_em` automaticamente.

---

#### 3. `infra.outbox`

**Descrição**: Transactional Outbox Pattern - Eventos pendentes para publicação no Kafka.

**DDL**:
```sql
CREATE TABLE infra.outbox (
    id BIGSERIAL PRIMARY KEY,
    
    -- Agregado
    agregado_id VARCHAR(50) NOT NULL,
    agregado_tipo VARCHAR(50) NOT NULL,        -- Ex: Pagamento, Estorno
    
    -- Evento
    evento_tipo VARCHAR(100) NOT NULL,         -- Ex: PagamentoAutorizado
    payload JSONB NOT NULL,
    topico_kafka VARCHAR(100) NOT NULL,
    
    -- Status
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE' 
        CHECK (status IN ('PENDENTE', 'PROCESSADO', 'ERRO')),
    tentativas INTEGER NOT NULL DEFAULT 0,
    ultimo_erro TEXT,
    
    -- Timestamps
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processado_em TIMESTAMP WITH TIME ZONE
);
```

**Índices**:
```sql
CREATE INDEX idx_outbox_status_pendente ON infra.outbox(status, criado_em) 
    WHERE status = 'PENDENTE';
CREATE INDEX idx_outbox_agregado ON infra.outbox(agregado_tipo, agregado_id);
```

**Função de Limpeza**:
```sql
-- Remove eventos processados há mais de 7 dias
CREATE FUNCTION infra.limpar_outbox_processados() RETURNS INTEGER;
```

---

#### 4. `infra.idempotencia`

**Descrição**: Fallback de idempotência (quando Redis indisponível). TTL 24h.

**DDL**:
```sql
CREATE TABLE infra.idempotencia (
    chave VARCHAR(100) PRIMARY KEY,            -- Header: Chave-Idempotencia
    
    -- Transação
    id_transacao VARCHAR(50) NOT NULL,
    endpoint VARCHAR(100) NOT NULL,            -- Ex: POST /pagamentos
    
    -- Response
    status_http INTEGER NOT NULL,
    response_body JSONB,
    
    -- TTL
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expira_em TIMESTAMP WITH TIME ZONE NOT NULL,
    
    CONSTRAINT chk_idempotencia_expiracao CHECK (expira_em > criado_em)
);
```

**Índices**:
```sql
CREATE INDEX idx_idempotencia_expiracao ON infra.idempotencia(expira_em);
CREATE INDEX idx_idempotencia_id_transacao ON infra.idempotencia(id_transacao);
```

**Função de Limpeza**:
```sql
-- Remove registros expirados (executar periodicamente)
CREATE FUNCTION infra.limpar_idempotencia_expirada() RETURNS INTEGER;
```

---

#### 5. `infra.evento_auditoria`

**Descrição**: Log de auditoria de todos os eventos do sistema (compliance).

**DDL**:
```sql
CREATE TABLE infra.evento_auditoria (
    id BIGSERIAL PRIMARY KEY,
    
    -- Evento
    evento_tipo VARCHAR(100) NOT NULL,
    
    -- Agregado (opcional)
    agregado_tipo VARCHAR(50),
    agregado_id VARCHAR(50),
    
    -- Usuário/Sistema
    usuario VARCHAR(100),
    
    -- Dados
    dados JSONB,
    metadados JSONB,                           -- Ex: IP, User-Agent
    
    -- Timestamp
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

**Índices**:
```sql
CREATE INDEX idx_evento_auditoria_tipo ON infra.evento_auditoria(evento_tipo);
CREATE INDEX idx_evento_auditoria_agregado ON infra.evento_auditoria(agregado_tipo, agregado_id);
CREATE INDEX idx_evento_auditoria_criado_em ON infra.evento_auditoria(criado_em DESC);
CREATE INDEX idx_evento_auditoria_usuario ON infra.evento_auditoria(usuario);
```

---

### Flyway Migrations

Migrações localizadas em `src/main/resources/db/migration/`:

| Migration | Descrição | Artefatos Criados |
|-----------|-----------|-------------------|
| **V1** | Schemas e extensões | Schemas: `pagamento`, `estorno`, `infra`<br>Extensions: `uuid-ossp`, `pg_trgm` |
| **V2** | Tabela de pagamentos | `pagamento.pagamento` + 6 índices + trigger |
| **V3** | Tabela de estornos | `estorno.estorno` + 6 índices + trigger + constraint única |
| **V4** | Tabelas de infraestrutura | `infra.outbox`, `infra.idempotencia`, `infra.evento_auditoria` + funções de limpeza |
| **V5** | Dados de exemplo | INSERT de pagamentos e estornos para testes |
| **V6** | Campo reprocessamento | Coluna `tentativas_reprocessamento` + índices para DLQ |

**Execução**: Automática no startup via `spring.flyway.enabled=true`

**Validação**:
```sql
-- Verificar versão das migrations
SELECT version, description, installed_on 
FROM flyway_schema_history 
ORDER BY installed_rank;
```

---

## 📨 Mensageria (Kafka)

### Tópicos Kafka

| Tópico | Eventos | Publicador | Consumidores |
|--------|---------|------------|--------------|
| `pagamentos` | `PagamentoCriadoEvento`, `PagamentoStatusAlteradoEvento` | `OutboxPublisher` via Outbox Pattern | Auditoria (futuros consumidores) |
| `estornos` | `EstornoCriadoEvento`, `EstornoStatusAlteradoEvento` | `OutboxPublisher` via Outbox Pattern | Auditoria (futuros consumidores) |
| `adquirente` | `AutorizacaoRealizadaEvento` | `OutboxPublisher` via Outbox Pattern | Auditoria (futuros consumidores) |

**Nota**: O projeto usa **Outbox Pattern** - eventos são salvos na tabela `infra.outbox` de forma transacional, e um scheduler (`OutboxPublisher`) processa e publica no Kafka a cada 500ms.

### Estrutura de Evento

**PagamentoCriadoEvento**:

```json
{
  "idPagamento": 123,
  "idTransacao": "PAG-20251104-550e8400",
  "descricao": "Compra na Loja X",
  "valor": 150.50,
  "metodoPagamento": "CARTAO_CREDITO",
  "formaPagamento": "AVISTA",
  "status": "AUTORIZADO",
  "criadoEm": "2025-11-04T10:30:00-03:00"
}
```

**EstornoCriadoEvento**:

```json
{
  "idEstorno": 456,
  "idTransacao": "PAG-20251104-550e8400",
  "idEstornoUnico": "EST-20251104-660f9511",
  "valor": 150.50,
  "motivo": "Cliente solicitou cancelamento",
  "status": "CANCELADO",
  "criadoEm": "2025-11-04T11:00:00-03:00"
}
```

### Configuração Kafka

**Producer** (KafkaConfig.java):

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      # Configurações de confiabilidade (KafkaConfig)
      acks: all                              # Aguarda confirmação de todos os replicas
      retries: 3                             # Retry automático em caso de erro
      enable.idempotence: true               # Previne duplicatas no Kafka
      max.in.flight.requests.per.connection: 1  # Garante ordem das mensagens
      # Performance
      batch.size: 16384                      # Batch de 16KB
      linger.ms: 10                          # Aguarda 10ms antes de enviar
      buffer.memory: 33554432                # Buffer de 32MB
      compression.type: snappy               # Compressão Snappy
      # Timeouts
      request.timeout.ms: 30000              # 30 segundos
      delivery.timeout.ms: 120000            # 2 minutos
```

**Consumer** (aplicação futura - não implementado ainda):

```yaml
spring:
  kafka:
    consumer:
      group-id: pagamentos-group
      auto-offset-reset: earliest            # Processa desde início
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: br.com.sicredi.toolschallenge
```

**Outbox Pattern - Publicação Transacional**:

1. **Salvar evento**: Chamada a `OutboxService.criarEvento()` dentro da mesma transação da mudança de estado
2. **Scheduler**: `OutboxPublisher` roda a cada **500ms** buscando eventos `PENDENTE`
3. **Publicação**: Eventos são enviados ao Kafka via `KafkaTemplate`
4. **Confirmação**: Após sucesso, evento é marcado como `PROCESSADO`
5. **Retry**: Em caso de erro, incrementa `tentativas` e tenta novamente (max 3 tentativas)
6. **Limpeza**: Eventos `PROCESSADO` são removidos após **7 dias** (função `limpar_outbox_processados()`)

**Tópicos definidos dinamicamente** no código ao chamar `OutboxService.criarEvento(agregadoId, agregadoTipo, eventoTipo, payload, topicoKafka)`. Exemplos:
- `"pagamentos"` - eventos de pagamento
- `"estornos"` - eventos de estorno
- `"adquirente"` - eventos de autorização

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
          - br.com.sicredi.toolschallenge.shared.exception.ServicoIndisponivelException
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

**URL**: http://localhost:8080/swagger-ui.html

Documentação interativa **OpenAPI 3.0** de todas as APIs com:

- ✅ **Schemas completos** de request/response
- ✅ **Validações** de campos (`@NotBlank`, `@Size`, `@DecimalMin`)
- ✅ **Códigos de erro** documentados
- ✅ **Exemplos prontos** para testar
- ✅ **Try it out** - Execute requests direto do navegador
- ✅ **Autenticação JWT** integrada (clique em "Authorize")

**Como usar**:
1. Acesse http://localhost:8080/swagger-ui.html
2. Clique em **"Authorize"** (cadeado 🔒)
3. Gere um token em `POST /admin/tokens/{appName}`
4. Cole o token no formato: `Bearer <seu-token>`
5. Teste qualquer endpoint clicando em **"Try it out"**

---

## 🌐 APIs e Endpoints

> **💡 Dica**: Use o [Swagger UI](http://localhost:8080/swagger-ui.html) para testar todas as APIs interativamente!

### Autenticação

Todos os endpoints (exceto `/admin/tokens/*`) requerem **autenticação JWT** via header `Authorization: Bearer <token>`.

**Gerar Token**:
```powershell
# PowerShell
$response = Invoke-RestMethod -Uri "http://localhost:8080/admin/tokens/admin" -Method POST
$token = $response.token
Write-Host "Token gerado: $token"
```

### Pagamentos

#### `POST /pagamentos`

Cria novo pagamento (idempotente).

**Autenticação**: Requer scope `pagamentos:write`

**Headers**:
- `Authorization: Bearer <token>` (obrigatório)
- `Chave-Idempotencia: <UUID>` (obrigatório)
- `Content-Type: application/json`

**Request**:

```json
{
  "descricao": "Compra na Loja X",
  "valor": 150.50,
  "tipoPagamento": "CARTAO_CREDITO"
}
```

**Validações**:
- `descricao`: obrigatório, entre 3 e 500 caracteres
- `valor`: obrigatório, maior que 0
- `tipoPagamento`: obrigatório, valores aceitos: `CARTAO_CREDITO`, `CARTAO_DEBITO`, `PIX`

**Response 201 Created**:

```json
{
  "id": 123,
  "idTransacao": "TXN-123-2025",
  "descricao": "Compra na Loja X",
  "valor": 150.50,
  "tipoPagamento": "CARTAO_CREDITO",
  "status": "AUTORIZADO",
  "nsu": "1234567890",
  "codigoAutorizacao": "AUTH987654",
  "dataCriacao": "2025-11-05T10:30:00-03:00"
}
```

**Possíveis Status**:
- `AUTORIZADO` - Pagamento aprovado pelo adquirente
- `NEGADO` - Pagamento recusado pelo adquirente
- `PENDENTE` - Aguardando processamento (será reprocessado em background)

---

#### `GET /pagamentos/{id}`

Consulta pagamento por ID.

**Autenticação**: Requer scope `pagamentos:read`

**Path Parameters**:
- `id`: ID do pagamento (Long)

**Response 200 OK**:
```json
{
  "id": 123,
  "idTransacao": "TXN-123-2025",
  "descricao": "Compra na Loja X",
  "valor": 150.50,
  "status": "AUTORIZADO",
  "nsu": "1234567890",
  "codigoAutorizacao": "AUTH987654",
  "dataCriacao": "2025-11-05T10:30:00-03:00"
}
```

**Response 404 Not Found**: Pagamento não encontrado

---

#### `GET /pagamentos`

Lista todos os pagamentos (paginado).

**Autenticação**: Requer scope `pagamentos:read`

**Query Parameters** (opcionais):
- `page`: Número da página (padrão: 0)
- `size`: Tamanho da página (padrão: 20)
- `sort`: Ordenação (ex: `dataCriacao,desc`)

**Response 200 OK**:
```json
{
  "content": [
    {
      "id": 123,
      "idTransacao": "TXN-123-2025",
      "valor": 150.50,
      "status": "AUTORIZADO",
      "dataCriacao": "2025-11-05T10:30:00-03:00"
    }
  ],
  "totalElements": 50,
  "totalPages": 3,
  "number": 0,
  "size": 20
}
```

---

### Estornos

#### `POST /estornos`

Solicita estorno de pagamento (idempotente).

**Autenticação**: Requer scope `estornos:write`

**Headers**:
- `Authorization: Bearer <token>` (obrigatório)
- `Chave-Idempotencia: <UUID>` (obrigatório)
- `Content-Type: application/json`

**Request**:

```json
{
  "idTransacao": "TXN-123-2025",
  "motivo": "Cliente solicitou cancelamento"
}
```

**Validações**:
- `idTransacao`: obrigatório, deve existir e estar AUTORIZADO
- `motivo`: opcional, máximo 500 caracteres
- **Janela**: Pagamento deve ter < 24h (regra de negócio)
- **Valor**: Estorno sempre é do valor total do pagamento

**Response 201 Created**:

```json
{
  "id": 456,
  "idEstorno": "EST-456-2025",
  "idTransacao": "TXN-123-2025",
  "valor": 150.50,
  "motivo": "Cliente solicitou cancelamento",
  "status": "CANCELADO",
  "nsu": "9876543210",
  "codigoAutorizacao": "REV123456",
  "dataCriacao": "2025-11-05T11:00:00-03:00"
}
```

**Possíveis Status**:
- `CANCELADO` - Estorno aprovado pelo adquirente
- `NEGADO` - Estorno recusado (ex: fora da janela de 24h)
- `PENDENTE` - Aguardando processamento

**Response 422 Unprocessable Entity**:
```json
{
  "timestamp": "2025-11-05T11:00:00-03:00",
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Pagamento fora da janela de estorno (24h)"
}
```

---

#### `GET /estornos/{id}`

Consulta estorno específico.

**Autenticação**: Requer scope `estornos:read`

**Path Parameters**:
- `id`: ID do estorno (Long)

**Response 200 OK**:
```json
{
  "id": 456,
  "idEstorno": "EST-456-2025",
  "idTransacao": "TXN-123-2025",
  "valor": 150.50,
  "status": "CANCELADO",
  "dataCriacao": "2025-11-05T11:00:00-03:00"
}
```

---

#### `GET /estornos`

Lista todos os estornos (paginado).

**Autenticação**: Requer scope `estornos:read`

**Query Parameters** (opcionais):
- `page`: Número da página (padrão: 0)
- `size`: Tamanho da página (padrão: 20)
- `sort`: Ordenação (ex: `dataCriacao,desc`)

---

#### `GET /estornos/pagamento/{idTransacao}`

Lista estornos de um pagamento específico.

**Autenticação**: Requer scope `estornos:read`

**Path Parameters**:
- `idTransacao`: ID de transação do pagamento (String)

---

#### `GET /estornos/status/{status}`

Lista estornos por status.

**Autenticação**: Requer scope `estornos:read`

**Path Parameters**:
- `status`: Status do estorno (`CANCELADO`, `NEGADO`, `PENDENTE`)

---

### Admin (Tokens JWT)

#### `POST /admin/tokens/{appName}`

Gera token JWT para aplicação específica (endpoint público - sem autenticação).

**Path Parameters**:
- `appName`: Nome da aplicação (`frontend`, `mobile` ou `admin`)

**Apps Disponíveis**:
- `frontend`: scopes = `pagamentos:read`, `pagamentos:write`
- `mobile`: scopes = `pagamentos:read`
- `admin`: scopes = `pagamentos:read`, `pagamentos:write`, `estornos:read`, `estornos:write`

**Response 200**:

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "appName": "frontend",
  "scopes": ["pagamentos:read", "pagamentos:write"],
  "expiresAt": "2025-12-04T10:30:00",
  "expirationSeconds": 2592000
}
```

**Exemplo de Uso** (PowerShell):

```powershell
# Gerar token para frontend
$response = Invoke-RestMethod -Uri "http://localhost:8080/admin/tokens/frontend" -Method POST
$token = $response.token

# Usar token em requisições
$headers = @{
    "Authorization" = "Bearer $token"
    "Chave-Idempotencia" = [guid]::NewGuid().ToString()
    "Content-Type" = "application/json"
}
Invoke-RestMethod -Uri "http://localhost:8080/pagamentos" -Method POST -Headers $headers -Body $jsonBody
```

#### `GET /admin/tokens/apps`

Lista apps disponíveis e seus scopes.

**Response 200**:

```json
{
  "frontend": ["pagamentos:read", "pagamentos:write"],
  "mobile": ["pagamentos:read"],
  "admin": ["pagamentos:read", "pagamentos:write", "estornos:read", "estornos:write"]
}
```

### Códigos de Erro

| Código | Descrição | Quando Ocorre |
|--------|-----------|---------------|
| `400 Bad Request` | Validação falhou | Campos obrigatórios faltando, formato inválido |
| `401 Unauthorized` | Não autenticado | Token JWT ausente ou inválido |
| `403 Forbidden` | Sem permissão | Token válido mas sem scopes necessários |
| `404 Not Found` | Recurso não encontrado | ID de pagamento/estorno não existe |
| `409 Conflict` | Chave idempotente duplicada | Mesmo `Chave-Idempotencia` já processado |
| `422 Unprocessable Entity` | Regra de negócio violada | Estorno fora da janela de 24h, pagamento já estornado |
| `500 Internal Server Error` | Erro inesperado | Erro não tratado na aplicação |
| `503 Service Unavailable` | Circuit Breaker OPEN | Adquirente indisponível (muitas falhas consecutivas) |

**Exemplo de Response de Erro**:

```json
{
  "timestamp": "2025-11-05T11:00:00-03:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Validação falhou",
  "errors": [
    {
      "field": "descricao",
      "message": "não deve estar em branco"
    },
    {
      "field": "valor",
      "message": "deve ser maior que 0"
    }
  ],
  "path": "/pagamentos"
}
```

---

### Documentação Adicional

- 📄 **Swagger UI Interativo**: http://localhost:8080/swagger-ui.html
- 📄 **Exemplos de Pagamentos**: [docs/EXEMPLOS_API_PAGAMENTO.md](docs/EXEMPLOS_API_PAGAMENTO.md)
- 📄 **Exemplos de Estornos**: [docs/EXEMPLOS_API_ESTORNO.md](docs/EXEMPLOS_API_ESTORNO.md)
- 📄 **Testes de Idempotência**: [docs/TESTES_IDEMPOTENCIA.md](docs/TESTES_IDEMPOTENCIA.md)
- 📄 **Outbox Pattern**: [docs/TESTES_OUTBOX_PATTERN.md](docs/TESTES_OUTBOX_PATTERN.md)

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
src/test/java/br/com/sicredi/toolschallenge/
├── adquirente/service/          # Testes unitários Adquirente
├── pagamento/
│   ├── controller/              # Testes unitários Controller
│   └── service/                 # Testes unitários Service
├── estorno/
│   ├── controller/              # Testes unitários Controller
│   └── service/                 # Testes unitários Service
├── infra/
│   ├── auditoria/               # Testes de auditoria
│   ├── idempotencia/            # Testes de idempotência
│   ├── outbox/                  # Testes do Outbox Pattern
│   ├── scheduled/               # Testes de reprocessamento
│   └── tracing/                 # Testes de Correlation ID
└── shared/security/             # Testes de JWT
```

### Testes Unitários (@WebMvcTest)

O projeto utiliza **testes unitários** (slice tests) focados na camada de controller, com mocks de dependências.

**Características**:
- ✅ **Rápidos** (< 1 segundo cada)
- ✅ **Isolados** (todos os dependencies mockados)
- ✅ **Focados** (testam 1 comportamento por vez)
- ✅ **Executados a cada build**

**Configuração**:

```java
@WebMvcTest(controllers = EstornoController.class)
@AutoConfigureMockMvc(addFilters = false)  // Desabilita filtros HTTP (JWT, CSRF)
@Import(GlobalExceptionHandler.class)      // Carrega exception handler
@WithMockUser(authorities = {"estornos:read", "estornos:write"})  // Simula usuário autenticado
class EstornoControllerTest {
    
    @MockBean private EstornoService estornoService;
    @MockBean private IdempotenciaService idempotenciaService;
    
    // Mocks de Security necessários (são @Component escaneados pelo Spring)
    @MockBean private JwtService jwtService;
    @MockBean private JwtAuthenticationFilter jwtAuthenticationFilter;
    
    @Test
    void deveCriarEstornoComSucesso() throws Exception {
        // Arrange
        EstornoRequestDTO request = ...;
        when(estornoService.criar(any())).thenReturn(response);
        
        // Act & Assert
        mockMvc.perform(post("/estornos")
                .header("Chave-Idempotencia", UUID.randomUUID().toString())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").exists());
    }
}
```

**Por que mockar `JwtService` e `JwtAuthenticationFilter`?**

- São classes anotadas com `@Component`, portanto Spring sempre tenta instanciá-las
- `@AutoConfigureMockMvc(addFilters = false)` apenas desabilita **execução** dos filtros no MockMvc
- **NÃO** impede o Spring de escanear e criar os beans durante inicialização do contexto
- Sem `@MockBean`, ApplicationContext falha com `NoSuchBeanDefinitionException`

**O que é testado**:
- ✅ Status HTTP corretos (201, 400, 404, etc)
- ✅ Serialização JSON de request/response
- ✅ Bean Validation (`@NotBlank`, `@Size`, `@DecimalMin`, etc)
- ✅ Tratamento de exceções via `@ControllerAdvice`
- ✅ Lógica de negócio nos Services (com mocks de repositories)

### Cobertura de Testes

O projeto possui **13 classes de teste** cobrindo:

| Módulo | Classes Testadas | Cenários |
|--------|------------------|----------|
| **Pagamento** | PagamentoController, PagamentoService | Criação, consulta, validações, DLQ |
| **Estorno** | EstornoController, EstornoService | Criação, consulta, validações, DLQ, lock distribuído |
| **Adquirente** | AdquirenteService, AdquirenteSimuladoService | Autorização, Circuit Breaker, Retry, Chaos |
| **Infraestrutura** | OutboxService, KafkaPublisherService, AuditoriaService | Outbox Pattern, Kafka, Auditoria |
| **Scheduled** | ReprocessamentoScheduler | DLQ reprocessing |
| **Tracing** | CorrelationIdFilter | Correlation ID propagation |
| **Idempotência** | IdempotenciaService | Cache Redis, fallback PostgreSQL |
| **Security** | JwtService | Geração e validação de tokens JWT |

**Total**: ~125+ testes unitários

### Executar Testes

```bash
# Todos os testes unitários
mvn test

# Testes de um módulo específico
mvn test -Dtest=PagamentoServiceTest

# Testes com output detalhado
mvn test -X

# Com cobertura (JaCoCo)
mvn test jacoco:report
# Relatório em: target/site/jacoco/index.html
```

### Exemplo de Saída

```
[INFO] Tests run: 125, Failures: 0, Errors: 0, Skipped: 0
[INFO] BUILD SUCCESS
```

---

## 📄 Licença

Projeto desenvolvido para desafio técnico Sicredi - Uso Interno.

---

**Última Atualização**: 04/11/2025  
**Versão**: 0.0.1-SNAPSHOT  
