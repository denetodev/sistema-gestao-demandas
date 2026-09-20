package dev.denetodev.sgd_api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TipoAtividadeRequest(
        @NotBlank(message = "nome é obrigatório") String nome,
        String descricao
) {
}