package dev.denetodev.sgd_api.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PecaResponse(
        UUID id,
        UUID demandaId, String demandaTitulo,
        UUID tipoPecaId, String tipoPecaNome,
        String nome, String descricao,
        OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
}