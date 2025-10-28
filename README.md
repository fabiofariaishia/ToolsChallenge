# API de Pagamentos - ToolsChallenge

## 📋 Visão Geral

API RESTful de processamento de pagamentos e estornos com cartão de crédito, construída com **Spring Boot** e **arquitetura event-driven**. O sistema implementa padrões de resiliência, observabilidade e segurança de nível enterprise, com foco em idempotência, rastreabilidade e alta disponibilidade.

### 🎯 Abordagem Arquitetural

**Monolito Modular Event-Driven** (arquitetura híbrida evolutiva):
- ✅ **Monolito** para produtividade e simplicidade de deploy (1 JAR/WAR)
- ✅ **Modular** com bounded contexts bem definidos (preparado para extração)
- ✅ **Event-Driven** via Kafka para desacoplamento e resiliência
- ✅ **Evolutivo** para microsserviços quando necessário (strangler fig pattern)

**Todos os endpoints, campos e mensagens estão em português-BR** conforme requisitos de negócio.

---

## 🎯 Funcionalidades Principais

### Operações de Pagamento
- **`POST /pagamentos`** — Criar novo pagamento (AVISTA, PARCELADO LOJA, PARCELADO EMISSOR)
- **`GET /pagamentos`** — Consultar pagamentos com paginação e filtros
- **`GET /pagamentos/{idTransacao}`** — Consultar pagamento específico por ID

### Operações de Estorno
- **`POST /estornos`** — Solicitar estorno de pagamento autorizado (janela 24h)
- **`GET /estornos/{idTransacao}`** — Consultar estorno por ID da transação

### Características Técnicas
✅ **Idempotência** garantida via header `Chave-Idempotencia` (Redis TTL 24h + fallback BD)  
✅ **Processamento assíncrono** via Kafka (padrão outbox para garantia de entrega)  
✅ **Resiliência** com Resilience4j (circuit breaker, retry, bulkhead)  
✅ **Lock distribuído** (Redisson) para prevenir estornos concorrentes  
✅ **Observabilidade** completa (Prometheus + Grafana + Jaeger)  
✅ **Segurança JWT** com escopos granulares (Keycloak)  
✅ **Mascaramento PCI-DSS** de dados sensíveis (PAN de cartão)  

---

## 🏗️ Arquitetura

### Stack Tecnológica (Bloqueante)

| Camada | Tecnologias |
|--------|-------------|
| **Framework** | Spring Boot 3.x (Spring MVC) |
| **Persistência** | JPA/Hibernate + JDBC (HikariCP) |
| **Banco de Dados** | PostgreSQL 15+ |
| **Migrações** | Flyway |
| **Mensageria** | Apache Kafka 3.x |
| **Cache/Locks** | Redis 7.x (Lettuce + Redisson) |
| **Resiliência** | Resilience4j |
| **Observabilidade** | Actuator + Micrometer → Prometheus + Grafana |
| **Tracing** | OpenTelemetry + Jaeger |
| **Segurança** | Spring Security (JWT) + Keycloak |
| **Testes** | JUnit 5 + Mockito + AssertJ + Testcontainers |
| **Build** | Maven 3.9+ |
| **Containerização** | Docker + Docker Compose |

### Componentes do Sistema

```
┌──────────────────────────────────────────────────────────────────┐
│         API de Pagamentos (Monolito Modular - Spring Boot)      │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ BOUNDED CONTEXT: Pagamento                                  ││
│  │  ├── PagamentoController → PagamentoService                 ││
│  │  ├── PagamentoRepository (JPA) → PostgreSQL (schema: pag)   ││
│  │  └── EventPublisher → Kafka (pagamento.eventos)             ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                  │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ BOUNDED CONTEXT: Estorno                                    ││
│  │  ├── EstornoController → EstornoService                     ││
│  │  ├── EstornoRepository (JPA) → PostgreSQL (schema: estorno) ││
│  │  ├── EventConsumer ← Kafka (pagamento.eventos)              ││
│  │  └── Lock Distribuído (Redisson)                            ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                  │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │ SHARED: Infraestrutura                                      ││
│  │  ├── Idempotência (Redis + fallback BD)                     ││
│  │  ├── Outbox Publisher (job 500ms)                           ││
│  │  ├── Resiliência (Resilience4j)                             ││
│  │  └── Segurança (Spring Security + JWT)                      ││
│  └─────────────────────────────────────────────────────────────┘│
└──────────────────────────────────────────────────────────────────┘
                            │           │
                ┌───────────┴───┐   ┌───┴──────────┐
                │  PostgreSQL   │   │ Kafka Cluster│
                │  (schemas     │   │ (6-12 parts) │
                │   separados)  │   │   + DLQ      │
                └───────────────┘   └──────────────┘
                                           │
                                    ┌──────┴──────────┐
                                    │ Redis (Lettuce  │
                                    │    + Redisson)  │
                                    └─────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│        Adquirente Simulado (Spring Boot - serviço separado)      │
│   POST /autorizacoes  |  POST /estornos                          │
│   (flags: ?falha=timeout&latenciaMs=800&status=NEGADO)           │
└──────────────────────────────────────────────────────────────────┘

┌──────────────────────────────────────────────────────────────────┐
│           Observabilidade & Segurança (Docker Compose)           │
│  Prometheus | Grafana | Jaeger | Keycloak                       │
└──────────────────────────────────────────────────────────────────┘
```

**Nota Importante**: Esta é uma arquitetura **monolito modular**, não microsserviços. Todos os bounded contexts rodam na **mesma JVM** (1 JAR), mas estão **estruturalmente preparados** para extração futura se necessário (Fase 9+).

---

## 📐 Modelo de Domínio

### Máquina de Estados

```
PAGAMENTO:
  PENDENTE ──┬──> AUTORIZADO ──> CANCELADO (via estorno)
             └──> NEGADO

ESTORNO:
  PENDENTE ──┬──> CANCELADO (sucesso)
             └──> NEGADO (fora da janela / já cancelado)
```

### Regras de Negócio

| Regra | Descrição |
|-------|-----------|
| **ID Transação** | Único, imutável, obrigatório (`transacao.id`) |
| **Formas de Pagamento** | `AVISTA` (1x), `PARCELADO LOJA` (≥2x), `PARCELADO EMISSOR` (≥2x) |
| **Estorno** | Apenas pagamentos `AUTORIZADO`; valor total (sem parcial); janela 24h |
| **Moeda** | ISO-4217 (default `BRL`); valores `DECIMAL(15,2)` |
| **Data/Hora** | Entrada `dd/MM/yyyy HH:mm:ss`; armazenamento UTC |
| **NSU/Código Autorização** | Snowflake ID (time-sortable); NSU 10 dígitos, Código 9 dígitos (c/ Luhn) |
| **Mascaramento Cartão** | `4444********1234` em 100% logs/respostas; PAN completo NUNCA armazenado |

---

## 🔒 Segurança

### Autenticação e Autorização
- **Provedor**: Keycloak (JWT Bearer tokens)
- **Escopos**:
  - `pagamentos:ler` — GET /pagamentos
  - `pagamentos:escrever` — POST /pagamentos
  - `estornos:escrever` — POST /estornos
  - `operacoes:reprocessar` — (admin) reprocessamento manual

### Proteções Implementadas
- Validação rigorosa de entrada (Bean Validation JSR-380)
- Limite de tamanho do corpo (max 1MB)
- CORS restritivo (whitelist de origens)
- Rate-limit (Bucket4j, 100 req/min por API-key no gateway)
- Secrets via variáveis de ambiente (migração futura: Vault)
- OWASP Dependency-Check no pipeline CI

### Códigos HTTP

| Código | Uso |
|--------|-----|
| **201** | Pagamento AUTORIZADO criado |
| **202** | Pagamento PENDENTE (reprocesso assíncrono) |
| **400** | Requisição inválida (validação) |
| **401** | Não autenticado (token ausente/inválido) |
| **403** | Não autorizado (escopo insuficiente) |
| **404** | Recurso não encontrado |
| **409** | Conflito (idempotência violada / ID duplicado) |
| **422** | NEGADO (regra de negócio / adquirente) |
| **500** | Erro interno (sem vazar detalhes sensíveis) |

---

## 📊 Observabilidade

### Métricas de Negócio (Custom)
```
pagamentos_autorizados_total
pagamentos_negados_total
estornos_total
tempo_autorizacao_seconds (histograma p95/p99)
```

### Métricas Técnicas
- Latência HTTP (por endpoint)
- Pool de conexões Hikari (active, idle, waiting)
- Threads e GC (JVM)
- Lag e throughput Kafka
- Hit rate Redis

### Endpoints de Monitoramento (em português)
```
/atuador/saude      → health check
/atuador/metricas   → Prometheus metrics
/atuador/info       → versão, build, git commit
```

### Alertas Sugeridos

| Condição | Severidade | Ação |
|----------|------------|------|
| p95 > 300ms por 5min | Warning | Investigar slow queries |
| Erros 5xx > 1% por 5min | Critical | Escalar on-call |
| Fila outbox > 1000 pendentes | Warning | Verificar Kafka |
| Circuit breaker aberto | Warning | Validar adquirente |

---

## 🗄️ Modelo de Dados

### Tabelas Principais

#### **pagamento**
```sql
id                  BIGSERIAL PRIMARY KEY
id_transacao        VARCHAR(50) UNIQUE NOT NULL
status              VARCHAR(20) NOT NULL  -- PENDENTE | AUTORIZADO | NEGADO
valor               DECIMAL(15,2) NOT NULL
moeda               CHAR(3) DEFAULT 'BRL'
data_hora           TIMESTAMP WITH TIME ZONE NOT NULL
estabelecimento     VARCHAR(255) NOT NULL
tipo_pagamento      VARCHAR(30) NOT NULL  -- AVISTA | PARCELADO LOJA | PARCELADO EMISSOR
parcelas            INTEGER NOT NULL
nsu                 VARCHAR(10) UNIQUE
codigo_autorizacao  VARCHAR(9) UNIQUE
cartao_mascarado    VARCHAR(20)
snowflake_id        BIGINT UNIQUE  -- ID técnico para auditoria
criado_em           TIMESTAMP DEFAULT NOW()
atualizado_em       TIMESTAMP DEFAULT NOW()
```

#### **estorno**
```sql
id                  BIGSERIAL PRIMARY KEY
id_transacao        VARCHAR(50) NOT NULL REFERENCES pagamento(id_transacao) ON DELETE RESTRICT
status              VARCHAR(20) NOT NULL  -- PENDENTE | CANCELADO | NEGADO
valor               DECIMAL(15,2) NOT NULL
data_hora           TIMESTAMP WITH TIME ZONE NOT NULL
nsu                 VARCHAR(10) UNIQUE
codigo_autorizacao  VARCHAR(9) UNIQUE
criado_em           TIMESTAMP DEFAULT NOW()
```

#### **outbox**
```sql
id                  BIGSERIAL PRIMARY KEY
tipo_agregado       VARCHAR(50) NOT NULL  -- 'pagamento' | 'estorno'
id_agregado         VARCHAR(50) NOT NULL  -- id_transacao
tipo_evento         VARCHAR(100) NOT NULL -- pagamento.autorizado, estorno.concluido, etc.
payload_json        JSONB NOT NULL
tentativas          INTEGER DEFAULT 0
erro_ultima         TEXT
criado_em           TIMESTAMP DEFAULT NOW()
processado_em       TIMESTAMP  -- NULL enquanto pendente
```

### Índices
```sql
CREATE UNIQUE INDEX uk_pagamento_id_transacao ON pagamento(id_transacao);
CREATE INDEX ix_pagamento_status ON pagamento(status);
CREATE INDEX ix_pagamento_criado_em ON pagamento(criado_em DESC);
CREATE INDEX ix_estorno_id_transacao ON estorno(id_transacao);
CREATE INDEX ix_outbox_processado_em ON outbox(processado_em) WHERE processado_em IS NULL;
```

---

## 📡 Eventos Kafka

### Tópico Principal: `pagamento.eventos`
- **Partições**: 6-12 (estratégia de chave por `id_transacao`)
- **Retenção**: 7 dias
- **Formato**: JSON (Cloud Events futuramente)

### Tipos de Evento
```
pagamento.solicitado
pagamento.autorizado
pagamento.negado
estorno.solicitado
estorno.concluido
estorno.negado
```

### Padrão Outbox (Transactional Outbox)
1. **Write**: Grava entidade + evento na mesma transação DB
2. **Publish**: Job periódico (500ms) processa lotes de 100-500 registros pendentes
3. **Retry**: 3 tentativas com backoff exponencial
4. **DLQ**: Falhas → `pagamento.eventos.dlq` (reprocesso manual documentado)

---

## 🔄 Resiliência (Resilience4j)

### Circuit Breaker (Cliente Adquirente)
```
Closed → Half-Open (após 60s) → Open
Threshold: 50% falhas em 10 chamadas
```

### Retry
- **Tentativas**: 3
- **Backoff**: Exponencial com jitter (100ms → 200ms → 400ms)

### Bulkhead
- **Pool isolado** para chamadas externas (max 10 concurrent)

### Fallback
- Marca pagamento como `PENDENTE`
- Agenda reprocesso assíncrono via Kafka

---

## 🧪 Estratégia de Testes

| Tipo | Ferramentas | Cobertura |
|------|-------------|-----------|
| **Unitários** | JUnit 5 + Mockito + AssertJ | 80% linhas, 70% branches |
| **Integração** | Testcontainers (Postgres, Kafka, Redis) | Fluxos E2E críticos |
| **Contratos** | Spring Cloud Contract / Pact | Consumidores críticos |
| **Carga** | k6 / Gatling | p95 < 300ms, p99 < 500ms |
| **Segurança** | OWASP Dependency-Check | Zero CVEs críticos |

### Adquirente nos Testes
- **Unitários**: `@MockBean` do `AdquirenteClient`
- **Integração**: Serviço `adquirente-simulado` no Docker Compose
  - Flags: `?falha=timeout&latenciaMs=800&status=NEGADO`

---

## 🚀 Roadmap de Implementação

### **Fase 0 — Inicialização** ✅
- Estrutura do repositório + README
- Esqueleto Spring Boot (dependências mínimas)
- Docker Compose: Postgres + Redis + Kafka (preparados, mesmo sem uso inicial)
- **Aceite**: `mvn spring-boot:run` sobe; Flyway cria esquema; `/atuador/saude` retorna `UP`

### **Fase 1 — Domínio e Persistência** 🔄
- Entidades JPA (`Pagamento`, `Estorno`)
- Repositórios Spring Data JPA
- Migrations Flyway (tabelas + índices + constraints)
- **Aceite**: CRUD básico + testes com Testcontainers (Postgres) passando

### **Fase 2 — API Pagamentos e Idempotência** 🔜
- Controllers: `POST /pagamentos`, `GET /pagamentos`, `GET /pagamentos/{idTransacao}`
- Idempotência: Redis (Lettuce) com TTL 24h + fallback BD
- Geração NSU/Código Autorização (Snowflake ID)
- OpenAPI via anotações `springdoc` (exportado em CI)
- **Aceite**: Mesma `Chave-Idempotencia` → mesma resposta; corpo diferente → 409

### **Fase 3 — Mensageria e Outbox (Pagamentos)** 🔜
- Tabela `outbox` + migrations
- Publisher periódico (500ms, lotes 100-500) → Kafka
- Consumidor básico + retries + DLQ
- **Aceite**: Commit pagamento ⇒ evento publicado; reprocesso automático em falha

### **Fase 3.5 — Estornos com Lock Distribuído** 🔜
- Endpoint `POST /estornos` + `GET /estornos/{idTransacao}`
- Lock distribuído (Redisson) por `id_transacao`
- Validações: status AUTORIZADO, janela 24h, valor total
- **Aceite**: Estornos concorrentes bloqueados; apenas um sucede

### **Fase 4 — Resiliência** 🔜
- Resilience4j: circuit breaker, retry, bulkhead (cliente adquirente)
- Adquirente simulado no Docker Compose (flags de falha/latência)
- Fallback: PENDENTE + reprocesso
- **Aceite**: Sob falha do adquirente, API responsiva; sem duplicidade

### **Fase 5 — Observabilidade** 🔜
- Micrometer → Prometheus → Grafana (dashboards HTTP/DB/Kafka)
- OpenTelemetry (agente Java) → Jaeger
- Propagação `Id-Correlacao` entre serviços
- Métricas de negócio customizadas
- **Aceite**: Dashboards funcionais; trace completo em Jaeger

### **Fase 6 — Segurança** 🔜
- Spring Security + JWT
- Keycloak no Docker Compose (realm/cliente demo)
- Escopos: `pagamentos:ler/escrever`, `estornos:escrever`
- **Aceite**: Sem token → 401; escopo incorreto → 403; válido → sucesso

### **Fase 7 — Qualidade e Carga** 🔜
- Cobertura de testes (mínimo 80%/70%)
- Testes de integração completos (Testcontainers)
- Contratos (Pact/Cloud Contract)
- Scripts k6/Gatling + relatórios p95/p99
- OWASP Dependency-Check no pipeline
- **Aceite**: Metas de latência atingidas; zero duplicidade sob concorrência

### **Fase 8 — Publicação (POC Gratuita)** 🔜
- Deploy em VM (Docker Compose) ou free tier gerenciado
- Variáveis de ambiente documentadas (`.env.example`)
- Endpoints públicos + dashboards acessíveis
- **Aceite**: Sistema operacional em ambiente externo

### **Fase 9 — Evoluções Opcionais** 📋
- Rate-limit (Bucket4j) no gateway
- Debezium (CDC) substituindo poller do outbox
- Migração para Kubernetes (HPA, service mesh, Helm)
- Multi-região com replicação

---

## 📦 Entregáveis do Repositório

```
ToolsChallenge/
├── README.md                          # Este arquivo (+ instruções de execução)
├── ARCHITECTURE.md                    # Diagramas, fluxos, decisões técnicas
├── CONTRIBUTING.md                    # Guia para contribuidores
├── CHANGELOG.md                       # Histórico de versões
├── SECURITY.md                        # Políticas de segurança
├── .env.example                       # Variáveis de ambiente (template)
├── docker-compose.yml                 # Infra completa (app + deps)
├── pom.xml                            # Build Maven
│
├── docs/
│   ├── openapi.yaml                   # Contrato OpenAPI (gerado + versionado)
│   └── diagramas/                     # Arquitetura, fluxos, máquina de estados
│
├── src/
│   ├── main/
│   │   ├── java/.../pagamentos/
│   │   │   │
│   │   │   ├── pagamento/            # ← Bounded Context 1 (Pagamento)
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   ├── repository/
│   │   │   │   ├── domain/           # Entidades JPA
│   │   │   │   └── dto/
│   │   │   │
│   │   │   ├── estorno/              # ← Bounded Context 2 (Estorno)
│   │   │   │   ├── controller/
│   │   │   │   ├── service/
│   │   │   │   ├── repository/
│   │   │   │   ├── domain/
│   │   │   │   └── dto/
│   │   │   │
│   │   │   ├── shared/               # ← Código compartilhado (vira lib se extrair)
│   │   │   │   ├── dto/              # DTOs comuns (ErroDTO, etc)
│   │   │   │   ├── event/            # Eventos Kafka (contratos)
│   │   │   │   ├── util/             # SnowflakeIdGenerator, validadores
│   │   │   │   └── exception/        # Exceções customizadas
│   │   │   │
│   │   │   ├── config/               # Configurações Spring
│   │   │   ├── client/               # AdquirenteClient (Resilience4j)
│   │   │   ├── infra/                # Outbox Publisher, Kafka config
│   │   │   └── security/             # JWT, filtros
│   │   │
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── db/migration/         # Flyway scripts
│   │           ├── V1__schema_pagamento.sql
│   │           ├── V2__schema_estorno.sql
│   │           └── V3__schema_outbox.sql
│   │
│   └── test/
│       ├── java/.../pagamentos/
│       │   ├── unit/                 # Testes unitários
│       │   ├── integration/          # Testcontainers
│       │   └── contract/             # Pact/Cloud Contract
│       └── resources/
│
├── monitoramento/
│   ├── dashboards/                   # Grafana JSONs
│   │   ├── api-overview.json
│   │   ├── jvm-metrics.json
│   │   └── kafka-metrics.json
│   └── alertas/                      # Regras Prometheus
│
├── adquirente-simulado/              # Mock externo (Spring Boot leve)
│   ├── src/
│   ├── pom.xml
│   └── Dockerfile
│
└── .github/workflows/
    ├── ci.yml                        # Build + testes + OWASP + Docker
    └── cd.yml                        # Deploy (POC)
```

**Estrutura Modular**: Note a separação clara por **bounded contexts** (`pagamento/`, `estorno/`) mesmo dentro do monolito. Isso facilita a extração futura para microsserviços se necessário (Fase 9+).

---

## 🛠️ Como Executar o Projeto

> **Nota**: Instruções detalhadas de execução serão adicionadas conforme as fases forem implementadas.

### Pré-requisitos
- Java 17+
- Maven 3.9+
- Docker + Docker Compose
- (Opcional) k6, Postman/Insomnia

### Variáveis de Ambiente Principais
_(Ver `.env.example` para lista completa)_

```bash
DATABASE_URL=jdbc:postgresql://localhost:5432/pagamentos
REDIS_HOST=localhost
KAFKA_BOOTSTRAP_SERVERS=localhost:9092
KEYCLOAK_REALM=pagamentos-realm
JWT_ISSUER_URI=http://localhost:8080/realms/pagamentos-realm
```

### Comandos Rápidos
```bash
# Subir infraestrutura
docker-compose up -d postgres redis kafka keycloak

# Build e testes
mvn clean verify

# Executar aplicação
mvn spring-boot:run

# Rodar testes de integração
mvn verify -P integration-test

# Gerar relatório de cobertura
mvn jacoco:report

# Análise de segurança
mvn dependency-check:check
```

---

## 📞 Suporte e Contribuição

- **Issues**: Reporte bugs ou solicite features via GitHub Issues
- **Pull Requests**: Consulte `CONTRIBUTING.md` antes de submeter
- **Segurança**: Vulnerabilidades devem ser reportadas via `SECURITY.md`

---

## 📄 Licença

_(A definir - MIT, Apache 2.0 ou proprietária)_

---

## 🏆 Créditos

Desenvolvido como desafio técnico de arquitetura de APIs enterprise com foco em:
- **Event-Driven Architecture** (Kafka + Padrão Outbox)
- **Monolito Modular Evolutivo** (preparado para microsserviços)
- **Resiliência** (Resilience4j)
- **Observabilidade Full-Stack** (Prometheus + Grafana + Jaeger)
- **Segurança Enterprise** (JWT + Keycloak + PCI-DSS)

**Stack principal**: Spring Boot • Kafka • Redis • PostgreSQL • Resilience4j • Prometheus • Jaeger • Keycloak

---

**Status do Projeto**: 🚧 Em desenvolvimento (Fase 0 - Inicialização)

**Última atualização**: Outubro 2025

---

## 🎓 Conceitos e Decisões Arquiteturais

### Por que Monolito Modular ao invés de Microsserviços?

**Contexto**: Sistema de pagamentos com requisitos claros e escopo definido.

**Decisão**: Arquitetura híbrida (monolito modular event-driven) com caminho de evolução para microsserviços.

**Justificativa**:

| Critério | Monolito Modular | Microsserviços | Escolha |
|----------|------------------|----------------|---------|
| **Produtividade inicial** | 🟢 Alta (1 deploy, 1 pipeline) | 🔴 Baixa (N deploys, N pipelines) | ✅ Monolito |
| **Transações ACID** | 🟢 Nativo (mesmo BD) | 🔴 Saga pattern (complexo) | ✅ Monolito |
| **Debugging** | 🟢 Simples (mesma JVM) | 🔴 Distribuído (tracing obrigatório) | ✅ Monolito |
| **Escala independente** | 🔴 Vertical apenas | 🟢 Granular por serviço | ⚖️ Não necessário agora |
| **Evolução futura** | 🟢 Preparado (bounded contexts) | 🟢 Nativo | ✅ Ambos |

**Padrões Aplicados para Permitir Evolução**:
1. ✅ **Bounded Contexts** separados (pagamento/, estorno/)
2. ✅ **Comunicação assíncrona** via Kafka (não chamadas diretas)
3. ✅ **Schemas PostgreSQL isolados** (fácil migrar para DBs separados)
4. ✅ **Eventos como contratos** (JSON versionado, não objetos Java)
5. ✅ **Biblioteca shared/** (vira artefato Maven se extrair)

**Quando Migrar para Microsserviços?**
- Time > 5 desenvolvedores
- Necessidade de escala independente (ex.: consultas 10x mais que escritas)
- Deploy independente obrigatório (times autônomos)
- Maturidade em Kubernetes, observabilidade distribuída, saga patterns

### Por que PostgreSQL ao invés de MongoDB?

**Decisão**: PostgreSQL 15+

**Justificativa**:

| Requisito | PostgreSQL | MongoDB | Escolha |
|-----------|------------|---------|---------|
| **Transações ACID multi-tabela** | 🟢 Nativo | 🟡 Limitado (overhead) | ✅ Postgres |
| **Padrão Outbox** | 🟢 Simples (BEGIN/COMMIT) | 🔴 Complexo (multi-doc) | ✅ Postgres |
| **Foreign Keys** | 🟢 Nativo | 🔴 Inexistente | ✅ Postgres |
| **Unicidade composta** | 🟢 UNIQUE (col1, col2) | 🟡 Manual no código | ✅ Postgres |
| **Auditoria financeira** | 🟢 WAL, PITR | 🟡 Oplog (menos ferramentas) | ✅ Postgres |
| **Escala horizontal** | 🟡 Particionamento manual | 🟢 Sharding nativo | ⚖️ Não crítico agora |

**Uso futuro de MongoDB**: CQRS read-model (Fase 9 - microserviço de consulta)

### Por que Snowflake ID para NSU/Código Autorização?

**Decisão**: Snowflake ID (64-bit time-sortable) com derivação numérica

**Alternativas Avaliadas**:
- ❌ UUID v4: Não tem ordem temporal, difícil converter para número curto
- ❌ Sequence PostgreSQL: Não escala em multi-DC, previsível (segurança)
- ✅ Snowflake ID: Ordenação + baixa colisão + distribuído

**Implementação**:
```
Snowflake 64-bit → NSU (10 dígitos) = snowflake % 10^10
                 → Código (9 dígitos) = (snowflake % 10^8) + dígito Luhn
```

**Vantagens**:
- ✅ Ordenação temporal (útil para debug)
- ✅ Zero coordenação central
- ✅ Colisão negligenciável (timestamp + workerId + sequence)

---

**Pronto para iniciar a Fase 0!** 🚀
