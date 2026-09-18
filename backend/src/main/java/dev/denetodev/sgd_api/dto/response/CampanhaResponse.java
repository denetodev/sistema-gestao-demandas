package dev.denetodev.sgd_api.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record CampanhaResponse(
        UUID id, String nome, UUID projetoId, String projetoNome, String codigo,
        LocalDate dataInicio, LocalDate dataFim,
        OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
}