# 📘 **ToolsChallenge - Regras de Desenvolvimento**

> **Documentação Técnica Completa**: Ver [README.md](../../README.md)

## 📋 **Índice**
1. [Visão Geral](#-visão-geral)
2. [Arquitetura Monolito Modular](#-arquitetura-monolito-modular)
3. [Regras Fundamentais](#-regras-fundamentais)
4. [Anatomia de um Módulo](#-anatomia-de-um-módulo)
5. [Pasta shared/](#-pasta-shared)
6. [Comunicação Entre Módulos](#-comunicação-entre-módulos)
7. [Checklist de Novo Módulo](#-checklist-de-novo-módulo)
8. [As 10 Regras de Ouro](#-as-10-regras-de-ouro)
9. [Princípio KISS](#-princípio-kiss)
10. [Regras de TDD](#-regras-de-tdd)
11. [Roadmap](#-roadmap)

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
   - 🔍 **SEMPRE VERIFICAR ANTES DE CRIAR**: Antes de implementar qualquer código ou criar qualquer arquivo, **SEMPRE** verificar se aquele arquivo, classe, método ou código similar já existe no projeto. Use ferramentas de busca (`file_search`, `grep_search`, `semantic_search`) para evitar duplicatas e conflitos de beans no Spring. Esta é a **regra #1** - previne 90% dos problemas de conflito.
   - 🤔 **SEMPRE CONSULTAR QUANDO HOUVER MÚLTIPLAS OPÇÕES**: Ao executar uma tarefa ou pedido que tenha múltiplas libs/tecnologias/formas diferentes de implementar, **NUNCA** escolha automaticamente. Raciocine sobre as opções, considere os próximos passos do projeto, analise prós/contras de cada abordagem (simplicidade, manutenibilidade, over-engineering, compatibilidade com stack atual) e **APRESENTE AS OPÇÕES NO CHAT** para o usuário decidir. Isso previne over-engineering e mantém alinhamento com a visão do projeto.
   - ❌ **NUNCA** fazer `import` direto de classes de domínio de outro módulo
   - ❌ **NUNCA** usar `@Autowired` de `Service` de outro módulo diretamente
   - ❌ **NUNCA** criar abstrações complexas desnecessárias (custom annotations, frameworks internos, etc)
   - ❌ **NUNCA** criar arquivos markdown (.md) para documentar cada interação ou criar scripts de terminal para explicações - Use o chat para isso
   - ✅ **SEMPRE** usar eventos de domínio para comunicação assíncrona
   - ✅ **SEMPRE** usar DTOs para comunicação síncrona (se necessário)
   - ✅ **SEMPRE** preferir simplicidade: use recursos nativos do Spring/Java antes de criar código customizado
   - ✅ **SEMPRE** pensar: "Se esse módulo fosse um microserviço separado, isso funcionaria?"
   - ✅ **SEMPRE** questionar: "Preciso mesmo criar isso ou já existe uma solução padrão?"
   - ✅ **SEMPRE** explicar mudanças via chat, criar documentação markdown apenas quando solicitado explicitamente

4. **Princípio KISS (Keep It Simple, Stupid)**
   - 🎯 **Simplicidade sobre Complexidade**: O código mais fácil de manter é o código simples
   - 🚫 **Evite Over-Engineering**: Não crie abstrações "para o futuro" que podem nunca ser necessárias
   - ✅ **Use o Padrão**: Bean Validation (`@NotNull`, `@Size`) em vez de annotations customizadas
   - ✅ **Use o Framework**: Spring já resolve 90% dos problemas, não reinvente a roda
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

## 🏗️ **Arquitetura Monolito Modular**

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

---

## 🎖️ **Regras Fundamentais**

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

## 📐 **Anatomia de um Módulo**

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

### **Responsabilidades por Camada**

**Controller**:
- ✅ Receber requisições HTTP
- ✅ Validar entrada (`@Valid`)
- ✅ Delegar para Service
- ✅ Retornar status HTTP correto
- ❌ **NUNCA** ter lógica de negócio
- ❌ **NUNCA** acessar Repository diretamente

**Service**:
- ✅ Lógica de negócio e orquestração
- ✅ Gerenciar transações (`@Transactional`)
- ✅ Converter DTOs ↔ Entities
- ✅ Publicar eventos de domínio
- ✅ Comunicar com outros módulos via DTOs
- ❌ **NUNCA** retornar entidades JPA para Controller
- ❌ **NUNCA** receber HttpServletRequest/Response

**Repository**:
- ✅ Abstração de acesso ao banco
- ✅ Queries customizadas (JPQL ou @Query)
- ❌ **NUNCA** ter lógica de negócio

**Domain**:
- ✅ Representar o modelo de domínio
- ✅ Mapeamento JPA
- ✅ Validações de domínio (via métodos de negócio)
- ❌ **NUNCA** ser exposta diretamente na API (usar DTOs)

---

## 🔧 **Pasta `shared/`**

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

### **⚠️ Regra: Localização de Exceptions**

**Princípio**: Exceptions **genéricas** devem estar em `shared/exception/`. Apenas crie exceptions **específicas de módulo** quando houver:

1. ✅ **Lógica de negócio única** do domínio
2. ✅ **Tratamento HTTP diferenciado** específico
3. ✅ **Comportamento customizado** que não se aplica a outros módulos

**Checklist antes de criar exception em módulo**:
- [ ] Esta exception é **específica deste domínio**?
- [ ] Ela tem **lógica de negócio** que não se aplica a outros módulos?
- [ ] O tratamento HTTP é **diferente** das exceptions genéricas?
- [ ] Se virar microserviço, ainda faria sentido tê-la internamente?

Se **todas as respostas forem NÃO**, crie em `shared/exception/`.

---

## 📡 **Comunicação Entre Módulos**

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

## ✅ **Checklist de Novo Módulo**

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

## 🎖️ **As 11 Regras de Ouro**

*(Repetição das Regras Fundamentais para ênfase)*

1. 🚫 Nunca importe entidades JPA de outro módulo
2. 🚫 Nunca crie Foreign Keys entre tabelas de módulos diferentes
3. ✅ Sempre use DTOs para comunicação entre módulos
4. ✅ Sempre publique eventos de domínio para mudanças importantes
5. ✅ Controllers só devem retornar DTOs, nunca entidades
6. ✅ Services devem ser transacionais
7. ✅ Use @Idempotente em todos os endpoints de modificação
8. ✅ Validações de entrada no DTO com Bean Validation
9. ✅ Timestamps automáticos com @PrePersist/@PreUpdate
10. ✅ Exceções de negócio devem estender NegocioException ou RecursoNaoEncontradoException
11. 🧪 **SEMPRE criar teste unitário junto com Service/método - Red-Green-Refactor obrigatório**

---

## 🧪 **Regra #11 DETALHADA: Test-Driven Development Obrigatório**

### **⚠️ WORKFLOW OBRIGATÓRIO ao criar/modificar Services**

Esta é uma das regras mais críticas do projeto. **NUNCA** pode ser violada.

#### **Ao CRIAR um novo Service ou método em Service:**

1. 🔴 **RED - Criar teste que FALHA**
   ```java
   @Test
   void deveProcessarPagamentoComSucesso() {
       // Arrange - preparar dados
       PagamentoRequestDTO request = ...;
       when(repository.save(any())).thenReturn(pagamento);
       
       // Act - executar método
       PagamentoResponseDTO response = service.criar(request);
       
       // Assert - verificar resultado
       assertNotNull(response);
       assertEquals("PROCESSADO", response.getStatus());
   }
   ```
   
2. ▶️ **Executar teste** → Deve FALHAR (método ainda não existe)
   ```bash
   mvn test -Dtest=PagamentoServiceTest
   # DEVE mostrar erro: "método criar() não encontrado"
   ```

3. 🟢 **GREEN - Implementar código mínimo**
   ```java
   @Service
   public class PagamentoService {
       public PagamentoResponseDTO criar(PagamentoRequestDTO dto) {
           // Implementação mínima para passar no teste
       }
   }
   ```

4. ▶️ **Executar teste novamente** → Deve PASSAR
   ```bash
   mvn test -Dtest=PagamentoServiceTest
   # DEVE mostrar: "Tests run: 1, Failures: 0, Errors: 0"
   ```

5. 🔵 **REFACTOR - Melhorar código** (se necessário)
   - Refatorar mantendo testes passando
   - Executar testes após cada mudança

#### **Ao MODIFICAR um método existente em Service:**

1. ▶️ **ANTES de modificar**: Executar TODOS os testes da classe
   ```bash
   mvn test -Dtest=PagamentoServiceTest
   # Garantir que TODOS estão passando
   ```

2. 🔴 **Adicionar teste para novo comportamento** (se necessário)
   - Criar teste que falha com a mudança esperada

3. 🟢 **Implementar modificação**
   - Alterar código do método

4. ▶️ **APÓS modificar**: Executar TODOS os testes da classe novamente
   ```bash
   mvn test -Dtest=PagamentoServiceTest
   # Garantir que TODOS ainda estão passando
   ```

#### **Ao FINALIZAR implementação de um item da TODO:**

1. ▶️ **Executar aplicação Spring Boot COMPLETA**
   ```bash
   mvn spring-boot:run
   # SEM pular testes! Deixar rodar todos os testes
   ```

2. 📊 **Analisar logs de startup**
   - Verificar zero erros
   - Verificar zero warnings críticos
   - Verificar que todos os beans foram criados
   - Verificar que scheduler iniciou (se aplicável)
   - Verificar conexões com PostgreSQL, Redis, Kafka

3. ✅ **Validar testes passaram**
   ```
   [INFO] Tests run: X, Failures: 0, Errors: 0, Skipped: 0
   [INFO] BUILD SUCCESS
   ```

4. 📝 **Analisar logs da aplicação rodando**
   - Deixar rodar por pelo menos 1 minuto
   - Verificar se scheduler executou (se aplicável)
   - Verificar se não há exceptions em background
   - Verificar métricas de Circuit Breaker

### **❌ PROIBIÇÕES ABSOLUTAS:**

- ❌ **NUNCA** criar Service sem teste unitário correspondente
- ❌ **NUNCA** criar método em Service sem teste unitário
- ❌ **NUNCA** modificar método sem executar todos os testes da classe
- ❌ **NUNCA** dar item como completo sem rodar `mvn spring-boot:run` e analisar logs
- ❌ **NUNCA** usar `mvn spring-boot:run -DskipTests` ao finalizar item

### **✅ OBRIGAÇÕES ABSOLUTAS:**

- ✅ **SEMPRE** seguir Red-Green-Refactor
- ✅ **SEMPRE** criar teste ANTES da implementação (TDD clássico)
- ✅ **SEMPRE** executar teste e ver falhar ANTES de implementar
- ✅ **SEMPRE** executar TODOS os testes da classe após modificação
- ✅ **SEMPRE** rodar aplicação completa ao finalizar item
- ✅ **SEMPRE** analisar logs de startup e execução

### **📋 Checklist de Criação de Service/Método:**

- [ ] 🔴 Teste criado e executado → FALHOU ✅
- [ ] 🟢 Código implementado
- [ ] ▶️ Teste executado → PASSOU ✅
- [ ] 🔵 Código refatorado (se necessário)
- [ ] ▶️ TODOS os testes da classe executados → PASSARAM ✅
- [ ] ▶️ `mvn spring-boot:run` executado (sem skip tests)
- [ ] 📊 Logs analisados → Zero erros ✅
- [ ] 📊 Aplicação rodou por 1+ minuto → Sem exceptions ✅

---

## ⚠️ **Princípio KISS**

**Mantra**: *"O código que não existe é o código que não tem bugs"*

### **Checklist: "Preciso Mesmo Criar Isso?"**

Antes de criar qualquer abstração customizada, pergunte:

- [ ] **Já existe no Spring/Java?** (99% das vezes, sim)
- [ ] **Bean Validation resolve?** (`@NotNull`, `@Size`, `@Pattern`, `@DecimalMin`, etc)
- [ ] **Será usado em 3+ lugares?** (Se não, não crie abstração)
- [ ] **Alguém da equipe vai entender isso facilmente?** (Se não, simplifique)
- [ ] **Posso resolver com 1 linha de código padrão?** (Se sim, não crie classe customizada)

### **Regra dos 3 Usos**

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

## 🎯 **Regras de TDD**

### **⚠️ REGRAS CRÍTICAS - SEMPRE SEGUIR**

#### **1. Metodologia Red-Green-Refactor (TDD Clássico)**

**OBRIGATÓRIO**: Ao criar testes automatizados, seguir o ciclo completo:

**🔴 RED (Falha)**
1. Criar cenário de teste que **DEVE FALHAR**
2. Executar o teste
3. **VERIFICAR que falhou** com a mensagem esperada
4. **Nunca** prosseguir se o teste passar antes da implementação

**🟢 GREEN (Sucesso)**
1. Implementar o código mínimo para fazer o teste **passar**
2. Executar o teste novamente
3. **VERIFICAR que passou**

**🔵 REFACTOR (Melhoria)**
1. Melhorar o código mantendo os testes passando
2. Executar testes após cada refatoração

**Por que essa regra é crítica?**
- ✅ Garante que o teste está **realmente testando** a lógica
- ✅ Previne **falsos positivos** (testes que passam mas não validam nada)
- ✅ Documenta o comportamento esperado **antes** da implementação
- ❌ **Risco**: Testes que sempre passam podem estar encobrindo bugs

#### **2. Prioridade de Testes: Unitários PRIMEIRO**

**REGRA**: Se o usuário **NÃO** solicitar explicitamente testes de integração, criar **APENAS** testes unitários.

**Testes Unitários** (Prioridade ALTA - Fazer SEMPRE):
- ✅ Rápidos (< 1 segundo cada)
- ✅ Isolados (todos os dependencies mockados)
- ✅ Focados (testam 1 comportamento por vez)
- ✅ Executados a cada build
- **Padrão**: `*ServiceTest.java`, `*ControllerTest.java`, `*MapperTest.java`

**Testes de Integração** (Prioridade BAIXA - Fazer APENAS quando solicitado):
- ⏳ Lentos (> 5 segundos cada)
- ⏳ Complexos (Testcontainers, banco real, Kafka, Redis)
- ⏳ E2E (validam integração entre múltiplas camadas)
- ⏳ Executados em CI/CD
- **Padrão**: `*IntegrationTest.java`
- **Momento**: **Apenas após projeto completo** ou quando usuário solicitar

**Por que essa regra é crítica?**
- ✅ Testes unitários são mais **rápidos de criar e executar**
- ✅ Testes de integração requerem **infraestrutura complexa** (Docker, Testcontainers)
- ✅ Testes de integração devem ser feitos **após** projeto estabilizado
- ❌ **Risco**: Criar testes de integração prematuramente causa lentidão no desenvolvimento

#### **3. Processo de Decisão: Sempre Perguntar ao Usuário**

**REGRA**: Ao chegar em **cenários com múltiplas opções válidas**, **NUNCA** escolher automaticamente. **SEMPRE** perguntar ao usuário qual abordagem prefere.

**Por que essa regra é crítica?**
- ✅ Usuário mantém **controle das decisões** arquiteturais
- ✅ Evita **over-engineering** (agente escolhendo solução mais complexa)
- ✅ Decisões ficam **documentadas** na conversa
- ✅ Alinha expectativas entre agente e usuário
- ❌ **Risco**: Tomar decisões erradas que precisam ser revertidas depois

### **📋 Checklist de Testes**

Antes de considerar um módulo "testado", verificar:

- [ ] **RED**: Todos os testes falharam ANTES da implementação?
- [ ] **GREEN**: Todos os testes passam APÓS a implementação?
- [ ] **Unitários**: Todos os dependencies estão mockados?
- [ ] **Integração**: Apenas se solicitado explicitamente pelo usuário?
- [ ] **Decisões**: Todas as escolhas foram apresentadas ao usuário?
- [ ] **Cobertura**: Todos os cenários críticos estão cobertos?
- [ ] **Nomenclatura**: Nomes descrevem o comportamento esperado?
- [ ] **Isolamento**: Cada teste pode rodar independentemente?

### **🎯 Prioridade de Implementação de Testes**

**Ordem OBRIGATÓRIA**:
1. ✅ **Testes Unitários de Service** (`*ServiceTest.java`) - SEMPRE
2. ✅ **Testes Unitários de Controller** (`*ControllerTest.java`) - SEMPRE
3. ✅ **Testes Unitários de Mapper** (`*MapperTest.java`) - SEMPRE
4. ⏳ **Testes de Integração** (`*IntegrationTest.java`) - **APENAS SE SOLICITADO**

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

### **Fase 4 — Resiliência** 🔄 (75% completo)
- [x] Resilience4j configurado
- [x] Circuit Breaker + Retry + Bulkhead
- [x] Adquirente Simulado com Chaos
- [x] Scheduler de reprocessamento PENDENTE
- [ ] Integração completa
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

## 📚 **Documentação Técnica Completa**

Para informações detalhadas sobre:
- Stack Tecnológico (versões, tecnologias)
- Estrutura de Pastas
- Banco de Dados (DDL, migrations, índices)
- Mensageria Kafka (tópicos, eventos, configurações)
- Cache e Locks Distribuídos (Redis, Redisson)
- Resiliência (Resilience4j configurações)
- Observabilidade (Actuator, Prometheus, Swagger)
- APIs e Endpoints (exemplos completos)
- Configuração e Ambiente (setup, variáveis)
- Testes (estrutura, Testcontainers, execução)
- Deploy e CI/CD
- Monitoramento (Grafana, alertas)
- Troubleshooting
- FAQ (Perguntas Frequentes)

**Consulte**: [README.md](../../README.md)

---

**Última Atualização**: 02/11/2025  
**Versão**: 0.0.1-SNAPSHOT  
**Equipe**: ToolsChallenge
