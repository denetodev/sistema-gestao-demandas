package dev.denetodev.sgd_api.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record TipoAtividadeResponse(
        UUID id, String nome, String descricao, boolean ativo,
        OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
}