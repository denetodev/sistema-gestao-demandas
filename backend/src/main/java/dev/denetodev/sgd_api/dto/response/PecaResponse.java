package dev.denetodev.sgd_api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PecaResponse(
        UUID id,
        UUID demandaId, String demandaTitulo,
        UUID tipoPecaId, String tipoPecaNome,
        String nome, String descricao,
        Integer quantidade, BigDecimal valorUnitario, BigDecimal valorTotal,
        UUID pessoaId, String pessoaNome,
        LocalDate dataEntrega,
        OffsetDateTime createdAt, OffsetDateTime updatedAt
) {
}