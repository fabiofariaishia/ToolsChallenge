package br.com.sicredi.toolschallenge.estorno.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Entidade JPA que representa um estorno de pagamento.
 * Mapeia a tabela: estorno.estorno
 * 
 * Regras de negócio:
 * - Apenas pagamentos AUTORIZADOS podem ser estornados
 * - Estorno é sempre do valor total (estorno parcial não permitido)
 * - Janela de 24 horas para solicitar estorno
 * - Apenas 1 estorno CANCELADO por pagamento (constraint no banco)
 * - Múltiplas tentativas NEGADAS são permitidas
 */
@Entity
@Table(
    name = "estorno", 
    schema = "estorno",
    indexes = {
        @Index(name = "idx_estorno_id_transacao", columnList = "id_transacao"),
        @Index(name = "idx_estorno_id_estorno", columnList = "id_estorno"),
        @Index(name = "idx_estorno_status", columnList = "status"),
        @Index(name = "idx_estorno_data_hora", columnList = "data_hora")
    },
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_estorno_id_estorno", columnNames = "id_estorno"),
        @UniqueConstraint(name = "uk_estorno_nsu", columnNames = "nsu"),
        @UniqueConstraint(name = "uk_estorno_codigo_autorizacao", columnNames = "codigo_autorizacao"),
        @UniqueConstraint(name = "uk_estorno_snowflake_id", columnNames = "snowflake_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(of = "idEstorno")
public class Estorno {

    /**
     * Chave primária técnica (autoincrement)
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID da transação original do pagamento
     * Referência para pagamento.pagamento(id_transacao)
     */
    @Column(name = "id_transacao", nullable = false, length = 50)
    private String idTransacao;

    /**
     * ID único do estorno
     * Exemplo: EST-001-2025
     */
    @Column(name = "id_estorno", nullable = false, unique = true, length = 50, updatable = false)
    private String idEstorno;

    /**
     * Status do estorno: PENDENTE, CANCELADO, NEGADO
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private StatusEstorno status;

    /**
     * Valor a ser estornado (sempre valor total do pagamento)
     * Deve ser sempre maior que zero
     */
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal valor;

    /**
     * Data/hora da solicitação do estorno (armazenada em UTC)
     */
    @Column(name = "data_hora", nullable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime dataHora;

    /**
     * NSU do estorno (10 dígitos)
     * Gerado pelo adquirente após processar o estorno
     */
    @Column(length = 10, unique = true)
    private String nsu;

    /**
     * Código de Autorização do estorno (9 dígitos)
     * Gerado pelo adquirente após processar o estorno
     */
    @Column(name = "codigo_autorizacao", length = 9, unique = true)
    private String codigoAutorizacao;

    /**
     * Motivo/justificativa do estorno (opcional)
     * Exemplos: "Solicitação do cliente", "Produto com defeito", etc.
     */
    @Column(columnDefinition = "TEXT")
    private String motivo;

    /**
     * Snowflake ID para geração de NSU do estorno (time-sortable, único)
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
     * Verifica se o estorno foi cancelado (bem-sucedido)
     */
    public boolean isCancelado() {
        return StatusEstorno.CANCELADO.equals(status);
    }

    /**
     * Verifica se o estorno foi negado
     */
    public boolean isNegado() {
        return StatusEstorno.NEGADO.equals(status);
    }

    /**
     * Verifica se o estorno está pendente
     */
    public boolean isPendente() {
        return StatusEstorno.PENDENTE.equals(status);
    }
}
