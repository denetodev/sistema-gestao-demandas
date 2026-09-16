package dev.denetodev.sgd_api.dto.request;

import dev.denetodev.sgd_api.entity.PapelPessoaDemanda;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ParticipanteRequest(
        @NotNull(message = "pessoaId é obrigatório")
        UUID pessoaId,

        PapelPessoaDemanda papel
) {
}