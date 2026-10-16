package dev.denetodev.sgd_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TermoEquipamentoRequest(
        @NotBlank(message = "texto é obrigatório")
        @Size(max = 600, message = "o termo deve ser curto: no máximo 600 caracteres") String texto
) {
}
