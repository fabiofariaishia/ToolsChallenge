package br.com.sicredi.toolschallenge.infra.auditoria;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Entidade que representa um evento de auditoria no sistema.
 * Registra todas as operações importantes para rastreabilidade e compliance.
 */
@Entity
@Table(name = "evento_auditoria", schema = "infra")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventoAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Tipo do evento (ex: PAGAMENTO_CRIADO, STATUS_ALTERADO)
     */
    @Column(name = "evento_tipo", nullable = false, length = 100)
    private String eventoTipo;

    /**
     * Tipo do agregado (ex: PAGAMENTO, ESTORNO)
     */
    @Column(name = "agregado_tipo", length = 50)
    private String agregadoTipo;

    /**
     * ID do agregado (ex: ID do pagamento ou estorno)
     */
    @Column(name = "agregado_id", length = 50)
    private String agregadoId;

    /**
     * Usuário ou sistema que gerou o evento
     */
    @Column(name = "usuario", length = 100)
    private String usuario;

    /**
     * Dados do evento em formato JSON
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dados", columnDefinition = "jsonb")
    private Map<String, Object> dados;

    /**
     * Informações adicionais sobre o contexto (ex: IP, User-Agent)
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadados", columnDefinition = "jsonb")
    private Map<String, Object> metadados;

    /**
     * Data e hora do evento
     */
    @Column(name = "criado_em", nullable = false)
    private OffsetDateTime criadoEm;

    @PrePersist
    protected void onCreate() {
        if (criadoEm == null) {
            criadoEm = OffsetDateTime.now();
        }
        if (usuario == null) {
            usuario = "SISTEMA";
        }
    }
}
