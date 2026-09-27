package dev.denetodev.sgd_api.dto.response;

import dev.denetodev.sgd_api.entity.StatusProjeto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ProjetoResponse(
        UUID id, String nome,
        UUID demandanteId, String demandanteNome,
        UUID diretoriaId, String diretoriaNome,
        String descricao, StatusProjeto status,
        OffsetDateTime createdAt, OffsetDateTime updatedAt
) {}