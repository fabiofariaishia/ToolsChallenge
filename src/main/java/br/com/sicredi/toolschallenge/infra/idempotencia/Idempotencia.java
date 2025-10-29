package br.com.sicredi.toolschallenge.infra.idempotencia;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Entidade JPA para armazenamento de chaves de idempotência.
 * Mapeia a tabela: infra.idempotencia
 * 
 * Funciona como fallback quando Redis está indisponível.
 * TTL: 24 horas (mesma janela de estorno)
 * 
 * Fluxo de idempotência:
 * 1. Cliente envia header "Chave-Idempotencia"
 * 2. Verificar Redis (cache rápido)
 * 3. Se Redis indisponível, verificar banco
 * 4. Se chave existe, retornar resposta original
 * 5. Se chave não existe, processar e armazenar
 */
@Entity
@Table(
    name = "idempotencia",
    schema = "infra",
    indexes = {
        @Index(name = "idx_idempotencia_expiracao", columnList = "expira_em"),
        @Index(name = "idx_idempotencia_id_transacao", columnList = "id_transacao")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString(exclude = {"responseBody"})
public class Idempotencia {

    /**
     * Chave de idempotência (fornecida no header Chave-Idempotencia)
     * Formato sugerido: UUID ou hash único do request
     */
    @Id
    @Column(length = 100)
    private String chave;

    /**
     * ID da transação processada
     */
    @Column(name = "id_transacao", nullable = false, length = 50)
    private String idTransacao;

    /**
     * Endpoint que processou a requisição
     * Exemplos: POST /pagamentos, POST /estornos
     */
    @Column(nullable = false, length = 100)
    private String endpoint;

    /**
     * HTTP status code da resposta original
     * Exemplos: 201 (Created), 409 (Conflict), 422 (Unprocessable Entity)
     */
    @Column(name = "status_http", nullable = false)
    private Integer statusHttp;

    /**
     * Response body original (JSON)
     * Retornado em caso de retry para garantir idempotência
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body", columnDefinition = "jsonb")
    private Map<String, Object> responseBody;

    /**
     * Timestamp de criação
     */
    @Column(name = "criado_em", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime criadoEm;

    /**
     * Timestamp de expiração (TTL 24h)
     */
    @Column(name = "expira_em", nullable = false)
    private OffsetDateTime expiraEm;

    @PrePersist
    protected void onCreate() {
        if (criadoEm == null) {
            criadoEm = OffsetDateTime.now();
        }
        if (expiraEm == null) {
            // TTL padrão: 24 horas
            expiraEm = criadoEm.plusHours(24);
        }
    }

    /**
     * Verifica se a chave está expirada
     */
    public boolean isExpirada() {
        return OffsetDateTime.now().isAfter(expiraEm);
    }

    /**
     * Verifica se a chave ainda é válida
     */
    public boolean isValida() {
        return !isExpirada();
    }
}
