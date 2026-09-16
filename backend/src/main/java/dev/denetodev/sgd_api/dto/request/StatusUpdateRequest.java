package dev.denetodev.sgd_api.dto.request;

import dev.denetodev.sgd_api.entity.StatusDemanda;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotNull(message = "status é obrigatório")
        StatusDemanda status
) {
}
