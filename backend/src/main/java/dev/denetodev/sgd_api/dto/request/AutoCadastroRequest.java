package dev.denetodev.sgd_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AutoCadastroRequest(
        @NotBlank(message = "nome é obrigatório") String nome,
        @NotNull(message = "areaId é obrigatório") UUID areaId,
        UUID cargoId
) {}
