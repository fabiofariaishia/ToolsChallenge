package br.com.sicredi.toolschallenge.shared.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Padrão de resposta para erros da API
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Estrutura padrão de resposta de erro")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErroResposta {
    
    @Schema(description = "Timestamp do erro", example = "2025-10-29T21:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp = LocalDateTime.now();
    
    @Schema(description = "Código de status HTTP", example = "400")
    private int status;
    
    @Schema(description = "Nome do erro HTTP", example = "Bad Request")
    private String erro;
    
    @Schema(description = "Mensagem de erro principal", example = "Pagamento não pode ser estornado")
    private String mensagem;
    
    @Schema(description = "Caminho da requisição", example = "/estornos")
    private String caminho;
    
    @Schema(description = "Lista de erros de validação (opcional)")
    private List<CampoErro> errosValidacao;
    
    @Schema(description = "ID de rastreamento para debug (opcional)")
    private String traceId;
    
    /**
     * Construtor para erros básicos (sem validações)
     */
    public ErroResposta(int status, String erro, String mensagem, String caminho) {
        this.timestamp = LocalDateTime.now();
        this.status = status;
        this.erro = erro;
        this.mensagem = mensagem;
        this.caminho = caminho;
    }
    
    /**
     * Representa um erro de validação em um campo específico
     */
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    @Schema(description = "Erro de validação de campo")
    public static class CampoErro {
        
        @Schema(description = "Nome do campo com erro", example = "valor")
        private String campo;
        
        @Schema(description = "Valor rejeitado", example = "-100.00")
        private Object valorRejeitado;
        
        @Schema(description = "Mensagem de erro do campo", example = "deve ser maior que 0.01")
        private String mensagem;
    }
}
