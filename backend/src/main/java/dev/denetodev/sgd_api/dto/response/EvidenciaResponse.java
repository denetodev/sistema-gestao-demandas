package dev.denetodev.sgd_api.dto.response;

import dev.denetodev.sgd_api.entity.TipoEvidencia;

import java.time.OffsetDateTime;
import java.util.UUID;

public record EvidenciaResponse(
        UUID id, UUID atividadeId, UUID pecaId,
        TipoEvidencia tipo, String conteudo, String descricao,
        OffsetDateTime createdAt
) {
}