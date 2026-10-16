package dev.denetodev.sgd_api.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AreaResponse(
        UUID id, String nome, UUID diretoriaId, String diretoriaNome,
        UUID nucleoId, String nucleoNome,
        boolean ativo, boolean acessaEquipamentos, OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
}