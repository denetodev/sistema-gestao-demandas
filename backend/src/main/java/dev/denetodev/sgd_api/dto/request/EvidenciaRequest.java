package dev.denetodev.sgd_api.dto.request;

import dev.denetodev.sgd_api.entity.TipoEvidencia;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EvidenciaRequest(
        UUID atividadeId,
        UUID pecaId,
        @NotNull(message = "tipo é obrigatório") TipoEvidencia tipo,
        String conteudo,
        String descricao
) {
}