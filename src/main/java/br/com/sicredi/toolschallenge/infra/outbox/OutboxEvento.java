package br.com.sicredi.toolschallenge.infra.outbox;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;
import java.util.Map;

/**
 * Entidade JPA que implementa o Transactional Outbox Pattern.
 * Mapeia a tabela: infra.outbox
 * 
 * Garante que eventos sejam publicados no Kafka de forma transacional:
 * 1. Evento é salvo no banco na mesma transação da mudança de estado
 * 2. Job assíncrono (500ms) processa eventos PENDENTE
 * 3. Publica no Kafka e marca como PROCESSADO
 * 4. Em caso de erro, marca como ERRO e incrementa tentativas
 * 
 * Garante: At-least-once delivery (idempotência no consumidor)
 */
@Entity
@Table(
    name = "outbox",
    schema = "infra",
    indexes = {
        @Index(name = "idx_outbox_status_pendente", columnList = "status, criado_em"),
        @Index(name = "idx_outbox_agregado", columnList = "agregado_tipo, agregado_id"),
        @Index(name = "idx_outbox_criado_em", columnList = "criado_em")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class OutboxEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ID do agregado que gerou o evento (ex: id_transacao)
     */
    @Column(name = "agregado_id", nullable = false, length = 50)
    private String agregadoId;

    /**
     * Tipo do agregado (ex: Pagamento, Estorno)
     */
    @Column(name = "agregado_tipo", nullable = false, length = 50)
    private String agregadoTipo;

    /**
     * Tipo do evento de domínio
     * Exemplos: PagamentoAutorizado, PagamentoNegado, EstornoCancelado
     */
    @Column(name = "evento_tipo", nullable = false, length = 100)
    private String eventoTipo;

    /**
     * Payload do evento em formato JSON
     * Armazenado como JSONB no PostgreSQL para queries eficientes
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;

    /**
     * Tópico Kafka de destino
     * Exemplos: pagamento.eventos, estorno.eventos
     */
    @Column(name = "topico_kafka", nullable = false, length = 100)
    private String topicoKafka;

    /**
     * Status do processamento: PENDENTE, PROCESSADO, ERRO
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "PENDENTE";

    /**
     * Número de tentativas de envio ao Kafka
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer tentativas = 0;

    /**
     * Última mensagem de erro (se houver)
     */
    @Column(name = "ultimo_erro", columnDefinition = "TEXT")
    private String ultimoErro;

    /**
     * Timestamp de criação do evento
     */
    @Column(name = "criado_em", nullable = false, updatable = false, columnDefinition = "TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP")
    private OffsetDateTime criadoEm;

    /**
     * Timestamp de processamento (quando publicado no Kafka)
     */
    @Column(name = "processado_em", columnDefinition = "TIMESTAMP WITH TIME ZONE")
    private OffsetDateTime processadoEm;

    @PrePersist
    protected void onCreate() {
        if (criadoEm == null) {
            criadoEm = OffsetDateTime.now();
        }
    }

    /**
     * Marca evento como processado
     */
    public void marcarComoProcessado() {
        this.status = "PROCESSADO";
        this.processadoEm = OffsetDateTime.now();
    }

    /**
     * Marca evento como erro e incrementa tentativas
     */
    public void marcarComoErro(String mensagemErro) {
        this.status = "ERRO";
        this.ultimoErro = mensagemErro;
        this.tentativas++;
    }

    /**
     * Verifica se evento está pendente
     */
    public boolean isPendente() {
        return "PENDENTE".equals(status);
    }

    /**
     * Verifica se evento foi processado
     */
    public boolean isProcessado() {
        return "PROCESSADO".equals(status);
    }

    /**
     * Verifica se evento teve erro
     */
    public boolean isErro() {
        return "ERRO".equals(status);
    }
}
