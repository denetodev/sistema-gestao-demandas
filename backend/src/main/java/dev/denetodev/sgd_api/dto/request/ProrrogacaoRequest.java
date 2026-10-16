package dev.denetodev.sgd_api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.OffsetDateTime;

public record ProrrogacaoRequest(
        @NotNull(message = "a nova data de devolução é obrigatória") OffsetDateTime novaPrevista,
        String motivo
) {
}
