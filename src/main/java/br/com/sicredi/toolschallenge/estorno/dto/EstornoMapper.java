package br.com.sicredi.toolschallenge.estorno.dto;

import br.com.sicredi.toolschallenge.estorno.domain.Estorno;
import br.com.sicredi.toolschallenge.pagamento.domain.Pagamento;
import org.springframework.stereotype.Component;

/**
 * Mapper manual para conversão entre Entidade e DTOs de Estorno.
 * 
 * IMPORTANTE: Usa DTOs próprios do módulo estorno (TransacaoEstornoDTO, DescricaoEstornoDTO, FormaPagamentoEstornoDTO)
 * para garantir independência entre módulos (arquitetura monolito modular).
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
     * Converte entidade Estorno + Pagamento para DTO de response com estrutura aninhada.
     * 
     * @param estorno Entidade Estorno com dados do estorno
     * @param pagamento Entidade Pagamento original (para pegar dados da transação)
     * @return DTO de resposta com estrutura TransacaoEstornoDTO
     */
    public EstornoResponseDTO paraDTO(Estorno estorno, Pagamento pagamento) {
        if (estorno == null || pagamento == null) {
            return null;
        }

        // Formatar data/hora no padrão dd/MM/yyyy HH:mm:ss
        String dataHoraFormatada = pagamento.getDataHora().format(
            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")
        );

        // Construir DescricaoEstornoDTO com dados do pagamento original + dados do ESTORNO
        DescricaoEstornoDTO descricao = DescricaoEstornoDTO.builder()
            .valor(pagamento.getValor())
            .dataHora(dataHoraFormatada)  // String formatada
            .estabelecimento(pagamento.getEstabelecimento())
            .nsu(estorno.getNsu())  // NSU do ESTORNO (pode ser null se PENDENTE/NEGADO)
            .codigoAutorizacao(estorno.getCodigoAutorizacao())  // Código do ESTORNO (pode ser null)
            .status(estorno.getStatus().name())  // Status do ESTORNO: "CANCELADO", "PENDENTE", "NEGADO"
            .build();

        // Construir FormaPagamentoEstornoDTO com dados do pagamento original
        FormaPagamentoEstornoDTO formaPagamento = FormaPagamentoEstornoDTO.builder()
            .tipo(pagamento.getTipoPagamento().name())  // Converter enum para String
            .parcelas(pagamento.getParcelas())
            .build();

        // Construir TransacaoEstornoDTO
        TransacaoEstornoDTO transacao = TransacaoEstornoDTO.builder()
            .cartao(pagamento.getCartaoMascarado())
            .id(pagamento.getIdTransacao())
            .descricao(descricao)  // Contém nsu/codigoAutorizacao/status do ESTORNO
            .formaPagamento(formaPagamento)
            .build();

        return EstornoResponseDTO.builder()
            .transacao(transacao)
            .build();
    }
}
