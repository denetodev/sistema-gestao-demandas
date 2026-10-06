package dev.denetodev.sgd_api.dto.response;

import dev.denetodev.sgd_api.entity.PerfilPessoa;
import dev.denetodev.sgd_api.entity.StatusPessoa;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PessoaResponse(
        UUID id, String nome, String nomeExibicao, String email, String fotoUrl,
        UUID diretoriaId, String diretoriaNome,
        UUID areaId, String areaNome,
        UUID cargoId, String cargoNome,
        StatusPessoa status, PerfilPessoa perfil,
        UUID referenciaAreaId, String referenciaAreaNome,
        UUID authUserId,
        UUID aprovadoPorId, String aprovadoPorNome, OffsetDateTime aprovadoEm,
        OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
}