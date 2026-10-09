package dev.denetodev.sgd_api.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Linha da visão de Gestor/Admin: situação do relatório de cada profissional no mês. */
public record RelatorioResumoResponse(
        UUID pessoaId,
        String nome,
        String area,
        /** NAO_INICIADO quando a pessoa ainda não abriu o relatório do mês. */
        String situacao,
        long atividadesNoMes,
        OffsetDateTime aprovadoEm
) {
}
