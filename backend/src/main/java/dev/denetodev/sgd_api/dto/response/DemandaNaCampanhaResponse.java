package dev.denetodev.sgd_api.dto.response;

import dev.denetodev.sgd_api.entity.StatusDemanda;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record DemandaNaCampanhaResponse(
        UUID id,
        String titulo,
        StatusDemanda status,
        LocalDate dataCriacao,
        BigDecimal valor
) {
}
