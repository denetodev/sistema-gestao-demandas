package dev.denetodev.sgd_api.dto.request;

import dev.denetodev.sgd_api.entity.PapelPessoaDemanda;
import jakarta.validation.constraints.NotNull;

public record AtualizarPapelRequest(
        @NotNull(message = "papel é obrigatório")
        PapelPessoaDemanda papel
) {
}
