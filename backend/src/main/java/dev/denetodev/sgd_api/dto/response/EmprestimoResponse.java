package dev.denetodev.sgd_api.dto.response;

import dev.denetodev.sgd_api.entity.StatusEmprestimo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record EmprestimoResponse(
        UUID id, StatusEmprestimo status, boolean atrasado,
        /** Sai do prédio: o frontend destaca para quem autoriza. */
        boolean externo, String localUso,
        String retiranteNome, String retiranteChaveC, String retiranteContato, UUID retirantePessoaId,
        String finalidade, OffsetDateTime previstaDevolucao,
        UUID demandaId, String demandaTitulo, UUID atividadeId,
        UUID registradoPorId, String registradoPorNome,
        int termoVersao, OffsetDateTime termoAceitoEm,
        String autorizadoPorNome, OffsetDateTime autorizadoEm, String motivoRecusa,
        String retiradoPorNome, OffsetDateTime retiradoEm,
        String devolvidoPorNome, OffsetDateTime devolvidoEm, String observacoesDevolucao,
        List<Item> itens, OffsetDateTime createdAt
) {
    public record Item(
            UUID id, UUID equipamentoId, String equipamentoNome, String patrimonio, String categoriaNome,
            int quantidade, OffsetDateTime devolvidoEm
    ) {
    }
}
