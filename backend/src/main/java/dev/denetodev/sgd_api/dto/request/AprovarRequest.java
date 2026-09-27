package dev.denetodev.sgd_api.dto.request;

import dev.denetodev.sgd_api.entity.PerfilPessoa;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AprovarRequest(
        @NotNull(message = "perfil é obrigatório") PerfilPessoa perfil,
        UUID areaId,
        UUID cargoId,
        UUID referenciaAreaId
) {
}
