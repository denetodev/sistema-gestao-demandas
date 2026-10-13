package dev.denetodev.sgd_api.dto.request;

import dev.denetodev.sgd_api.entity.EscopoListagem;
import dev.denetodev.sgd_api.entity.StatusDemanda;

import java.util.UUID;

/**
 * Filtros da lista de demandas: diretoria da demanda, núcleo e equipe (área) de quem está no time.
 * Os filtros só estreitam o escopo: nunca mostram o que o escopo da pessoa não permite.
 */
public record FiltroDemanda(
        EscopoListagem escopo,
        String busca,
        StatusDemanda status,
        UUID diretoriaId,
        UUID nucleoId,
        UUID areaId
) {
}
