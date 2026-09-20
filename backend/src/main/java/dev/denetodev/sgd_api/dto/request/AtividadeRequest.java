package dev.denetodev.sgd_api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record AtividadeRequest(
        UUID demandaId,
        @NotNull(message = "tipoAtividadeId é obrigatório") UUID tipoAtividadeId,
        UUID pessoaId,
        String descricao,
        LocalDate dataRealizacao
) {
}