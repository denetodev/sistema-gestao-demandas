package dev.denetodev.sgd_api.dto.response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AtividadeResponse(
        UUID id,
        UUID demandaId, String demandaTitulo,
        UUID tipoAtividadeId, String tipoAtividadeNome,
        UUID pessoaId, String pessoaNome,
        String descricao, LocalDate dataRealizacao,
        OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
}