package dev.denetodev.sgd_api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record RecusarEmprestimoRequest(
        @NotBlank(message = "o motivo da recusa é obrigatório") String motivo
) {
}
