package br.com.sicredi.toolschallenge.pagamento.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Entidade JPA que representa um pagamento com cartão de crédito.
 * Mapeia a tabela: pagamento.pagamento
 * 
 * Regras de negócio:
 * - ID da transação é único e imutável (chave de negócio)
 * - Valor deve ser sempre positivo
 * - AVISTA: 1 parcela obrigatória
 * - PARCELADO_LOJA/PARCELADO_EMISSOR: mínimo 2 parcelas
 * - Cartão sempre mascarado (PAN completo NUNCA armazenado)
 * - Dados armazenados em UTC
 */
@Entity
@Table(
    name = "pagamento", 
    schema = "pagamento",
    indexes = {
        @Index(name = "idx_pagamento_id_transacao", columnList = "id_transacao"),
        @Index(name = "idx_pagamento_status", columnList = "status"),
        @Index(name = "idx_pagamento_data_hora", columnList = "data_hora"),
        @Index(name = "idx_pagamento_estabelecimento", columnList = "estabelecimento")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_pagamento_id_transacao", columnNames = "id_transacao"),
        @UniqueConstraint(name = "uk_pagamento_nsu", columnNames = "nsu"),
        @UniqueConstraint(name = "uk_pagamento_codigo_autorizacao", columnNames = "codigo_autorizacao"),
        @UniqueConstraint(name = "uk_pagamento_snowflake_id", columnNames = "snowflake_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"cartaoMascarado"})
@EqualsAndHashCode(of = "idTransacao")
public class Pagamento {

    /**
     * Chave primária técnica (autoincrement)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID da transação (chave de negócio, única, imutável)
     * Exemplo: TXN-001-2025-AVISTA
     */
    @Column(name = "id_transacao", nullable = false, unique = true, length = 50, updatable = false)
    private String idTransacao;

    /**
     * Status do pagamento: PENDENTE, AUTORIZADO, NEGADO
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusPagamento status;

    /**
     * Valor da transação (DECIMAL 15,2)
     * Deve ser sempre maior que zero
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    /**
     * Código da moeda ISO-4217 (3 letras maiúsculas)
     * Padrão: BRL
     */
    @Column(nullable = false, length = 3)
    @Builder.Default
    private String moeda = "BRL";

    /**
     * Data/hora da transação (armazenada em UTC)
     */
    @Column(name = "data_hora", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime dataHora;

    /**
     * Nome do estabelecimento comercial
     */
    @Column(nullable = false)
    private String estabelecimento;

    /**
     * Tipo de pagamento: AVISTA, PARCELADO_LOJA, PARCELADO_EMISSOR
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_pagamento", nullable = false, length = 20)
    private TipoPagamento tipoPagamento;

    /**
     * Número de parcelas
     * AVISTA: 1
     * PARCELADO: >= 2 e <= 12
     */
    @Column(nullable = false)
    private Integer parcelas;

    /**
     * NSU - Número Sequencial Único (10 dígitos)
     * Gerado pelo adquirente após autorização
     */
    @Column(length = 10, unique = true)
    private String nsu;

    /**
     * Código de Autorização (9 dígitos com Luhn check)
     * Gerado pelo adquirente após autorização
     */
    @Column(name = "codigo_autorizacao", length = 9, unique = true)
    private String codigoAutorizacao;

    /**
     * Cartão mascarado no formato: 4444********1234
     * PAN completo NUNCA é armazenado (PCI-DSS compliance)
     */
    @Column(name = "cartao_mascarado", nullable = false, length = 20)
    private String cartaoMascarado;

    /**
     * Snowflake ID para geração de NSU (time-sortable, único)
     * Usado internamente para garantir unicidade e ordenação temporal
     */
    @Column(name = "snowflake_id", unique = true)
    private Long snowflakeId;

    /**
     * Timestamp de criação do registro (auditoria)
     */
    @Column(name = "criado_em", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime criadoEm;

    /**
     * Timestamp da última atualização (auditoria)
     * Atualizado automaticamente via trigger do banco
     */
    @Column(name = "atualizado_em", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime atualizadoEm;

    /**
     * Callback JPA executado antes de persistir nova entidade
     */
    @PrePersist
    protected void onCreate() {
        if (criadoEm == null) {
            criadoEm = OffsetDateTime.now();
        }
        if (atualizadoEm == null) {
            atualizadoEm = OffsetDateTime.now();
        }
    }

    /**
     * Callback JPA executado antes de atualizar entidade
     */
    @PreUpdate
    protected void onUpdate() {
        atualizadoEm = OffsetDateTime.now();
    }

    /**
     * Verifica se o pagamento foi autorizado
     */
    public boolean isAutorizado() {
        return StatusPagamento.AUTORIZADO.equals(status);
    }

    /**
     * Verifica se o pagamento foi negado
     */
    public boolean isNegado() {
        return StatusPagamento.NEGADO.equals(status);
    }

    /**
     * Verifica se o pagamento está pendente
     */
    public boolean isPendente() {
        return StatusPagamento.PENDENTE.equals(status);
    }

    /**
     * Verifica se o pagamento é à vista
     */
    public boolean isAvista() {
        return TipoPagamento.AVISTA.equals(tipoPagamento);
    }

    /**
     * Verifica se o pagamento é parcelado
     */
    public boolean isParcelado() {
        return TipoPagamento.PARCELADO_LOJA.equals(tipoPagamento) 
            || TipoPagamento.PARCELADO_EMISSOR.equals(tipoPagamento);
    }
}
