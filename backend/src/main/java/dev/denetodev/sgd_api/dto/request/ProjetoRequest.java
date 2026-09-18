package dev.denetodev.sgd_api.dto.request;

import dev.denetodev.sgd_api.entity.StatusProjeto;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record ProjetoRequest(
        @NotBlank(message = "nome é obrigatório") String nome,
        UUID clienteId,
        UUID diretoriaId,
        String descricao,
        StatusProjeto status
) {
}