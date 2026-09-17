package dev.denetodev.sgd_api.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DiretoriaResponse(
        UUID id, String nome, String sigla, boolean ativo,
        OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
}
