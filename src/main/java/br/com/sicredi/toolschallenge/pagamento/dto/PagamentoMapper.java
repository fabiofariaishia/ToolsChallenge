package br.com.sicredi.toolschallenge.pagamento.dto;

import br.com.sicredi.toolschallenge.pagamento.domain.Pagamento;
import org.springframework.stereotype.Component;

/**
 * Mapper manual para conversão entre Entidade e DTOs.
 * 
 */
@Component
public class PagamentoMapper {

    /**
     * Converte DTO de request para entidade.
     * 
     * @param dto DTO com dados da requisição
     * @return Entidade Pagamento (sem ID, status=PENDENTE)
     */
    public Pagamento toEntity(PagamentoRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        return Pagamento.builder()
            .valor(dto.getValor())
            .moeda(dto.getMoeda())
            .estabelecimento(dto.getEstabelecimento())
            .tipoPagamento(dto.getTipoPagamento())
            .parcelas(dto.getParcelas())
            .cartaoMascarado(dto.getCartaoMascarado())
            .build();
    }

    /**
     * Converte entidade para DTO de response.
     * 
     * @param entity Entidade Pagamento
     * @return DTO de resposta
     */
    public PagamentoResponseDTO toDTO(Pagamento entity) {
        if (entity == null) {
            return null;
        }

        return PagamentoResponseDTO.builder()
            .idTransacao(entity.getIdTransacao())
            .status(entity.getStatus())
            .valor(entity.getValor())
            .moeda(entity.getMoeda())
            .dataHora(entity.getDataHora())
            .estabelecimento(entity.getEstabelecimento())
            .tipoPagamento(entity.getTipoPagamento())
            .parcelas(entity.getParcelas())
            .nsu(entity.getNsu())
            .codigoAutorizacao(entity.getCodigoAutorizacao())
            .cartaoMascarado(entity.getCartaoMascarado())
            .criadoEm(entity.getCriadoEm())
            .atualizadoEm(entity.getAtualizadoEm())
            .build();
    }

    /**
     * Converte entidade para DTO de response com mensagem customizada.
     * 
     * @param entity Entidade Pagamento
     * @param mensagem Mensagem adicional
     * @return DTO de resposta com mensagem
     */
    public PagamentoResponseDTO toDTO(Pagamento entity, String mensagem) {
        PagamentoResponseDTO dto = toDTO(entity);
        if (dto != null) {
            dto.setMensagem(mensagem);
        }
        return dto;
    }
}
