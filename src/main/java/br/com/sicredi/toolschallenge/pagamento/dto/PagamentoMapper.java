package br.com.sicredi.toolschallenge.pagamento.dto;

import br.com.sicredi.toolschallenge.pagamento.domain.Pagamento;
import org.springframework.stereotype.Component;

/**
 * Mapper manual para conversão entre Entidade e DTOs.
 * 
 * Mapeia estrutura aninhada:
 * - transacao.cartao → entity.cartaoMascarado
 * - transacao.id → entity.idTransacao
 * - transacao.descricao.valor → entity.valor
 * - transacao.descricao.dataHora → entity.dataHora
 * - transacao.descricao.estabelecimento → entity.estabelecimento
 * - transacao.formaPagamento.tipo → entity.tipoPagamento
 * - transacao.formaPagamento.parcelas → entity.parcelas
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
        if (dto == null || dto.getTransacao() == null) {
            return null;
        }

        TransacaoDTO transacao = dto.getTransacao();
        DescricaoDTO descricao = transacao.getDescricao();
        FormaPagamentoDTO formaPagamento = transacao.getFormaPagamento();

        return Pagamento.builder()
            .cartaoMascarado(transacao.getCartao())
            .idTransacao(transacao.getId())
            .valor(descricao != null ? descricao.getValor() : null)
            .moeda("BRL") // Default moeda BRL
            .dataHora(descricao != null ? descricao.getDataHora() : null)
            .estabelecimento(descricao != null ? descricao.getEstabelecimento() : null)
            .tipoPagamento(formaPagamento != null ? formaPagamento.getTipo() : null)
            .parcelas(formaPagamento != null ? formaPagamento.getParcelas() : null)
            .build();
    }

    /**
     * Converte entidade para DTO de response.
     * 
     * @param entity Entidade Pagamento
     * @return DTO de resposta com estrutura aninhada TransacaoDTO
     */
    public PagamentoResponseDTO toDTO(Pagamento entity) {
        if (entity == null) {
            return null;
        }

        // Criar DescricaoDTO
        DescricaoDTO descricao = DescricaoDTO.builder()
            .valor(entity.getValor())
            .dataHora(entity.getDataHora())
            .estabelecimento(entity.getEstabelecimento())
            .build();

        // Criar FormaPagamentoDTO
        FormaPagamentoDTO formaPagamento = FormaPagamentoDTO.builder()
            .tipo(entity.getTipoPagamento())
            .parcelas(entity.getParcelas())
            .build();

        // Criar TransacaoDTO com nsu, codigoAutorizacao e status no nível da transação
        TransacaoDTO transacao = TransacaoDTO.builder()
            .cartao(entity.getCartaoMascarado())
            .id(entity.getIdTransacao())
            .descricao(descricao)
            .formaPagamento(formaPagamento)
            .nsu(entity.getNsu())
            .codigoAutorizacao(entity.getCodigoAutorizacao())
            .status(entity.getStatus().name())
            .build();

        // Retornar PagamentoResponseDTO com TransacaoDTO aninhado
        return PagamentoResponseDTO.builder()
            .transacao(transacao)
            .build();
    }
}
