package dev.denetodev.sgd_api.dto.request;

import dev.denetodev.sgd_api.entity.TipoCliente;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ClienteRequest(
        @NotBlank(message = "nome é obrigatório") String nome,
        @NotNull(message = "tipo é obrigatório") TipoCliente tipo,
        String observacao
) {
}