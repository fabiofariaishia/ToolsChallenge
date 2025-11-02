# 📘 **ToolsChallenge - API de Pagamentos Sicredi**

## 📋 **Índice**
1. [Visão Geral](#-visão-geral)
2. [Arquitetura](#-arquitetura)
3. [Estrutura de Pastas](#-estrutura-de-pastas)
4. [Stack Tecnológico](#-stack-tecnológico)
5. [Padrões Implementados](#-padrões-implementados)
6. [Módulos e Funcionalidades](#-módulos-e-funcionalidades)
7. [Camadas da Aplicação](#-camadas-da-aplicação)
8. [Banco de Dados](#-banco-de-dados)
9. [Mensageria (Kafka)](#-mensageria-kafka)
10. [Cache e Locks Distribuídos](#-cache-e-locks-distribuídos)
11. [Resiliência (Resilience4j)](#-resiliência-resilience4j)
12. [Observabilidade](#-observabilidade)
13. [Segurança](#-segurança)
14. [APIs e Endpoints](#-apis-e-endpoints)
15. [Configuração e Ambiente](#-configuração-e-ambiente)
16. [Testes](#-testes)
17. [Deploy e CI/CD](#-deploy-e-cicd)
18. [Monitoramento](#-monitoramento)
19. [Troubleshooting](#-troubleshooting)
20. [Roadmap](#-roadmap)

---

## 🎯 **Visão Geral**

**ToolsChallenge** é uma API REST de processamento de pagamentos desenvolvida para o **Sicredi**, implementando padrões de arquitetura moderna, resiliente e escalável.

### ⚠️ **IMPORTANTE: Arquitetura Monolito Modular → Microserviços**

Este projeto está sendo desenvolvido como **Monolito Modular** com a visão de evolução para **Microserviços**. 

**Princípios Fundamentais:**

1. **Modularização Estrita**
   - Cada módulo (`pagamento`, `estorno`, `adquirente`) é **autocontido** e **independente**
   - Baixo acoplamento entre módulos
   - Alta coesão dentro de cada módulo
   - Comunicação entre módulos APENAS via interfaces bem definidas

2. **Preparação para Microserviços**
   - ✅ Cada módulo deve ter seu próprio **pacote raiz** (`br.com.sicredi.toolschallenge.{modulo}`)
   - ✅ Não compartilhar **entidades JPA** entre módulos
   - ✅ Usar **DTOs** para comunicação entre módulos
   - ✅ Eventos de domínio para comunicação assíncrona (já preparados para mensageria distribuída)
   - ✅ Infraestrutura compartilhada apenas para **cross-cutting concerns** (`infra/`, `shared/`)

3. **Regras de Desenvolvimento**
   - ❌ **NUNCA** fazer `import` direto de classes de domínio de outro módulo
   - ❌ **NUNCA** usar `@Autowired` de `Service` de outro módulo diretamente
   - ❌ **NUNCA** criar abstrações complexas desnecessárias (custom annotations, frameworks internos, etc)
   - ✅ **SEMPRE** usar eventos de domínio para comunicação assíncrona
   - ✅ **SEMPRE** usar DTOs para comunicação síncrona (se necessário)
   - ✅ **SEMPRE** preferir simplicidade: use recursos nativos do Spring/Java antes de criar código customizado
   - ✅ **SEMPRE** pensar: "Se esse módulo fosse um microserviço separado, isso funcionaria?"
   - ✅ **SEMPRE** questionar: "Preciso mesmo criar isso ou já existe uma solução padrão?"

4. **Princípio KISS (Keep It Simple, Stupid)**
   - 🎯 **Simplicidade sobre Complexidade**: O código mais fácil de manter é o código simples
   - 🚫 **Evite Over-Engineering**: Não crie abstrações "para o futuro" que podem nunca ser necessárias
   - ✅ **Use o Padrão**: Bean Validation (`@NotNull`, `@Size`) em vez de annotations customizadas
   - ✅ **Use o Framework**: Spring já resolve 90% dos problemas, não reinvente a roda
   - ⚠️ **Exemplo de Complexidade Desnecessária**:
     ```java
     // ❌ ERRADO: Criar annotation customizada para algo que Bean Validation já faz
     @Target(ElementType.FIELD)
     @Retention(RetentionPolicy.RUNTIME)
     @Constraint(validatedBy = ValorMinimoValidator.class)
     public @interface ValorMinimo {
         String message() default "Valor inválido";
         double value();
     }
     
     // ✅ CORRETO: Usar Bean Validation padrão
     @DecimalMin(value = "0.01", message = "Valor mínimo é R$ 0,01")
     private BigDecimal valor;
     ```
   - 💡 **Regra de Ouro**: Se você está criando código que parece "muito inteligente", provavelmente está fazendo errado

5. **Estrutura de Banco de Dados**
   - Cada módulo tem suas **próprias tabelas**
   - Não há FK (Foreign Keys) entre tabelas de módulos diferentes
   - Consistência eventual via eventos

### **Objetivos do Projeto**
- ✅ Processar transações de pagamento com alta confiabilidade
- ✅ Garantir idempotência em todas as operações
- ✅ Implementar auditoria completa de eventos
- ✅ Suportar estornos com controle de concorrência
- ✅ Garantir consistência eventual via Event Sourcing
- ✅ Resiliência na comunicação com serviços externos (adquirente)
- ✅ Observabilidade total (logs, métricas, traces)
- ✅ **Preparar para migração para microserviços sem reescrever código**

### **Características Principais**
- 🔐 **Idempotência**: Chaves idempotentes em todos os endpoints mutáveis
- 🔄 **Outbox Pattern**: Garantia de entrega de eventos via transactional outbox
- 🔒 **Locks Distribuídos**: Prevenção de race conditions com Redisson
- 🛡️ **Resiliência**: Circuit Breaker, Retry e Bulkhead com Resilience4j
- 📊 **Auditoria**: Registro completo de todos os eventos de negócio
- 🚀 **Performance**: Cache Redis e processamento assíncrono via Kafka

---

## 🏗️ **Arquitetura**

### **Arquitetura: Monolito Modular**

Este projeto adota a arquitetura **Modular Monolith** (Monolito Modular), que combina os benefícios de um monolito (simplicidade de deploy, baixa latência) com a modularização de microserviços (independência, escalabilidade de desenvolvimento).

```
┌────────────────────────────────────────────────────────────────────┐
│                    MONOLITO MODULAR                                │
│  ┌──────────────┐  ┌──────────────┐  ┌──────────────────────┐    │
│  │   MÓDULO     │  │   MÓDULO     │  │      MÓDULO          │    │
│  │  PAGAMENTO   │  │   ESTORNO    │  │    ADQUIRENTE        │    │
│  │              │  │              │  │                      │    │
│  │ ┌──────────┐ │  │ ┌──────────┐ │  │ ┌──────────────────┐ │    │
│  │ │Controller│ │  │ │Controller│ │  │ │    Service       │ │    │
│  │ │  (API)   │ │  │ │  (API)   │ │  │ │  (Internal)      │ │    │
│  │ └────┬─────┘ │  │ └────┬─────┘ │  │ └────────┬─────────┘ │    │
│  │      │       │  │      │       │  │          │           │    │
│  │ ┌────▼─────┐ │  │ ┌────▼─────┐ │  │ ┌────────▼─────────┐ │    │
│  │ │ Service  │ │  │ │ Service  │ │  │ │ Simulado Service │ │    │
│  │ │(Lógica)  │ │  │ │(Lógica)  │ │  │ │  + Resilience4j  │ │    │
│  │ └────┬─────┘ │  │ └────┬─────┘ │  │ └──────────────────┘ │    │
│  │      │       │  │      │       │  │                      │    │
│  │ ┌────▼─────┐ │  │ ┌────▼─────┐ │  │                      │    │
│  │ │Repository│ │  │ │Repository│ │  │                      │    │
│  │ │   (DB)   │ │  │ │   (DB)   │ │  │                      │    │
│  │ └──────────┘ │  │ └──────────┘ │  │                      │    │
│  └──────┬───────┘  └──────┬───────┘  └──────────────────────┘    │
│         │                  │                                       │
│         └──────────────────┴──────────────────┐                   │
│                                                │                   │
│  ┌─────────────────────────────────────────────▼────────────────┐ │
│  │              INFRAESTRUTURA (SHARED)                         │ │
│  │  ┌──────────────┐ ┌──────────────┐ ┌────────────────────┐  │ │
│  │  │ Idempotência │ │   Auditoria  │ │  Outbox Pattern    │  │ │
│  │  └──────────────┘ └──────────────┘ └────────────────────┘  │ │
│  │  ┌──────────────┐ ┌──────────────┐ ┌────────────────────┐  │ │
│  │  │    Configs   │ │  Exceptions  │ │  Locks Distribuídos│  │ │
│  │  └──────────────┘ └──────────────┘ └────────────────────┘  │ │
│  └─────────────────────────────────────────────────────────────┘ │
└─────────────────────────┬──────────────────────────────────────────┘
                          │
         ┌────────────────┼────────────────┐
         │                │                │
    ┌────▼─────┐   ┌──────▼───┐   ┌───────▼────┐
    │PostgreSQL│   │  Redis   │   │   Kafka    │
    │ (Dados)  │   │ (Cache/  │   │ (Eventos)  │
    │          │   │  Locks)  │   │            │
    └──────────┘   └──────────┘   └────────────┘
```

### **Por que Monolito Modular?**

| Aspecto | Monolito Modular | Microserviços Puros |
|---------|------------------|---------------------|
| **Deploy** | ✅ Simples (1 JAR) | ❌ Complexo (N serviços) |
| **Latência** | ✅ Baixa (in-process) | ❌ Alta (network calls) |
| **Desenvolvimento** | ✅ Rápido (sem overhead) | ❌ Lento (infra complexa) |
| **Transações** | ✅ ACID nativo | ❌ Eventual consistency |
| **Modularização** | ✅ Forte (pacotes) | ✅ Forte (serviços) |
| **Escalabilidade** | ⚠️ Vertical | ✅ Horizontal por serviço |
| **Migração** | ✅ Gradual para MS | - |

**Decisão**: Começar com Monolito Modular e migrar módulos específicos para microserviços conforme necessidade de escala.

### **Evolução: Monolito → Microserviços**

```
FASE 1: MONOLITO MODULAR (ATUAL)          FASE 2: HÍBRIDO                    FASE 3: MICROSERVIÇOS
┌─────────────────────────┐                ┌──────────────┐                   ┌──────────────┐
│    MONOLITO (JAR)       │                │  MONOLITO    │                   │  Gateway     │
│  ┌────────┐ ┌────────┐  │                │ ┌──────────┐ │                   └──────┬───────┘
│  │Pagamen.│ │Estorno │  │                │ │Pagamento │ │                          │
│  │Service │ │Service │  │   ────────►    │ │ Service  │ │    ────────►      ┌──────┼───────┐
│  └────────┘ └────────┘  │                │ └──────────┘ │                   │      │       │
│  ┌────────────────────┐  │                └──────┬───────┘           ┌───────▼──┐ ┌─▼──────────┐
│  │ Adquirente Service │  │                       │                   │Pagamento │ │  Estorno   │
│  └────────────────────┘  │                       │ HTTP              │ Service  │ │  Service   │
└───────────┬───────────────┘                       │                   │          │ │            │
            │                                ┌──────▼────────┐          │ (Port    │ │ (Port      │
     ┌──────▼──────┐                         │  Adquirente   │          │  8081)   │ │  8082)     │
     │ PostgreSQL  │                         │  Service      │          └──────────┘ └────────────┘
     │   (Shared)  │                         │  (Separado)   │                 │            │
     └─────────────┘                         │  (Port 8082)  │          ┌──────▼────────────▼──────┐
                                             └───────────────┘          │  PostgreSQL (Separados) │
✅ Deploy simples                            ⚠️ Escala específica        └─────────────────────────┘
✅ Latência baixa                            ✅ Módulo crítico isolado   ✅ Escala independente
⚠️ Escala vertical                           ⚠️ 2 deploys gerenciar      ✅ Times autônomos
                                                                         ⚠️ Complexidade operacional
```

### **Arquitetura Geral (Simplificada)**

```
┌─────────────┐
│   Cliente   │
└──────┬──────┘
       │ HTTP REST
       ▼
┌──────────────────────────────────────────────────────────┐
│          API REST (Spring Boot)                          │
│  ┌────────────┐  ┌────────────┐  ┌──────────────────┐  │
│  │ Controller │→ │  Service   │→ │   Repository     │  │
│  └────────────┘  └────────────┘  └──────────────────┘  │
│         │              │                    │            │
│         ▼              ▼                    ▼            │
│  ┌──────────────────────────────────────────────────┐  │
│  │        Infraestrutura (Cross-cutting)            │  │
│  │  • Idempotência  • Auditoria  • Locks  • Outbox │  │
│  └──────────────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────┘
       │                │                │
       ▼                ▼                ▼
┌────────────┐   ┌──────────┐    ┌──────────────┐
│ PostgreSQL │   │  Redis   │    │    Kafka     │
│  (Dados)   │   │ (Cache/  │    │ (Eventos)    │
│            │   │  Locks)  │    │              │
└────────────┘   └──────────┘    └──────────────┘
                                        │
                                        ▼
                            ┌──────────────────────┐
                            │  Consumidores Kafka  │
                            │  (Outros Sistemas)   │
                            └──────────────────────┘
```

### **Padrões Arquiteturais**

#### **1. Domain-Driven Design (DDD)**
- **Entidades**: `Pagamento`, `Estorno`
- **Value Objects**: `StatusPagamento`, `StatusEstorno`, `TipoPagamento`
- **Aggregates**: Cada `Pagamento` é um aggregate root que gerencia seus `Estornos`
- **Repositories**: Abstração de persistência (`PagamentoRepository`, `EstornoRepository`)

#### **2. Clean Architecture (Camadas)**
```
┌───────────────────────────────────────┐
│   Presentation (Controllers)          │  ← Entrada HTTP
├───────────────────────────────────────┤
│   Application (Services/DTOs)         │  ← Lógica de aplicação
├───────────────────────────────────────┤
│   Domain (Entities/Enums)             │  ← Regras de negócio
├───────────────────────────────────────┤
│   Infrastructure (Config/Jobs)        │  ← Tecnologias (DB, Kafka, Redis)
└───────────────────────────────────────┘
```

#### **3. Event-Driven Architecture**
- **Eventos de Domínio**: `PagamentoCriadoEvento`, `EstornoStatusAlteradoEvento`
- **Outbox Pattern**: Garante entrega via tabela transacional (`outbox_evento`)
- **Event Listeners**: Processadores assíncronos de eventos de auditoria

#### **4. Microservices Patterns**
- **Transactional Outbox**: Garantia de consistência entre DB e Kafka
- **Idempotency**: Prevenção de duplicação de transações
- **Circuit Breaker**: Proteção contra falhas em cascata (adquirente)
- **Distributed Lock**: Controle de concorrência em estornos

---

## 📐 **Anatomia de um Módulo (Pattern)**

### **Estrutura Padrão de Módulo**

Cada módulo segue a mesma estrutura para garantir consistência e facilitar a migração para microserviços:

```
{modulo}/                              # Ex: pagamento/, estorno/, adquirente/
├── controller/                        # 🌐 Camada de Apresentação (REST API)
│   └── {Modulo}Controller.java       # Endpoints HTTP, validação de entrada
│
├── service/                           # 💼 Camada de Aplicação (Lógica de Negócio)
│   └── {Modulo}Service.java          # Orquestração, transações, eventos
│
├── repository/                        # 💾 Camada de Persistência
│   └── {Modulo}Repository.java       # Spring Data JPA
│
├── domain/                            # 🎯 Camada de Domínio (Core)
│   ├── {Modulo}.java                 # Entidade JPA principal
│   ├── Status{Modulo}.java           # Enum de status
│   └── Tipo{Modulo}.java             # Outros enums (opcional)
│
├── dto/                               # 📦 Data Transfer Objects
│   ├── {Modulo}RequestDTO.java       # Request da API
│   ├── {Modulo}ResponseDTO.java      # Response da API
│   └── {Modulo}Mapper.java           # Conversão Entity ↔ DTO
│
├── events/                            # 📨 Eventos de Domínio
│   ├── {Modulo}CriadoEvento.java     # Evento de criação
│   └── {Modulo}StatusAlteradoEvento.java  # Evento de mudança de status
│
└── exception/                         # ⚠️ Exceções específicas do módulo
    └── {Modulo}Exception.java        # Exceções customizadas (opcional)
```

### **Exemplo Prático: Módulo `pagamento/`**

#### **1. Controller** (`controller/PagamentoController.java`)

```java
package br.com.sicredi.toolschallenge.pagamento.controller;

import br.com.sicredi.toolschallenge.pagamento.dto.*;
import br.com.sicredi.toolschallenge.pagamento.service.PagamentoService;
import br.com.sicredi.toolschallenge.infra.idempotencia.annotation.Idempotente;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.validation.Valid;

@RestController
@RequestMapping("/pagamentos")
@RequiredArgsConstructor
public class PagamentoController {
    
    private final PagamentoService service;
    
    @PostMapping
    @Idempotente  // Interceptor automático de idempotência
    public ResponseEntity<PagamentoResponseDTO> criar(
        @Valid @RequestBody PagamentoRequestDTO request
    ) {
        PagamentoResponseDTO response = service.criarPagamento(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PagamentoResponseDTO> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(service.buscarPorId(id));
    }
}
```

**Responsabilidades**:
- ✅ Receber requisições HTTP
- ✅ Validar entrada (`@Valid`)
- ✅ Delegar para Service
- ✅ Retornar status HTTP correto
- ❌ **NUNCA** ter lógica de negócio
- ❌ **NUNCA** acessar Repository diretamente

#### **2. Service** (`service/PagamentoService.java`)

```java
package br.com.sicredi.toolschallenge.pagamento.service;

import br.com.sicredi.toolschallenge.pagamento.domain.Pagamento;
import br.com.sicredi.toolschallenge.pagamento.domain.StatusPagamento;
import br.com.sicredi.toolschallenge.pagamento.dto.*;
import br.com.sicredi.toolschallenge.pagamento.events.*;
import br.com.sicredi.toolschallenge.pagamento.repository.PagamentoRepository;
import br.com.sicredi.toolschallenge.adquirente.service.AdquirenteService;
import br.com.sicredi.toolschallenge.adquirente.dto.AutorizacaoRequest;
import br.com.sicredi.toolschallenge.infra.outbox.service.OutboxService;
import br.com.sicredi.toolschallenge.shared.exception.RecursoNaoEncontradoException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PagamentoService {
    
    private final PagamentoRepository repository;
    private final OutboxService outboxService;
    private final AdquirenteService adquirenteService;  // Comunicação entre módulos
    
    @Transactional
    public PagamentoResponseDTO criarPagamento(PagamentoRequestDTO dto) {
        log.info("Criando pagamento: {}", dto.getDescricao());
        
        // 1. Converter DTO → Entity
        Pagamento pagamento = PagamentoMapper.toEntity(dto);
        pagamento.setStatus(StatusPagamento.PENDENTE);
        
        // 2. Autorizar com adquirente (via módulo separado)
        AutorizacaoRequest autorizacaoReq = new AutorizacaoRequest(
            "1234567890123456", "123", "12/2025", 
            dto.getValor(), dto.getDescricao()
        );
        var autorizacao = adquirenteService.autorizarPagamento(autorizacaoReq);
        
        // 3. Atualizar status baseado na autorização
        if (autorizacao.autorizado()) {
            pagamento.setStatus(StatusPagamento.PROCESSADO);
            pagamento.setNsu(autorizacao.nsu());
            pagamento.setCodigoAutorizacao(autorizacao.codigoAutorizacao());
        } else if (autorizacao.isPendente()) {
            pagamento.setStatus(StatusPagamento.PENDENTE);
        } else {
            pagamento.setStatus(StatusPagamento.ERRO);
        }
        
        // 4. Persistir
        pagamento = repository.save(pagamento);
        
        // 5. Publicar evento (Outbox Pattern)
        PagamentoCriadoEvento evento = new PagamentoCriadoEvento(pagamento);
        outboxService.salvar("Pagamento", pagamento.getId(), evento);
        
        log.info("Pagamento criado com sucesso: ID={}, Status={}", 
            pagamento.getId(), pagamento.getStatus());
        
        // 6. Converter Entity → DTO
        return PagamentoMapper.toResponseDTO(pagamento);
    }
    
    public PagamentoResponseDTO buscarPorId(Long id) {
        Pagamento pagamento = repository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Pagamento não encontrado: " + id
            ));
        return PagamentoMapper.toResponseDTO(pagamento);
    }
}
```

**Responsabilidades**:
- ✅ Lógica de negócio e orquestração
- ✅ Gerenciar transações (`@Transactional`)
- ✅ Converter DTOs ↔ Entities
- ✅ Publicar eventos de domínio
- ✅ Comunicar com outros módulos via DTOs
- ❌ **NUNCA** retornar entidades JPA para Controller
- ❌ **NUNCA** receber HttpServletRequest/Response

#### **3. Repository** (`repository/PagamentoRepository.java`)

```java
package br.com.sicredi.toolschallenge.pagamento.repository;

import br.com.sicredi.toolschallenge.pagamento.domain.Pagamento;
import br.com.sicredi.toolschallenge.pagamento.domain.StatusPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PagamentoRepository extends JpaRepository<Pagamento, Long> {
    
    // Queries derivadas do nome do método
    List<Pagamento> findByStatus(StatusPagamento status);
    
    Optional<Pagamento> findByNsu(String nsu);
    
    // Query customizada (JPQL)
    @Query("SELECT p FROM Pagamento p WHERE p.status = :status " +
           "AND p.dataCriacao >= CURRENT_DATE")
    List<Pagamento> buscarPagamentosHoje(StatusPagamento status);
}
```

**Responsabilidades**:
- ✅ Abstração de acesso ao banco
- ✅ Queries customizadas (JPQL ou @Query)
- ❌ **NUNCA** ter lógica de negócio

#### **4. Domain** (`domain/Pagamento.java`)

```java
package br.com.sicredi.toolschallenge.pagamento.domain;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagamento", indexes = {
    @Index(name = "idx_pagamento_status", columnList = "status"),
    @Index(name = "idx_pagamento_nsu", columnList = "nsu")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pagamento {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String descricao;
    
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pagamento", nullable = false, length = 20)
    private TipoPagamento tipoPagamento;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusPagamento status;
    
    @Column(length = 50)
    private String nsu;
    
    @Column(name = "codigo_autorizacao", length = 50)
    private String codigoAutorizacao;
    
    @Column(name = "data_criacao", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;
    
    @Column(name = "data_atualizacao")
    private LocalDateTime dataAtualizacao;
    
    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        dataAtualizacao = LocalDateTime.now();
    }
}
```

**Responsabilidades**:
- ✅ Representar o modelo de domínio
- ✅ Mapeamento JPA
- ✅ Validações de domínio (via métodos de negócio)
- ❌ **NUNCA** ser exposta diretamente na API (usar DTOs)

#### **5. DTOs** (`dto/`)

**Request DTO**:
```java
package br.com.sicredi.toolschallenge.pagamento.dto;

import br.com.sicredi.toolschallenge.pagamento.domain.TipoPagamento;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagamentoRequestDTO {
    
    @NotBlank(message = "Descrição é obrigatória")
    @Size(max = 255, message = "Descrição deve ter no máximo 255 caracteres")
    private String descricao;
    
    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor mínimo é R$ 0,01")
    private BigDecimal valor;
    
    @NotNull(message = "Tipo de pagamento é obrigatório")
    private TipoPagamento tipoPagamento;
}
```

**Response DTO**:
```java
package br.com.sicredi.toolschallenge.pagamento.dto;

import br.com.sicredi.toolschallenge.pagamento.domain.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PagamentoResponseDTO {
    private Long id;
    private String descricao;
    private BigDecimal valor;
    private TipoPagamento tipoPagamento;
    private StatusPagamento status;
    private String nsu;
    private String codigoAutorizacao;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;
}
```

**Mapper**:
```java
package br.com.sicredi.toolschallenge.pagamento.dto;

import br.com.sicredi.toolschallenge.pagamento.domain.Pagamento;

public class PagamentoMapper {
    
    public static Pagamento toEntity(PagamentoRequestDTO dto) {
        return Pagamento.builder()
            .descricao(dto.getDescricao())
            .valor(dto.getValor())
            .tipoPagamento(dto.getTipoPagamento())
            .build();
    }
    
    public static PagamentoResponseDTO toResponseDTO(Pagamento entity) {
        return PagamentoResponseDTO.builder()
            .id(entity.getId())
            .descricao(entity.getDescricao())
            .valor(entity.getValor())
            .tipoPagamento(entity.getTipoPagamento())
            .status(entity.getStatus())
            .nsu(entity.getNsu())
            .codigoAutorizacao(entity.getCodigoAutorizacao())
            .dataCriacao(entity.getDataCriacao())
            .dataAtualizacao(entity.getDataAtualizacao())
            .build();
    }
}
```

#### **6. Events** (`events/`)

```java
package br.com.sicredi.toolschallenge.pagamento.events;

import br.com.sicredi.toolschallenge.pagamento.domain.Pagamento;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagamentoCriadoEvento {
    private String tipoEvento = "PAGAMENTO_CRIADO";
    private LocalDateTime timestamp = LocalDateTime.now();
    private Long agregadoId;
    private DadosPagamento dados;
    
    public PagamentoCriadoEvento(Pagamento pagamento) {
        this.agregadoId = pagamento.getId();
        this.dados = new DadosPagamento(
            pagamento.getId(),
            pagamento.getDescricao(),
            pagamento.getValor(),
            pagamento.getStatus().name(),
            pagamento.getNsu(),
            pagamento.getCodigoAutorizacao()
        );
    }
    
    @Data
    @AllArgsConstructor
    public static class DadosPagamento {
        private Long id;
        private String descricao;
        private BigDecimal valor;
        private String status;
        private String nsu;
        private String codigoAutorizacao;
    }
}
```

---

## 🔧 **Pasta `shared/` - Componentes Compartilhados**

A pasta `shared/` contém componentes **transversais** (cross-cutting concerns) que são usados por **todos os módulos**. Estes componentes são **stateless** e **genéricos**.

### **Estrutura `shared/`**

```
shared/
├── config/                            # ⚙️ Configurações Globais
│   ├── KafkaConfig.java              # Configuração de producers/consumers Kafka
│   ├── RedisConfig.java              # Configuração do Redis (cache)
│   ├── RedissonConfig.java           # Configuração Redisson (locks distribuídos)
│   └── IdempotenciaConfig.java       # Registra interceptors de idempotência
│
└── exception/                         # ⚠️ Exceções Globais
    ├── GlobalExceptionHandler.java   # @ControllerAdvice - trata todas exceções
    ├── NegocioException.java         # Exceção genérica de regra de negócio
    ├── RecursoNaoEncontradoException.java  # 404 Not Found
    └── ErroResposta.java             # DTO padrão de erro

```

### **Exemplo: `shared/config/KafkaConfig.java`**

```java
package br.com.sicredi.toolschallenge.shared.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonSerializer;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConfig {
    
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "all");  // Garantia de escrita
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        return new DefaultKafkaProducerFactory<>(config);
    }
    
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

### **Exemplo: `shared/exception/GlobalExceptionHandler.java`**

```java
package br.com.sicredi.toolschallenge.shared.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErroResposta> handleNotFound(RecursoNaoEncontradoException ex) {
        log.warn("Recurso não encontrado: {}", ex.getMessage());
        ErroResposta erro = new ErroResposta(
            HttpStatus.NOT_FOUND.value(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(erro);
    }
    
    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErroResposta> handleNegocio(NegocioException ex) {
        log.warn("Erro de negócio: {}", ex.getMessage());
        ErroResposta erro = new ErroResposta(
            HttpStatus.UNPROCESSABLE_ENTITY.value(),
            ex.getMessage(),
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(erro);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResposta> handleValidation(
        MethodArgumentNotValidException ex
    ) {
        String mensagem = ex.getBindingResult().getFieldErrors().stream()
            .map(e -> e.getField() + ": " + e.getDefaultMessage())
            .collect(Collectors.joining(", "));
        
        log.warn("Erro de validação: {}", mensagem);
        ErroResposta erro = new ErroResposta(
            HttpStatus.BAD_REQUEST.value(),
            mensagem,
            LocalDateTime.now()
        );
        return ResponseEntity.badRequest().body(erro);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResposta> handleGeneric(Exception ex) {
        log.error("Erro inesperado", ex);
        ErroResposta erro = new ErroResposta(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Erro interno do servidor",
            LocalDateTime.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(erro);
    }
}
```

### **Exemplo: `shared/exception/ErroResposta.java`**

```java
package br.com.sicredi.toolschallenge.shared.exception;

import lombok.*;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErroResposta {
    private Integer status;
    private String mensagem;
    private LocalDateTime timestamp;
}
```

---

## ✅ **Checklist para Criar um Novo Módulo**

Ao criar um novo módulo (ex: `notificacao/`, `relatorio/`), siga este checklist:

- [ ] **1. Criar estrutura de pastas**:
  ```
  {modulo}/
  ├── controller/
  ├── service/
  ├── repository/
  ├── domain/
  ├── dto/
  ├── events/
  └── exception/ (se necessário)
  ```

- [ ] **2. Criar entidade JPA** (`domain/{Modulo}.java`):
  - Usar `@Entity`, `@Table`, `@Id`, `@GeneratedValue`
  - Adicionar índices (`@Index`) para campos consultados
  - Implementar `@PrePersist` e `@PreUpdate` para timestamps

- [ ] **3. Criar enums de domínio** (`domain/Status{Modulo}.java`):
  - Status do ciclo de vida da entidade
  - Outros value objects necessários

- [ ] **4. Criar Repository** (`repository/{Modulo}Repository.java`):
  - Extend `JpaRepository<{Modulo}, Long>`
  - Adicionar queries customizadas se necessário

- [ ] **5. Criar DTOs** (`dto/`):
  - Request DTO com validações (`@NotNull`, `@NotBlank`, etc)
  - Response DTO (pode expor mais campos que Request)
  - Mapper com métodos `toEntity()` e `toResponseDTO()`

- [ ] **6. Criar Service** (`service/{Modulo}Service.java`):
  - Anotar com `@Service`, `@RequiredArgsConstructor`, `@Slf4j`
  - Usar `@Transactional` em métodos que modificam dados
  - Publicar eventos via `OutboxService`
  - **Nunca** injetar `Repository` ou `Service` de outro módulo diretamente

- [ ] **7. Criar Controller** (`controller/{Modulo}Controller.java`):
  - Anotar com `@RestController`, `@RequestMapping`
  - Usar `@Idempotente` em POST/PUT
  - Validar entrada com `@Valid`
  - Retornar status HTTP corretos (201, 200, 404, etc)

- [ ] **8. Criar Eventos de Domínio** (`events/`):
  - Evento de criação (`{Modulo}CriadoEvento`)
  - Evento de mudança de status (`{Modulo}StatusAlteradoEvento`)
  - Incluir timestamp e dados relevantes

- [ ] **9. Criar Migração Flyway** (`resources/db/migration/V{n}__criar_tabela_{modulo}.sql`):
  - DDL completo da tabela
  - Índices necessários
  - Constraints (PK, NOT NULL, etc)

- [ ] **10. Criar Testes**:
  - Testes unitários do Service (`{Modulo}ServiceTest.java`)
  - Testes de integração (`{Modulo}IntegrationTest.java`)

---

## 🎖️ **Regras de Ouro para Monolito Modular**

### **As 10 Regras que NÃO Podem Ser Quebradas**

#### **1. 🚫 Nunca importe entidades JPA de outro módulo**
```java
// ❌ PROIBIDO
import br.com.sicredi.toolschallenge.pagamento.domain.Pagamento;

// ✅ PERMITIDO
import br.com.sicredi.toolschallenge.pagamento.dto.PagamentoResponseDTO;
```

#### **2. 🚫 Nunca crie Foreign Keys entre tabelas de módulos diferentes**
```sql
-- ❌ PROIBIDO
FOREIGN KEY (pagamento_id) REFERENCES pagamento(id)

-- ✅ PERMITIDO
pagamento_id BIGINT NOT NULL  -- Apenas referência lógica
```

#### **3. ✅ Sempre use DTOs para comunicação entre módulos**
```java
// ✅ CORRETO
AutorizacaoRequest request = new AutorizacaoRequest(...);
AutorizacaoResponse response = adquirenteService.autorizarPagamento(request);
```

#### **4. ✅ Sempre publique eventos de domínio para mudanças importantes**
```java
// ✅ CORRETO - Outros módulos podem reagir assincronamente
PagamentoCriadoEvento evento = new PagamentoCriadoEvento(pagamento);
outboxService.salvar("Pagamento", pagamento.getId(), evento);
```

#### **5. ✅ Controllers só devem retornar DTOs, nunca entidades**
```java
// ❌ PROIBIDO
public ResponseEntity<Pagamento> criar(...) { }

// ✅ PERMITIDO
public ResponseEntity<PagamentoResponseDTO> criar(...) { }
```

#### **6. ✅ Services devem ser transacionais**
```java
// ✅ CORRETO
@Transactional
public PagamentoResponseDTO criar(...) {
    // Operações atomicas
}
```

#### **7. ✅ Use @Idempotente em todos os endpoints de modificação**
```java
// ✅ CORRETO
@PostMapping
@Idempotente
public ResponseEntity<PagamentoResponseDTO> criar(...) { }
```

#### **8. ✅ Validações de entrada no DTO com Bean Validation**
```java
// ✅ CORRETO
@NotNull(message = "Valor é obrigatório")
@DecimalMin(value = "0.01", message = "Valor mínimo é R$ 0,01")
private BigDecimal valor;
```

#### **9. ✅ Timestamps automáticos com @PrePersist/@PreUpdate**
```java
// ✅ CORRETO
@PrePersist
protected void onCreate() {
    dataCriacao = LocalDateTime.now();
}
```

#### **10. ✅ Exceções de negócio devem estender NegocioException ou RecursoNaoEncontradoException**
```java
// ✅ CORRETO
throw new RecursoNaoEncontradoException("Pagamento não encontrado: " + id);
throw new NegocioException("Pagamento já foi estornado");
```

---

### **⚠️ Princípio KISS: Evite Complexidade Desnecessária**

**Mantra**: *"O código que não existe é o código que não tem bugs"*

#### **❌ Anti-Pattern: Over-Engineering**

**Exemplo Real do Projeto** (o que NÃO fazer):

```java
// ❌ ERRADO: Criar annotation customizada desnecessária
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValorPositivoValidator.class)
public @interface ValorPositivo {
    String message() default "Valor deve ser positivo";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

public class ValorPositivoValidator implements ConstraintValidator<ValorPositivo, BigDecimal> {
    @Override
    public boolean isValid(BigDecimal valor, ConstraintValidatorContext context) {
        return valor != null && valor.compareTo(BigDecimal.ZERO) > 0;
    }
}

// Uso na classe
public class PagamentoRequestDTO {
    @ValorPositivo  // ❌ Annotation customizada desnecessária!
    private BigDecimal valor;
}

// ✅ CORRETO: Usar Bean Validation padrão
public class PagamentoRequestDTO {
    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0.01", message = "Valor mínimo é R$ 0,01")
    private BigDecimal valor;  // ✅ Resolve o mesmo problema com código padrão!
}
```

**Por que está errado?**
- ❌ Criou 15+ linhas de código customizado
- ❌ Mais código para manter e testar
- ❌ Outros desenvolvedores precisam aprender sua API customizada
- ✅ Bean Validation já resolve isso em 1 linha

#### **Exemplos de Simplicidade vs Complexidade**

**1. Validação de CPF**

```java
// ❌ COMPLEXO: Criar annotation customizada
@Cpf
private String cpf;

// ⚠️ ACEITÁVEL: Se realmente usado em muitos lugares
@Pattern(regexp = "\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}", 
         message = "CPF inválido")
private String cpf;

// ✅ MAIS SIMPLES: Validar no Service (se usado em 1-2 lugares)
public void criar(PagamentoRequestDTO dto) {
    if (!validarCpf(dto.getCpf())) {
        throw new NegocioException("CPF inválido");
    }
}
```

**2. Formatação de Datas**

```java
// ❌ COMPLEXO: Criar classe DateFormatter customizada
public class CustomDateFormatter {
    public static String format(LocalDateTime date, String pattern) { ... }
}

// ✅ SIMPLES: Usar Java padrão
LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);
```

**3. Conversão de Entidade → DTO**

```java
// ❌ COMPLEXO: Usar MapStruct para 3 campos
@Mapper
public interface PagamentoMapper {
    PagamentoResponseDTO toDto(Pagamento entity);
}

// ✅ SIMPLES: Método manual para casos triviais
public static PagamentoResponseDTO toDto(Pagamento entity) {
    return PagamentoResponseDTO.builder()
        .id(entity.getId())
        .valor(entity.getValor())
        .status(entity.getStatus())
        .build();
}

// ⚠️ MapStruct é útil quando há MUITOS campos (15+) ou lógica complexa
```

#### **Checklist: "Preciso Mesmo Criar Isso?"**

Antes de criar qualquer abstração customizada, pergunte:

- [ ] **Já existe no Spring/Java?** (99% das vezes, sim)
- [ ] **Bean Validation resolve?** (`@NotNull`, `@Size`, `@Pattern`, `@DecimalMin`, etc)
- [ ] **Será usado em 3+ lugares?** (Se não, não crie abstração)
- [ ] **Alguém da equipe vai entender isso facilmente?** (Se não, simplifique)
- [ ] **Posso resolver com 1 linha de código padrão?** (Se sim, não crie classe customizada)

#### **Regra dos 3 Usos**

> **Só crie abstração customizada após usar a mesma lógica em 3 lugares diferentes.**

```java
// 1º uso: Copie e cole (sim, é OK inicialmente)
if (valor.compareTo(BigDecimal.ZERO) <= 0) { ... }

// 2º uso: Ainda copie e cole
if (valor.compareTo(BigDecimal.ZERO) <= 0) { ... }

// 3º uso: AGORA extraia para método/classe
private boolean valorInvalido(BigDecimal valor) {
    return valor.compareTo(BigDecimal.ZERO) <= 0;
}
```

**Benefícios**:
- ✅ Evita abstrações prematuras
- ✅ Só cria quando há necessidade real
- ✅ Menos código = menos bugs

---

### **Checklist de Code Review**

Ao revisar um Pull Request, verifique:

**Modularização:**
- [ ] Nenhum `import` de classes `domain` de outros módulos?
- [ ] Migrations Flyway sem FKs entre módulos?
- [ ] Controller retorna DTOs (não entidades)?

**Transações e Eventos:**
- [ ] Service tem `@Transactional` onde necessário?
- [ ] Eventos de domínio publicados via Outbox?

**Validação e DTOs:**
- [ ] DTOs têm validações (`@Valid`, `@NotNull`, etc)?
- [ ] Validações usam **Bean Validation padrão** em vez de annotations customizadas?
- [ ] Mapper converte corretamente Entity ↔ DTO?

**Simplicidade (KISS):**
- [ ] Código é simples e direto? (Evita "código inteligente demais")
- [ ] Usa recursos nativos do Spring/Java antes de criar código customizado?
- [ ] Se criou abstração customizada, ela é usada em 3+ lugares?
- [ ] Não há classes/annotations/helpers desnecessários?

**Qualidade:**
- [ ] Testes unitários e de integração criados?
- [ ] Logs com nível adequado (INFO, WARN, ERROR)?
- [ ] Tratamento de exceções adequado?
- [ ] Código é legível para qualquer dev Java (sem "magia")?

---

#### **4. Microservices Patterns**
- **Transactional Outbox**: Garantia de consistência entre DB e Kafka
- **Idempotency**: Prevenção de duplicação de transações
- **Circuit Breaker**: Proteção contra falhas em cascata (adquirente)
- **Distributed Lock**: Controle de concorrência em estornos

---

## � **Comunicação Entre Módulos**

### **Regras de Comunicação**

Para garantir que o monolito possa ser facilmente decomposto em microserviços, **módulos NÃO devem se acoplar diretamente**.

#### **✅ Comunicação PERMITIDA**

**1. Via DTOs (Síncrona)**
```java
// CORRETO: Módulo Pagamento chama Módulo Adquirente via DTO
@Service
public class PagamentoService {
    private final AdquirenteService adquirenteService;  // OK: Service de outro módulo
    
    public PagamentoResponseDTO criar(PagamentoRequestDTO dto) {
        // Criar DTO de request
        AutorizacaoRequest request = new AutorizacaoRequest(...);  // DTO público
        
        // Chamar outro módulo
        AutorizacaoResponse response = adquirenteService.autorizarPagamento(request);
        
        // Usar DTO de response
        if (response.autorizado()) {
            // ...
        }
    }
}
```

**2. Via Eventos (Assíncrona - PREFERIDA)**
```java
// MELHOR: Comunicação via eventos de domínio
@Service
public class PagamentoService {
    private final OutboxService outboxService;
    
    public void criar(PagamentoRequestDTO dto) {
        Pagamento pagamento = repository.save(...);
        
        // Publicar evento - outros módulos podem consumir via Kafka
        PagamentoCriadoEvento evento = new PagamentoCriadoEvento(pagamento);
        outboxService.salvar("Pagamento", pagamento.getId(), evento);
    }
}

// Em outro módulo (ou microserviço futuro)
@Service
public class NotificacaoService {
    
    @KafkaListener(topics = "pagamentos")
    public void onPagamentoCriado(PagamentoCriadoEvento evento) {
        // Processar assincronamente
        enviarEmail(evento.getDados().getId());
    }
}
```

#### **❌ Comunicação PROIBIDA**

**1. Compartilhar Entidades JPA**
```java
// ❌ ERRADO: Estorno importando entidade Pagamento
package br.com.sicredi.toolschallenge.estorno.service;

import br.com.sicredi.toolschallenge.pagamento.domain.Pagamento;  // ❌ NUNCA!

@Service
public class EstornoService {
    private final PagamentoRepository pagamentoRepository;  // ❌ NUNCA!
    
    public void criar(EstornoRequestDTO dto) {
        Pagamento pagamento = pagamentoRepository.findById(...);  // ❌ ACOPLAMENTO!
    }
}
```

**Por quê?** Se `Pagamento` virar um microserviço, não teremos acesso à entidade JPA.

**2. Foreign Keys Entre Módulos**
```sql
-- ❌ ERRADO: FK entre tabelas de módulos diferentes
CREATE TABLE estorno (
    id BIGSERIAL PRIMARY KEY,
    pagamento_id BIGINT NOT NULL,
    FOREIGN KEY (pagamento_id) REFERENCES pagamento(id)  -- ❌ NUNCA!
);

-- ✅ CORRETO: Apenas a coluna, sem FK
CREATE TABLE estorno (
    id BIGSERIAL PRIMARY KEY,
    pagamento_id BIGINT NOT NULL  -- ✅ Apenas referência lógica
);
```

**Por quê?** Em microserviços, `pagamento` e `estorno` estarão em bancos diferentes.

### **Padrões de Integração**

| Cenário | Padrão | Exemplo |
|---------|--------|---------|
| **Leitura de dados de outro módulo** | ❌ Evitar / ✅ Usar eventos | Estorno precisa validar Pagamento → Evento `PagamentoCriadoEvento` já tem os dados |
| **Ação em outro módulo (síncrona)** | ✅ Service + DTO | Pagamento autoriza com Adquirente → `adquirenteService.autorizarPagamento(dto)` |
| **Notificar outro módulo** | ✅ Evento de domínio | Pagamento criado → Publica `PagamentoCriadoEvento` no Kafka |
| **Validação de regra de negócio** | ✅ Dentro do próprio módulo | Validar se pagamento existe → Fazer dentro do `EstornoService` via chamada HTTP futura |

### **Migração para Microserviços**

Com essa estrutura, a migração é simples:

**ANTES (Monolito)**:
```java
@Service
public class PagamentoService {
    private final AdquirenteService adquirenteService;  // In-process call
    
    public void criar(...) {
        AutorizacaoResponse response = adquirenteService.autorizarPagamento(request);
    }
}
```

**DEPOIS (Microserviços)**:
```java
@Service
public class PagamentoService {
    private final AdquirenteClient adquirenteClient;  // HTTP client (Feign/RestTemplate)
    
    public void criar(...) {
        AutorizacaoResponse response = adquirenteClient.autorizarPagamento(request);
        // Mesma interface, mesmos DTOs!
    }
}
```

**Mudança mínima**: Trocar `@Autowired AdquirenteService` por `@Autowired AdquirenteClient`.

---

## �📁 **Estrutura de Pastas**

```
ToolsChallenge/
│
├── .github/
│   └── instructions/
│       └── instructions.md          # 📄 Esta documentação
│
├── docker/
│   ├── postgres/init.sql            # Scripts iniciais do PostgreSQL
│   ├── kafka/                       # Configurações do Kafka
│   └── redis/                       # Configurações do Redis
│
├── docs/
│   ├── AUDITORIA.md                 # Documentação do sistema de auditoria
│   ├── LOCK_DISTRIBUIDO.md          # Implementação de locks distribuídos
│   ├── TESTES_IDEMPOTENCIA.md       # Testes de idempotência
│   └── TESTES_OUTBOX_PATTERN.md     # Testes do Outbox Pattern
│
├── src/
│   ├── main/
│   │   ├── java/br/com/sicredi/toolschallenge/
│   │   │   │
│   │   │   ├── adquirente/                    # 🏦 Módulo Adquirente (Resilience4j)
│   │   │   │   ├── dto/
│   │   │   │   │   ├── AutorizacaoRequest.java
│   │   │   │   │   └── AutorizacaoResponse.java
│   │   │   │   ├── exception/
│   │   │   │   │   └── AdquirenteIndisponivelException.java
│   │   │   │   └── service/
│   │   │   │       ├── AdquirenteService.java          # Circuit Breaker + Retry + Bulkhead
│   │   │   │       └── AdquirenteSimuladoService.java  # Simulador com chaos engineering
│   │   │   │
│   │   │   ├── pagamento/                     # 💳 Módulo Pagamento
│   │   │   │   ├── controller/
│   │   │   │   │   └── PagamentoController.java        # Endpoints REST
│   │   │   │   ├── domain/
│   │   │   │   │   ├── Pagamento.java                  # Entidade JPA
│   │   │   │   │   ├── StatusPagamento.java            # Enum (PENDENTE, PROCESSADO, ERRO)
│   │   │   │   │   └── TipoPagamento.java              # Enum (PIX, CARTAO_CREDITO, etc)
│   │   │   │   ├── dto/
│   │   │   │   │   ├── PagamentoRequestDTO.java
│   │   │   │   │   ├── PagamentoResponseDTO.java
│   │   │   │   │   └── PagamentoMapper.java            # MapStruct mapper
│   │   │   │   ├── events/
│   │   │   │   │   ├── PagamentoCriadoEvento.java
│   │   │   │   │   └── PagamentoStatusAlteradoEvento.java
│   │   │   │   ├── repository/
│   │   │   │   │   └── PagamentoRepository.java        # Spring Data JPA
│   │   │   │   └── service/
│   │   │   │       └── PagamentoService.java           # Lógica de negócio
│   │   │   │
│   │   │   ├── estorno/                       # 🔄 Módulo Estorno
│   │   │   │   ├── controller/
│   │   │   │   │   └── EstornoController.java
│   │   │   │   ├── domain/
│   │   │   │   │   ├── Estorno.java
│   │   │   │   │   └── StatusEstorno.java
│   │   │   │   ├── dto/
│   │   │   │   │   ├── EstornoRequestDTO.java
│   │   │   │   │   ├── EstornoResponseDTO.java
│   │   │   │   │   └── EstornoMapper.java
│   │   │   │   ├── events/
│   │   │   │   │   ├── EstornoCriadoEvento.java
│   │   │   │   │   └── EstornoStatusAlteradoEvento.java
│   │   │   │   ├── repository/
│   │   │   │   │   └── EstornoRepository.java
│   │   │   │   └── service/
│   │   │   │       └── EstornoService.java             # Usa Lock Distribuído
│   │   │   │
│   │   │   ├── infra/                         # 🛠️ Infraestrutura (Cross-cutting)
│   │   │   │   │
│   │   │   │   ├── idempotencia/              # Idempotência
│   │   │   │   │   ├── annotation/
│   │   │   │   │   │   └── Idempotente.java            # @Idempotente anotação
│   │   │   │   │   ├── interceptor/
│   │   │   │   │   │   ├── IdempotenciaInterceptor.java
│   │   │   │   │   │   └── IdempotenciaResponseAdvice.java
│   │   │   │   │   ├── job/
│   │   │   │   │   │   └── IdempotenciaLimpezaJob.java # Limpeza automática
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   └── IdempotenciaRepository.java
│   │   │   │   │   ├── service/
│   │   │   │   │   │   └── IdempotenciaService.java
│   │   │   │   │   └── Idempotencia.java               # Entidade
│   │   │   │   │
│   │   │   │   ├── auditoria/                 # Auditoria de Eventos
│   │   │   │   │   ├── listener/
│   │   │   │   │   │   ├── PagamentoEventListener.java # Kafka listener
│   │   │   │   │   │   └── EstornoEventListener.java
│   │   │   │   │   ├── repository/
│   │   │   │   │   │   └── EventoAuditoriaRepository.java
│   │   │   │   │   ├── scheduled/
│   │   │   │   │   │   └── AuditoriaScheduler.java     # Jobs agendados
│   │   │   │   │   ├── service/
│   │   │   │   │   │   └── AuditoriaService.java
│   │   │   │   │   └── EventoAuditoria.java            # Entidade
│   │   │   │   │
│   │   │   │   └── outbox/                    # Outbox Pattern
│   │   │   │       ├── publisher/
│   │   │   │       │   └── OutboxPublisher.java        # Job que publica eventos
│   │   │   │       ├── repository/
│   │   │   │       │   └── OutboxRepository.java
│   │   │   │       ├── service/
│   │   │   │       │   └── OutboxService.java
│   │   │   │       └── OutboxEvento.java               # Entidade transacional
│   │   │   │
│   │   │   ├── shared/                        # 🔧 Compartilhado
│   │   │   │   ├── config/
│   │   │   │   │   ├── KafkaConfig.java                # Configuração Kafka
│   │   │   │   │   ├── RedisConfig.java                # Configuração Redis
│   │   │   │   │   ├── RedissonConfig.java             # Redisson (Locks)
│   │   │   │   │   └── IdempotenciaConfig.java         # Registra interceptors
│   │   │   │   └── exception/
│   │   │   │       ├── GlobalExceptionHandler.java     # Exception handler global
│   │   │   │       ├── NegocioException.java
│   │   │   │       ├── RecursoNaoEncontradoException.java
│   │   │   │       └── ErroResposta.java               # DTO de erro
│   │   │   │
│   │   │   ├── security/                      # 🔐 Segurança (futura)
│   │   │   │   └── (placeholder para autenticação)
│   │   │   │
│   │   │   └── ToolschallengeApplication.java # 🚀 Main class
│   │   │
│   │   └── resources/
│   │       ├── application.yml                # Configuração principal
│   │       └── db/migration/                  # Flyway migrations
│   │           ├── V1__criar_tabela_pagamento.sql
│   │           ├── V2__criar_tabela_estorno.sql
│   │           ├── V3__criar_tabela_idempotencia.sql
│   │           ├── V4__criar_tabela_outbox.sql
│   │           └── V5__criar_tabela_auditoria.sql
│   │
│   └── test/
│       └── java/br/com/sicredi/toolschallenge/
│           ├── integration/                   # Testes de integração
│           └── unit/                          # Testes unitários
│
├── docker-compose.yml                         # Infraestrutura local
├── pom.xml                                    # Maven dependencies
├── README.md                                  # Quickstart
├── EXEMPLOS_API_PAGAMENTO.md                 # Exemplos de uso
├── EXEMPLOS_API_ESTORNO.md
└── QUICKSTART.md
```

---

## 🛠️ **Stack Tecnológico**

### **Backend**
| Tecnologia | Versão | Propósito |
|-----------|--------|-----------|
| **Java** | 17 | Linguagem base |
| **Spring Boot** | 3.5.7 | Framework principal |
| **Spring Data JPA** | 3.5.7 | Persistência ORM |
| **Spring Kafka** | 3.5.7 | Mensageria |
| **Spring Actuator** | 3.5.7 | Monitoramento |

### **Persistência**
| Tecnologia | Versão | Propósito |
|-----------|--------|-----------|
| **PostgreSQL** | 16 | Banco de dados principal |
| **Flyway** | 10.x | Migrações de schema |
| **Redis** | 7.x | Cache e locks distribuídos |

### **Mensageria**
| Tecnologia | Versão | Propósito |
|-----------|--------|-----------|
| **Apache Kafka** | 3.6.x | Event streaming |
| **Spring Kafka** | 3.5.7 | Integração com Kafka |

### **Resiliência**
| Tecnologia | Versão | Propósito |
|-----------|--------|-----------|
| **Resilience4j** | 2.2.0 | Circuit Breaker, Retry, Bulkhead |
| **Redisson** | 3.35.0 | Locks distribuídos |

### **Observabilidade**
| Tecnologia | Versão | Propósito |
|-----------|--------|-----------|
| **Micrometer** | 1.13.x | Métricas |
| **Prometheus** | 2.x | Coleta de métricas |
| **Springdoc OpenAPI** | 2.6.0 | Documentação Swagger |

### **Build e Testes**
| Tecnologia | Versão | Propósito |
|-----------|--------|-----------|
| **Maven** | 3.9.x | Build tool |
| **JUnit 5** | 5.10.x | Testes unitários |
| **Testcontainers** | 1.19.x | Testes de integração |
| **Lombok** | 1.18.x | Redução de boilerplate |

---

## 🎨 **Padrões Implementados**

### **1. Idempotência**
**Objetivo**: Garantir que requisições duplicadas não causem efeitos colaterais.

**Implementação**:
- Header `Idempotency-Key` obrigatório em `POST` e `PUT`
- Armazenamento em Redis com TTL de 24h
- Retorno de `409 Conflict` para duplicatas
- Limpeza automática via job agendado

**Exemplo**:
```bash
curl -X POST http://localhost:8080/pagamentos \
  -H "Idempotency-Key: 550e8400-e29b-41d4-a716-446655440000" \
  -H "Content-Type: application/json" \
  -d '{...}'
```

### **2. Outbox Pattern**
**Objetivo**: Garantir consistência entre banco de dados e Kafka.

**Fluxo**:
1. Transação SQL salva `Pagamento` + `OutboxEvento` na mesma transação
2. Job assíncrono (`OutboxPublisher`) publica eventos pendentes no Kafka
3. Marca eventos como `PUBLICADO` após confirmação

**Benefícios**:
- ✅ At-least-once delivery garantido
- ✅ Sem perda de eventos mesmo em crash
- ✅ Eventual consistency

### **3. Distributed Lock**
**Objetivo**: Prevenir race conditions em operações concorrentes.

**Implementação**:
- Redisson sobre Redis
- Lock pattern: `tryLock(5s wait, 30s lease)`
- Watchdog automático renova lease
- Graceful degradation se Redis indisponível

**Uso**:
```java
@Service
public class EstornoService {
    
    @Autowired(required = false)
    private RedissonClient redissonClient;
    
    public void processarEstorno(Long pagamentoId) {
        String lockKey = "estorno:pagamento:" + pagamentoId;
        RLock lock = redissonClient.getLock(lockKey);
        
        if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
            try {
                // Lógica crítica protegida
            } finally {
                lock.unlock();
            }
        }
    }
}
```

### **4. Circuit Breaker (Resilience4j)**
**Objetivo**: Proteger sistema de falhas em cascata ao chamar adquirente.

**Configuração**:
- **Threshold**: 50% de falhas abre circuito
- **Wait Duration**: 10s em estado OPEN
- **Half-Open**: 3 chamadas de teste
- **Fallback**: Retorna status `PENDENTE` para reprocessamento

**Estados**:
```
CLOSED → OPEN (50% failures) → HALF_OPEN (10s) → CLOSED (3/3 success)
                                              ↘ OPEN (1+ failure)
```

### **5. Event Sourcing (Auditoria)**
**Objetivo**: Rastreabilidade completa de eventos de negócio.

**Eventos Capturados**:
- `PagamentoCriadoEvento`
- `PagamentoStatusAlteradoEvento`
- `EstornoCriadoEvento`
- `EstornoStatusAlteradoEvento`

**Armazenamento**:
- Tabela `evento_auditoria` com JSON completo do evento
- Listeners Kafka processam e persistem assíncronamente

---

## 🧩 **Módulos e Funcionalidades**

### **1. Módulo Pagamento** (`pagamento/`)

**Responsabilidades**:
- Receber requisições de pagamento
- Validar dados de entrada
- Autorizar com adquirente (via `AdquirenteService`)
- Persistir transação
- Publicar eventos via Outbox

**Endpoints**:
- `POST /pagamentos` - Criar pagamento (idempotente)
- `GET /pagamentos/{id}` - Consultar pagamento
- `GET /pagamentos` - Listar todos (paginado)

**Regras de Negócio**:
- Valor mínimo: R$ 0,01
- Descrição obrigatória
- Tipo de pagamento validado (PIX, CARTAO_CREDITO, BOLETO)
- Geração automática de NSU e código de autorização

### **2. Módulo Estorno** (`estorno/`)

**Responsabilidades**:
- Processar estornos de pagamentos
- Validar elegibilidade (status PROCESSADO)
- Prevenir duplicação com lock distribuído
- Atualizar status de pagamento

**Endpoints**:
- `POST /pagamentos/{id}/estornos` - Solicitar estorno (idempotente)
- `GET /pagamentos/{id}/estornos` - Listar estornos do pagamento
- `GET /estornos/{id}` - Consultar estorno específico

**Regras de Negócio**:
- Apenas pagamentos `PROCESSADO` podem ser estornados
- Estorno total (valor integral)
- Lock distribuído previne estornos duplicados concorrentes
- Um pagamento pode ter múltiplos estornos (se falhou)

### **3. Módulo Adquirente** (`adquirente/`)

**Responsabilidades**:
- Simular comunicação com adquirente externo
- Aplicar resiliência (Circuit Breaker, Retry, Bulkhead)
- Chaos engineering configurável

**Componentes**:
- `AdquirenteService`: Orquestra resiliência
- `AdquirenteSimuladoService`: Mock com taxa de falhas configurável

**Configuração Chaos** (application.yml):
```yaml
adquirente:
  simulado:
    failure-rate: 0.2      # 20% de falhas
    latency-ms: 100        # 100ms de latência artificial
    timeout-rate: 0.1      # 10% de timeouts
    aprovacao-rate: 0.9    # 90% de aprovações
```

### **4. Infraestrutura** (`infra/`)

#### **4.1. Idempotência**
- Interceptor automático em métodos anotados com `@Idempotente`
- Armazenamento Redis com estrutura:
  ```json
  {
    "chave": "550e8400-...",
    "resposta": "{...}",
    "statusCode": 201,
    "timestamp": "2025-11-02T10:30:00Z"
  }
  ```

#### **4.2. Auditoria**
- Listeners Kafka consomem eventos de domínio
- Persistem em `evento_auditoria` com:
  - Tipo de evento
  - Agregado (pagamento_id, estorno_id)
  - Payload JSON completo
  - Timestamp

#### **4.3. Outbox**
- `OutboxService.salvar()` persiste eventos transacionalmente
- `OutboxPublisher` (job @Scheduled) publica pendentes
- Retry automático em falhas de publicação

---

## 🗄️ **Banco de Dados**

### **Schema PostgreSQL**

#### **Tabela: `pagamento`**
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

#### **Tabela: `estorno`**
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

#### **Tabela: `idempotencia`**
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

#### **Tabela: `outbox_evento`**
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

#### **Tabela: `evento_auditoria`**
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

### **Flyway Migrations**

Migrações localizadas em `src/main/resources/db/migration/`:

1. **V1**: Criar tabela `pagamento`
2. **V2**: Criar tabela `estorno`
3. **V3**: Criar tabela `idempotencia`
4. **V4**: Criar tabela `outbox_evento`
5. **V5**: Criar tabela `evento_auditoria`

**Execução**: Automática no startup via `spring.flyway.enabled=true`

---

## 📨 **Mensageria (Kafka)**

### **Tópicos Kafka**

| Tópico | Eventos | Consumidores |
|--------|---------|--------------|
| `pagamentos` | `PagamentoCriadoEvento`, `PagamentoStatusAlteradoEvento` | `PagamentoEventListener` (Auditoria) |
| `estornos` | `EstornoCriadoEvento`, `EstornoStatusAlteradoEvento` | `EstornoEventListener` (Auditoria) |

### **Estrutura de Evento**

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

### **Configuração Kafka**

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

## 🔴 **Cache e Locks Distribuídos**

### **Redis - Idempotência**

**TTL**: 24 horas  
**Estrutura de Chave**: `idempotencia:{UUID}`

```redis
SET idempotencia:550e8400-e29b-41d4-a716-446655440000 
    '{"resposta":"{...}","statusCode":201,"timestamp":"..."}'
    EX 86400
```

### **Redisson - Locks Distribuídos**

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
    // Tenta adquirir lock: 5s wait, 30s lease
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

## 🛡️ **Resiliência (Resilience4j)**

### **Circuit Breaker**

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

**Uso**:
```java
@CircuitBreaker(name = "adquirente", fallbackMethod = "autorizarPagamentoFallback")
public AutorizacaoResponse autorizarPagamento(AutorizacaoRequest request) {
    return adquirenteSimulado.autorizarPagamento(request);
}

private AutorizacaoResponse autorizarPagamentoFallback(AutorizacaoRequest request, Exception ex) {
    log.warn("Circuit Breaker OPEN - Fallback ativado");
    return new AutorizacaoResponse(false, null, null); // PENDENTE
}
```

### **Retry**

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

### **Bulkhead (Thread Pool)**

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

**Proteção**: Isola recursos e previne esgotamento de threads da aplicação.

---

## 📊 **Observabilidade**

### **Actuator Endpoints**

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

### **Prometheus Metrics**

**Métricas Principais**:
- `http_server_requests_seconds` - Latência de requisições
- `resilience4j_circuitbreaker_state` - Estado do CB (0=CLOSED, 1=OPEN, 2=HALF_OPEN)
- `resilience4j_circuitbreaker_failure_rate` - Taxa de falhas
- `resilience4j_retry_calls` - Número de retries
- `jvm_memory_used_bytes` - Uso de memória
- `hikaricp_connections_active` - Conexões DB ativas

### **Swagger UI**

**URL**: `http://localhost:8080/swagger-ui.html`

Documentação interativa de todas as APIs com:
- Schemas de request/response
- Validações
- Códigos de erro
- Exemplos de uso

---

## 🔐 **Segurança**

### **Implementado**
- ✅ Validação de entrada com `@Valid`
- ✅ Exception handling global
- ✅ Sanitização de logs (mascaramento de cartões)
- ✅ CORS configurado (em desenvolvimento: `*`)

### **TODO (Roadmap)**
- ⏳ Autenticação JWT
- ⏳ Rate limiting
- ⏳ HTTPS obrigatório
- ⏳ Criptografia de dados sensíveis

---

## 🌐 **APIs e Endpoints**

### **Pagamentos**

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

**Response 200**:
```json
{
  "id": 123,
  "descricao": "Compra na Loja X",
  "valor": 150.50,
  "status": "PROCESSADO",
  ...
}
```

### **Estornos**

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

**Response 200**:
```json
[
  {
    "id": 456,
    "pagamentoId": 123,
    "valor": 150.50,
    "status": "PROCESSADO",
    ...
  }
]
```

### **Códigos de Erro**

| Código | Descrição |
|--------|-----------|
| `400 Bad Request` | Validação falhou |
| `404 Not Found` | Recurso não encontrado |
| `409 Conflict` | Chave idempotente duplicada |
| `422 Unprocessable Entity` | Regra de negócio violada |
| `500 Internal Server Error` | Erro inesperado |
| `503 Service Unavailable` | Circuit Breaker OPEN |

---

## ⚙️ **Configuração e Ambiente**

### **Pré-requisitos**
- Java 17+
- Docker e Docker Compose
- Maven 3.9+

### **Variáveis de Ambiente**

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

### **Iniciar Infraestrutura**

```bash
# Subir PostgreSQL, Redis e Kafka
docker-compose up -d

# Verificar status
docker-compose ps

# Ver logs
docker-compose logs -f
```

### **Compilar e Executar**

```bash
# Compilar
mvn clean package

# Executar
mvn spring-boot:run

# Ou via JAR
java -jar target/toolschallenge-0.0.1-SNAPSHOT.jar
```

### **Acessar Serviços**

- **API**: http://localhost:8080
- **Swagger**: http://localhost:8080/swagger-ui.html
- **Actuator**: http://localhost:8080/atuador
- **Prometheus Metrics**: http://localhost:8080/atuador/prometheus

---

## 🧪 **Testes**

### **Estrutura de Testes**

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

### **Testcontainers**

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

### **Executar Testes**

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

---

## 🚀 **Deploy e CI/CD**

### **TODO - Pipeline GitHub Actions**

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

### **Dockerfile**

```dockerfile
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

## 📈 **Monitoramento**

### **Stack Proposta**

```
Aplicação → Micrometer → Prometheus → Grafana
                            ↓
                         Alertmanager
```

### **Dashboards Grafana**

**Painéis Principais**:
1. **HTTP Metrics**: Latência, throughput, erros por endpoint
2. **Circuit Breaker**: Estado, taxa de falhas, fallbacks
3. **Database**: Conexões ativas, latência de queries
4. **JVM**: Memory, GC, threads
5. **Kafka**: Offset lag, mensagens/s

### **Alertas Propostos**

| Alerta | Condição | Severidade |
|--------|----------|------------|
| Circuit Breaker OPEN | Estado = OPEN por > 1min | Critical |
| Alta Taxa de Erro | 5xx > 5% por 5min | High |
| Latência Alta | p95 > 1s por 5min | Medium |
| Database Pool Cheio | Connections = max por 2min | High |

---

## 🐛 **Troubleshooting**

### **Problema: 409 Conflict em requisição nova**

**Causa**: Chave idempotente duplicada ou não expirada no Redis.

**Solução**:
```bash
# Limpar chave específica
redis-cli -a redis123 DEL "idempotencia:550e8400-..."

# Limpar todas (CUIDADO!)
redis-cli -a redis123 FLUSHDB
```

### **Problema: Circuit Breaker sempre OPEN**

**Causa**: Taxa de falhas do adquirente simulado muito alta.

**Solução**: Reduzir `failure-rate` em `application.yml`:
```yaml
adquirente:
  simulado:
    failure-rate: 0.1  # 10% em vez de 20%
```

### **Problema: Eventos não chegam no Kafka**

**Verificações**:
1. Kafka rodando: `docker-compose ps kafka`
2. Tópico existe: `docker exec -it kafka kafka-topics --list --bootstrap-server localhost:9092`
3. Outbox pendente: `SELECT * FROM outbox_evento WHERE status = 'PENDENTE';`
4. Logs do `OutboxPublisher`: Procurar por erros

### **Problema: Lock Distribuído não funciona**

**Verificações**:
1. Redis rodando: `redis-cli -a redis123 ping`
2. RedissonClient injetado: Verificar logs de startup
3. Lock key correto: `redis-cli -a redis123 KEYS "estorno:*"`

---

## 🗺️ **Roadmap**

### **Fase 1 — Fundação** ✅
- [x] Setup Spring Boot
- [x] PostgreSQL + Flyway
- [x] Pagamentos CRUD
- [x] Estornos CRUD

### **Fase 2 — Event-Driven** ✅
- [x] Kafka configurado
- [x] Outbox Pattern
- [x] Auditoria de eventos

### **Fase 3 — Confiabilidade** ✅
- [x] Idempotência com Redis
- [x] Lock Distribuído (Redisson)
- [x] Testes de concorrência

### **Fase 4 — Resiliência** 🔄 (50% completo)
- [x] Resilience4j configurado
- [x] Circuit Breaker + Retry + Bulkhead
- [x] Adquirente Simulado com Chaos
- [ ] Integração completa
- [ ] Scheduler de reprocessamento PENDENTE
- [ ] Testes de resiliência

### **Fase 5 — Observabilidade** ⏳
- [ ] Prometheus + Grafana
- [ ] Dashboards customizados
- [ ] Alertas configurados
- [ ] Distributed Tracing (Sleuth + Zipkin)

### **Fase 6 — Segurança** ⏳
- [ ] Autenticação JWT
- [ ] Rate Limiting (Bucket4j)
- [ ] HTTPS obrigatório
- [ ] Vault para secrets

### **Fase 7 — Produção** ⏳
- [ ] CI/CD Pipeline (GitHub Actions)
- [ ] Kubernetes manifests
- [ ] Terraform infra
- [ ] Load tests (JMeter/Gatling)

---

## ❓ **Perguntas Frequentes (FAQ) - Monolito Modular**

### **1. Por que não começar direto com microserviços?**

**Resposta**: Microserviços trazem complexidade operacional desde o dia 1:
- Deploy de N serviços independentes
- Service discovery, API Gateway, Load Balancer
- Distributed tracing, logging centralizado
- Latência de rede entre serviços
- Transações distribuídas (complexas)

**Monolito Modular permite**:
- ✅ Começar simples (1 deploy)
- ✅ Evoluir a arquitetura conforme necessidade
- ✅ Migrar módulos específicos quando justificável (ex: escala, times independentes)

### **2. Como saber quando migrar um módulo para microserviço?**

**Sinais de que está na hora**:
- 🔥 Módulo tem carga muito maior que outros (necessita escala independente)
- 👥 Time cresceu e precisa de autonomia de deploy
- 🚀 Tecnologia diferente seria melhor (ex: módulo de ML em Python)
- 🌍 Necessidade de deploy em regiões diferentes

**Não migre se**:
- ❌ Módulo tem baixa carga
- ❌ Comunicação é muito frequente com outros módulos (latência de rede prejudicaria)
- ❌ Time é pequeno e consegue gerenciar o monólito

### **3. Posso ter transações entre módulos?**

**No monolito**: ✅ Sim, `@Transactional` funciona entre módulos (mesma JVM).

**Em microserviços**: ❌ Não, cada serviço tem seu próprio banco.

**Solução**: Use **Saga Pattern** ou **Outbox Pattern**:
```java
// Módulo Pagamento
@Transactional
public void criar() {
    pagamentoRepository.save(pagamento);
    outboxService.salvar(evento);  // Mesmo banco, mesma transação
}

// Kafka entrega para outros módulos/serviços
```

### **4. Como testar a modularização?**

**Teste da "Linha Imaginária"**:

Imagine uma linha dividindo os módulos. Se você consegue responder "sim" para todas:

- [ ] Módulo A funciona sem conhecer implementação de Módulo B?
- [ ] Posso mover Módulo B para outro repositório sem quebrar A?
- [ ] A comunicação entre A e B é apenas via DTOs ou eventos?
- [ ] Não há FKs de A para B no banco de dados?

Se alguma resposta for "não", **há acoplamento** que precisa ser removido.

### **5. Shared/Infra não vai gerar acoplamento?**

**Resposta**: Apenas se mal usado.

**✅ Correto**: `shared/` tem apenas **utilitários genéricos**:
- Configurações (Kafka, Redis)
- Exception handlers
- Annotations (`@Idempotente`)
- DTOs base (se necessário)

**❌ Errado**: `shared/` **NÃO** deve ter:
- Lógica de negócio específica de um domínio
- Entidades JPA compartilhadas
- Services que orquestram múltiplos módulos

**Regra**: Se `shared/` tiver conhecimento de negócio de `pagamento/`, está errado.

### **6. Como lidar com consultas que precisam de dados de múltiplos módulos?**

**Opção 1: Backend for Frontend (BFF)**
```java
@Service
public class PagamentoComEstornoBFFService {
    private final PagamentoService pagamentoService;
    private final EstornoService estornoService;
    
    public PagamentoComEstornosDTO buscar(Long id) {
        PagamentoDTO pag = pagamentoService.buscar(id);
        List<EstornoDTO> estornos = estornoService.buscarPorPagamento(id);
        return new PagamentoComEstornosDTO(pag, estornos);  // Agrega
    }
}
```

**Opção 2: CQRS com Read Model**
- Write: Cada módulo escreve em sua tabela
- Read: View materializada com JOIN (ou denormalizada)

**Opção 3: GraphQL Federation** (futuro, em microserviços)

### **7. E se eu precisar fazer rollback de um módulo?**

**No monolito**: Rollback completo (volta versão do JAR).

**Em microserviços**: Rollback apenas do serviço afetado.

**Mitigação**: 
- ✅ Feature Flags (ativar/desativar sem deploy)
- ✅ Blue/Green Deployment
- ✅ Canary Releases (testar com % do tráfego)

### **8. Preciso duplicar código de validação entre módulos?**

**Sim e Não**.

**❌ Não duplique**: Regras genéricas (CPF, email) → `shared/validation/`

**✅ Duplique**: Regras de negócio específicas → Cada módulo tem as suas

**Exemplo**:
```java
// shared/validation/CpfValidator.java (genérico)
public class CpfValidator { }

// pagamento/service/PagamentoService.java (regra de negócio)
if (pagamento.getValor().compareTo(BigDecimal.ZERO) <= 0) {
    throw new NegocioException("Valor deve ser positivo");
}
```

### **9. Como evitar over-engineering (excesso de engenharia)?**

**Sintomas de Over-Engineering**:
- 🚨 Você criou 5 classes para fazer algo que poderia ser 1 método
- 🚨 Você usa palavras como "Factory", "Builder", "Strategy" sem necessidade real
- 🚨 Você criou abstração "para facilitar no futuro" que nunca é usada
- 🚨 Outros devs precisam de 30min para entender seu código "elegante"

**Soluções**:

**1. Siga o Princípio YAGNI** (*You Aren't Gonna Need It*)
```java
// ❌ OVER-ENGINEERING
public interface PagamentoValidatorStrategy { }
public class ValorMinimoValidator implements PagamentoValidatorStrategy { }
public class DescricaoObrigatoriaValidator implements PagamentoValidatorStrategy { }
public class PagamentoValidatorFactory { }

// ✅ SIMPLES: Bean Validation resolve
public class PagamentoRequestDTO {
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal valor;
    
    @NotBlank
    private String descricao;
}
```

**2. Use a "Regra dos 3"**: Só abstraia após 3º uso repetido

**3. Prefira composição simples a herança complexa**
```java
// ❌ COMPLEXO
public abstract class BaseService<T, ID> { }
public abstract class CrudService<T, ID> extends BaseService<T, ID> { }
public class PagamentoService extends CrudService<Pagamento, Long> { }

// ✅ SIMPLES
@Service
public class PagamentoService {
    private final PagamentoRepository repository;
    // Métodos diretos, sem abstração forçada
}
```

**4. Code Review com foco em simplicidade**
- Pergunte: "Consigo explicar isso em 1 frase?"
- Se não: Simplifique

**Lembre-se**: 
> *"Debugging is twice as hard as writing the code. So if you write the code as cleverly as possible, you are, by definition, not smart enough to debug it."* - Brian Kernighan

---

## 📚 **Referências e Documentação Adicional**

### **Documentos Internos**
- [AUDITORIA.md](../../docs/AUDITORIA.md) - Sistema de auditoria
- [LOCK_DISTRIBUIDO.md](../../docs/LOCK_DISTRIBUIDO.md) - Locks distribuídos
- [TESTES_IDEMPOTENCIA.md](../../docs/TESTES_IDEMPOTENCIA.md) - Testes de idempotência
- [TESTES_OUTBOX_PATTERN.md](../../docs/TESTES_OUTBOX_PATTERN.md) - Testes do Outbox
- [EXEMPLOS_API_PAGAMENTO.md](../../EXEMPLOS_API_PAGAMENTO.md) - Exemplos de API
- [EXEMPLOS_API_ESTORNO.md](../../EXEMPLOS_API_ESTORNO.md) - Exemplos de estornos

### **Tecnologias**
- [Spring Boot](https://spring.io/projects/spring-boot)
- [Resilience4j](https://resilience4j.readme.io/)
- [Redisson](https://github.com/redisson/redisson)
- [Apache Kafka](https://kafka.apache.org/)
- [PostgreSQL](https://www.postgresql.org/)

### **Padrões**
- [Outbox Pattern](https://microservices.io/patterns/data/transactional-outbox.html)
- [Circuit Breaker](https://martinfowler.com/bliki/CircuitBreaker.html)
- [Idempotency](https://stripe.com/docs/api/idempotent_requests)
- [Modular Monolith](https://www.kamilgrzybek.com/blog/posts/modular-monolith-primer) - Kamil Grzybek
- [Saga Pattern](https://microservices.io/patterns/data/saga.html) - Transações distribuídas

### **Princípios de Design**
- [KISS Principle](https://en.wikipedia.org/wiki/KISS_principle) - Keep It Simple, Stupid
- [YAGNI](https://martinfowler.com/bliki/Yagni.html) - You Aren't Gonna Need It (Martin Fowler)
- [Occam's Razor](https://fs.blog/occams-razor/) - A solução mais simples é geralmente a melhor

---

## 👥 **Contribuindo**

### **Fluxo de Desenvolvimento**

1. Criar branch feature: `git checkout -b feature/minha-feature`
2. Implementar mudanças
3. Executar testes: `mvn test`
4. Commit: `git commit -m "feat: descrição"`
5. Push: `git push origin feature/minha-feature`
6. Abrir Pull Request

### **Convenções de Commit**

- Sempre inicie a mensagem com "Criado(a) ..." sem muitos detalhes

---

## 📄 **Licença**

Projeto desenvolvido para desafio técnico Sicredi - Uso Interno.

---

**Última Atualização**: 02/11/2025  
**Versão**: 0.0.1-SNAPSHOT  
**Autor**: Equipe ToolsChallenge
