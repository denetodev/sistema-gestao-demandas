package dev.denetodev.sgd_api.dto.request;

import dev.denetodev.sgd_api.entity.PerfilPessoa;
import dev.denetodev.sgd_api.entity.StatusPessoa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PessoaRequest(
        @NotBlank(message = "nome é obrigatório") String nome,
        String email,
        @NotNull(message = "areaId é obrigatório") UUID areaId,
        UUID cargoId,
        StatusPessoa status,
        PerfilPessoa perfil
) {
}