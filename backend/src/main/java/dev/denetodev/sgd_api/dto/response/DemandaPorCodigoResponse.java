package dev.denetodev.sgd_api.dto.response;

import dev.denetodev.sgd_api.entity.StatusDemanda;

import java.util.UUID;

/** O mínimo para avisar, ao criar uma demanda, que o código já existe: qual é e em que situação está. */
public record DemandaPorCodigoResponse(UUID id, String codigo, String titulo, StatusDemanda status) {
}
