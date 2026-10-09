package dev.denetodev.sgd_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record AutoCadastroRequest(
        @NotBlank(message = "nome completo é obrigatório") String nome,
        @NotBlank(message = "CPF é obrigatório") String cpf,
        @Size(max = 80, message = "nome de exibição deve ter no máximo 80 caracteres") String nomeExibicao,
        @NotNull(message = "areaId é obrigatório") UUID areaId,
        UUID cargoId
) {}
