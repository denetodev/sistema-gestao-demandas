package dev.denetodev.sgd_api.dto.response;

import java.util.List;

public record CampanhaDetalheResponse(
        CampanhaResumoResponse resumo,
        List<PessoaNaCampanhaResponse> pessoas,
        List<DemandaNaCampanhaResponse> demandas
) {
}
