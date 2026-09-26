package dev.denetodev.sgd_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record TipoPecaRequest(
        @NotBlank(message = "nome é obrigatório") String nome,
        String descricao,
        @NotNull(message = "areaId é obrigatório") UUID areaId,
        @NotNull(message = "valorReferencia é obrigatório") BigDecimal valorReferencia
) {
}