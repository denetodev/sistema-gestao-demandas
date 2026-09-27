package dev.denetodev.sgd_api.dto.response;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record TipoPecaResponse(
        UUID id, String nome, String descricao,
        UUID areaId, String areaNome,
        BigDecimal valorReferencia,
        boolean ativo, OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
}