package dev.denetodev.sgd_api.dto.response;

import dev.denetodev.sgd_api.entity.ControleEquipamento;
import dev.denetodev.sgd_api.entity.StatusEmprestimo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Agenda dos equipamentos (o gráfico de Gantt): uma linha por equipamento com os períodos em que está reservado,
 * emprestado ou já foi devolvido dentro da janela pedida.
 */
public record AgendaResponse(OffsetDateTime de, OffsetDateTime ate, OffsetDateTime agora, List<Linha> equipamentos) {

    public record Linha(
            UUID equipamentoId, String nome, String patrimonio, UUID categoriaId, String categoriaNome,
            ControleEquipamento controle, int quantidadeTotal, List<Bloco> blocos
    ) {
    }

    /**
     * Um período de uso. {@code fim} é a devolução prevista (ou a real, se já devolveu); quando {@code atrasado},
     * o equipamento continua com a pessoa depois de {@code fim}, até {@code agora}.
     */
    public record Bloco(
            UUID emprestimoId, String retiranteNome, StatusEmprestimo status, boolean devolvido, boolean atrasado,
            boolean externo, String localUso, int quantidade, OffsetDateTime inicio, OffsetDateTime fim
    ) {
    }
}
