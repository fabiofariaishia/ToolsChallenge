package br.com.sicredi.toolschallenge.shared.exception;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Padrão de resposta para erros da API
 */
@Schema(description = "Estrutura padrão de resposta de erro")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErroResposta {
    
    @Schema(description = "Timestamp do erro", example = "2025-10-29T21:30:00")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
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
    
    public ErroResposta() {
        this.timestamp = LocalDateTime.now();
    }
    
    public ErroResposta(int status, String erro, String mensagem, String caminho) {
        this();
        this.status = status;
        this.erro = erro;
        this.mensagem = mensagem;
        this.caminho = caminho;
    }
    
    // Getters e Setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }
    
    public String getErro() {
        return erro;
    }
    
    public void setErro(String erro) {
        this.erro = erro;
    }
    
    public String getMensagem() {
        return mensagem;
    }
    
    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
    
    public String getCaminho() {
        return caminho;
    }
    
    public void setCaminho(String caminho) {
        this.caminho = caminho;
    }
    
    public List<CampoErro> getErrosValidacao() {
        return errosValidacao;
    }
    
    public void setErrosValidacao(List<CampoErro> errosValidacao) {
        this.errosValidacao = errosValidacao;
    }
    
    public String getTraceId() {
        return traceId;
    }
    
    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }
    
    /**
     * Representa um erro de validação em um campo específico
     */
    @Schema(description = "Erro de validação de campo")
    public static class CampoErro {
        
        @Schema(description = "Nome do campo com erro", example = "valor")
        private String campo;
        
        @Schema(description = "Valor rejeitado", example = "-100.00")
        private Object valorRejeitado;
        
        @Schema(description = "Mensagem de erro do campo", example = "deve ser maior que 0.01")
        private String mensagem;
        
        public CampoErro(String campo, Object valorRejeitado, String mensagem) {
            this.campo = campo;
            this.valorRejeitado = valorRejeitado;
            this.mensagem = mensagem;
        }
        
        // Getters e Setters
        public String getCampo() {
            return campo;
        }
        
        public void setCampo(String campo) {
            this.campo = campo;
        }
        
        public Object getValorRejeitado() {
            return valorRejeitado;
        }
        
        public void setValorRejeitado(Object valorRejeitado) {
            this.valorRejeitado = valorRejeitado;
        }
        
        public String getMensagem() {
            return mensagem;
        }
        
        public void setMensagem(String mensagem) {
            this.mensagem = mensagem;
        }
    }
}
