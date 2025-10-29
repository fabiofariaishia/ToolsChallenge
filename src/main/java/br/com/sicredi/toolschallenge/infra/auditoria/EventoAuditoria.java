package br.com.sicredi.toolschallenge.infra.auditoria;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Entidade JPA para auditoria de eventos do sistema.
 * Mapeia a tabela: infra.evento_auditoria
 * 
 * Registra todos os eventos importantes para:
 * - Compliance e rastreabilidade
 * - Troubleshooting e debug
 * - Análise de comportamento
 * - Segurança e detecção de fraudes
 * 
 * Exemplos de eventos auditados:
 * - PagamentoCriado, PagamentoAutorizado, PagamentoNegado
 * - EstornoSolicitado, EstornoCancelado, EstornoNegado
 * - ErroProcessamento, TentativaRetry
 */
@Entity
@Table(
    name = "evento_auditoria",
    schema = "infra",
    indexes = {
        @Index(name = "idx_evento_auditoria_tipo", columnList = "evento_tipo"),
        @Index(name = "idx_evento_auditoria_agregado", columnList = "agregado_tipo, agregado_id"),
        @Index(name = "idx_evento_auditoria_criado_em", columnList = "criado_em"),
        @Index(name = "idx_evento_auditoria_usuario", columnList = "usuario")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class EventoAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tipo do evento auditado
     * Exemplos: PagamentoCriado, EstornoSolicitado, ErroProcessamento
     */
    @Column(name = "evento_tipo", nullable = false, length = 100)
    private String eventoTipo;

    /**
     * Tipo do agregado relacionado ao evento
     * Exemplos: Pagamento, Estorno
     */
    @Column(name = "agregado_tipo", length = 50)
    private String agregadoTipo;

    /**
     * ID do agregado relacionado
     * Exemplo: TXN-001-2025-AVISTA
     */
    @Column(name = "agregado_id", length = 50)
    private String agregadoId;

    /**
     * Usuário/sistema que executou a ação
     * Exemplos: operador@exemplo.com, sistema, scheduler
     */
    @Column(length = 100)
    private String usuario;

    /**
     * Dados do evento em formato JSON
     * Contém informações relevantes do evento
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> dados;

    /**
     * Metadados adicionais do contexto
     * Exemplos: IP, User-Agent, Request-ID, Correlation-ID
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadados;

    /**
     * Timestamp do evento
     */
    @Column(name = "criado_em", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime criadoEm;

    @PrePersist
    protected void onCreate() {
        if (criadoEm == null) {
            criadoEm = OffsetDateTime.now();
        }
    }
}
