package dev.denetodev.sgd_api.dto.request;

import dev.denetodev.sgd_api.entity.StatusDemanda;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StatusUpdateRequest(
        @NotNull(message = "status é obrigatório")
        StatusDemanda status,

        /** Opcional; fica na auditoria (útil ao cancelar ou devolver uma demanda). */
        @Size(max = 500, message = "motivo deve ter no máximo 500 caracteres")
        String motivo
) {
}
