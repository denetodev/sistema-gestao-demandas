package dev.denetodev.sgd_api.dto.request;

import dev.denetodev.sgd_api.entity.PerfilPessoa;
import dev.denetodev.sgd_api.entity.StatusPessoa;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PessoaRequest(
        @NotBlank(message = "nome é obrigatório") String nome,
        String email,
        /** Só escrita: aceita com ou sem pontuação; vazio/nulo mantém o atual. A resposta traz apenas a máscara. */
        String cpf,
        @NotNull(message = "areaId é obrigatório") UUID areaId,
        UUID cargoId,
        StatusPessoa status,
        PerfilPessoa perfil,
        /** Referência de equipe dessa área; nulo remove a referência (é uma atribuição, como areaId e cargoId). */
        UUID referenciaAreaId
) {
}