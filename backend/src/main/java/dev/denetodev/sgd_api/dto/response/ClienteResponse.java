package dev.denetodev.sgd_api.dto.response;

import dev.denetodev.sgd_api.entity.TipoCliente;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ClienteResponse(
        UUID id, String nome, TipoCliente tipo, String observacao, boolean ativo,
        OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
}