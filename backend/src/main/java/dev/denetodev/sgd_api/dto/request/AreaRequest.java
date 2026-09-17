package dev.denetodev.sgd_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AreaRequest(
        @NotBlank(message = "nome é obrigatório") String nome,
        @NotNull(message = "diretoriaId é obrigatório") UUID diretoriaId
) {
}