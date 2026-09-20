package dev.denetodev.sgd_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PecaRequest(
        @NotNull(message = "demandaId é obrigatório") UUID demandaId,
        @NotNull(message = "tipoPecaId é obrigatório") UUID tipoPecaId,
        @NotBlank(message = "nome é obrigatório") String nome,
        String descricao
) {
}