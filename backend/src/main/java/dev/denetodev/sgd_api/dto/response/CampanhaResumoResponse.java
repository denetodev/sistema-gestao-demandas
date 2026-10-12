package dev.denetodev.sgd_api.dto.response;

import dev.denetodev.sgd_api.entity.StatusDemanda;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * O que uma campanha reuniu. Demandas canceladas contam em {@code demandas} e {@code porStatus}, mas ficam
 * de fora de pessoas, valor, peças e atividades.
 */
public record CampanhaResumoResponse(
        UUID id,
        String nome,
        String codigo,
        int demandas,
        int demandasAtivas,
        int pessoas,
        BigDecimal valor,
        int pecas,
        int atividades,
        Map<StatusDemanda, Integer> porStatus,
        LocalDate primeiraDemanda,
        LocalDate ultimaDemanda
) {
}
