package dev.denetodev.sgd_api.dto.response;

import dev.denetodev.sgd_api.entity.StatusRelatorio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Relatório mensal de uma pessoa. Aberto: reflete as atividades do mês ao vivo, com a
 * flag {@code incluida}. Aprovado: é o retrato congelado na aprovação (só itens incluídos).
 */
public record RelatorioResponse(
        UUID pessoaId,
        String pessoaNome,
        String diretoria,
        String area,
        String cargo,
        String mes,
        StatusRelatorio status,
        OffsetDateTime aprovadoEm,
        String observacoes,
        List<ItemAtividade> atividades,
        List<ItemPeca> pecas,
        BigDecimal valorReferencia,
        boolean podeEditar,
        boolean podeReabrir
) {

    public record ItemAtividade(
            UUID id,
            LocalDate data,
            UUID demandaId,
            String demandaTitulo,
            String tipo,
            String descricao,
            boolean incluida
    ) {
    }

    public record ItemPeca(
            UUID id,
            String demandaTitulo,
            String nome,
            String tipo,
            int quantidade,
            BigDecimal valorUnitario,
            BigDecimal valorTotal
    ) {
    }

    public RelatorioResponse comPermissoes(boolean podeEditar, boolean podeReabrir) {
        return new RelatorioResponse(pessoaId, pessoaNome, diretoria, area, cargo, mes, status, aprovadoEm,
                observacoes, atividades, pecas, valorReferencia, podeEditar, podeReabrir);
    }
}
