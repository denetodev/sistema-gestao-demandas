package dev.denetodev.sgd_api.dto.response;

import dev.denetodev.sgd_api.entity.ControleEquipamento;
import dev.denetodev.sgd_api.entity.StatusEmprestimo;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record EquipamentoResponse(
        UUID id, UUID categoriaId, String categoriaNome, ControleEquipamento controle,
        String nome, String patrimonio, String numeroSerie,
        int quantidadeTotal, int quantidadeDisponivel,
        String localizacao, String observacoes, boolean ativo,
        /** Quem está com o equipamento (ou reservou) agora: responde "com quem está e se está atrasado". */
        List<UsoAtual> emUso
) {
    public record UsoAtual(
            UUID emprestimoId, String retiranteNome, int quantidade, OffsetDateTime previstaDevolucao,
            StatusEmprestimo status, boolean atrasado, boolean externo, String localUso
    ) {
    }
}
