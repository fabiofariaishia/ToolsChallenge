package br.com.sicredi.toolschallenge.estorno.dto;

import br.com.sicredi.toolschallenge.estorno.domain.Estorno;
import org.springframework.stereotype.Component;

/**
 * Mapper manual para conversão entre Entidade e DTOs de Estorno.
 */
@Component
public class EstornoMapper {

    /**
     * Converte DTO de request para entidade.
     * 
     * @param dto DTO com dados da requisição
     * @return Entidade Estorno (sem ID, status=PENDENTE)
     */
    public Estorno paraEntidade(EstornoRequestDTO dto) {
        if (dto == null) {
            return null;
        }

        return Estorno.builder()
            .idTransacao(dto.getIdTransacao())
            .valor(dto.getValor())
            .motivo(dto.getMotivo())
            .build();
    }

    /**
     * Converte entidade para DTO de response.
     * 
     * @param entidade Entidade Estorno
     * @return DTO de resposta
     */
    public EstornoResponseDTO paraDTO(Estorno entidade) {
        if (entidade == null) {
            return null;
        }

        return EstornoResponseDTO.builder()
            .idTransacao(entidade.getIdTransacao())
            .idEstorno(entidade.getIdEstorno())
            .status(entidade.getStatus())
            .valor(entidade.getValor())
            .dataHora(entidade.getDataHora())
            .nsu(entidade.getNsu())
            .codigoAutorizacao(entidade.getCodigoAutorizacao())
            .motivo(entidade.getMotivo())
            .criadoEm(entidade.getCriadoEm())
            .atualizadoEm(entidade.getAtualizadoEm())
            .build();
    }

    /**
     * Converte entidade para DTO de response com mensagem customizada.
     * 
     * @param entidade Entidade Estorno
     * @param mensagem Mensagem adicional
     * @return DTO de resposta com mensagem
     */
    public EstornoResponseDTO paraDTO(Estorno entidade, String mensagem) {
        EstornoResponseDTO dto = paraDTO(entidade);
        if (dto != null) {
            dto.setMensagem(mensagem);
        }
        return dto;
    }
}
