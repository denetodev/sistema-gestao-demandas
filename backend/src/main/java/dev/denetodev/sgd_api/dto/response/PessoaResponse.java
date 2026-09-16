package dev.denetodev.sgd_api.dto.response;

import dev.denetodev.sgd_api.entity.StatusPessoa;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PessoaResponse(
        UUID id,
        String nome,
        String email,
        UUID diretoriaId,
        String diretoriaNome,
        UUID areaId,
        UUID cargoId,
        StatusPessoa status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
