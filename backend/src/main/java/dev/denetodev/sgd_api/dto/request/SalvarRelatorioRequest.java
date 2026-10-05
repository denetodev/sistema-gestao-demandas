package dev.denetodev.sgd_api.dto.request;

import java.util.List;
import java.util.UUID;

/** Seleção da pessoa: o que ficou de fora do relatório do mês e as observações gerais. */
public record SalvarRelatorioRequest(
        String observacoes,
        List<UUID> atividadesExcluidas
) {
}
