package dev.denetodev.sgd_api.dto.request;

import jakarta.validation.constraints.Size;

public record AtualizarPerfilRequest(
        /** Como a pessoa quer ser chamada; vazio volta a usar o nome completo. */
        @Size(max = 80, message = "nomeExibicao deve ter no máximo 80 caracteres") String nomeExibicao,
        String fotoUrl
) {
}