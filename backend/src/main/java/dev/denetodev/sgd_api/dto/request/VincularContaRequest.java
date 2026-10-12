package dev.denetodev.sgd_api.dto.request;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/** Pessoa que já existe no sistema (por exemplo importada da planilha) e que vai ficar com a conta pendente. */
public record VincularContaRequest(
        @NotNull(message = "pessoaExistenteId é obrigatório") UUID pessoaExistenteId
) {
}
