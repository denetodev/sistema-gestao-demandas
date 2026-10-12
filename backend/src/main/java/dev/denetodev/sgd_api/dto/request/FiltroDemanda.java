package dev.denetodev.sgd_api.dto.request;

import dev.denetodev.sgd_api.entity.EscopoListagem;
import dev.denetodev.sgd_api.entity.StatusDemanda;

import java.util.UUID;

/**
 * Filtros da lista de demandas. {@code diretoriaId} é o núcleo e {@code areaId} é a equipe.
 * Os filtros só estreitam o escopo: nunca mostram o que o escopo da pessoa não permite.
 */
public record FiltroDemanda(
        EscopoListagem escopo,
        String busca,
        StatusDemanda status,
        UUID diretoriaId,
        UUID areaId
) {
}
