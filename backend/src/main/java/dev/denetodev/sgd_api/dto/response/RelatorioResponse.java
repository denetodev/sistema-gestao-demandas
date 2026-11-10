package dev.denetodev.sgd_api.dto.response;

import dev.denetodev.sgd_api.entity.ModeloRelatorio;
import dev.denetodev.sgd_api.entity.StatusRelatorio;
import dev.denetodev.sgd_api.entity.TipoEvidencia;

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
        /** Como o documento é montado para a função da pessoa (nulo nos relatórios aprovados antes desse recurso). */
        ModeloRelatorio modelo,
        /** As demandas em que a pessoa trabalhou no mês, com peças e evidências. */
        List<ItemDemanda> demandas,
        boolean podeEditar,
        boolean podeReabrir
) {

    /** Construtor sem o modelo e as demandas (padrão e lista vazia). */
    public RelatorioResponse(UUID pessoaId, String pessoaNome, String diretoria, String area, String cargo, String mes,
                             StatusRelatorio status, OffsetDateTime aprovadoEm, String observacoes,
                             List<ItemAtividade> atividades, List<ItemPeca> pecas, BigDecimal valorReferencia,
                             boolean podeEditar, boolean podeReabrir) {
        this(pessoaId, pessoaNome, diretoria, area, cargo, mes, status, aprovadoEm, observacoes, atividades, pecas,
                valorReferencia, ModeloRelatorio.PADRAO, List.of(), podeEditar, podeReabrir);
    }

    public record ItemDemanda(
            UUID id,
            String codigo,
            String titulo,
            String descricao,
            /** Link de acompanhamento da demanda (Planner, ClickUp). */
            String link,
            String demandante,
            String diretoria,
            String campanha,
            /** Primeiro dia do mês em que a pessoa trabalhou nela. */
            LocalDate data,
            /** As atividades da pessoa nessa demanda no mês (para respeitar a seleção do relatório). */
            List<UUID> atividadeIds,
            int quantidadePecas,
            BigDecimal valor,
            /** Peças por tipo, como "2 e-mails". */
            List<ResumoPeca> pecas,
            /** Tipos das atividades feitas, para a coluna Natureza. */
            List<String> naturezas,
            List<ItemEvidencia> evidencias
    ) {
    }

    public record ResumoPeca(String tipo, int quantidade) {
    }

    public record ItemEvidencia(
            UUID id,
            UUID atividadeId,
            UUID pecaId,
            TipoEvidencia tipo,
            String descricao,
            /** O endereço, quando a evidência é um link. */
            String link,
            /** Tem arquivo no armazenamento (a tela pede o endereço temporário dele). */
            boolean temArquivo,
            String arquivoMime
    ) {
    }

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
                observacoes, atividades, pecas, valorReferencia, modelo, demandas, podeEditar, podeReabrir);
    }
}
