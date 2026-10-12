package dev.denetodev.sgd_api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

/** A parte de uma pessoa numa campanha (sem demandas canceladas). */
public record PessoaNaCampanhaResponse(
        UUID pessoaId,
        String nome,
        int demandas,
        int atividades,
        int pecas,
        BigDecimal valor
) {
}
