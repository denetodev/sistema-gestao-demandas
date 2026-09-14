package dev.denetodev.sgd_api.dto.response;

import dev.denetodev.sgd_api.entity.Prioridade;
import dev.denetodev.sgd_api.entity.StatusDemanda;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record DemandaResponse(
        UUID id,
        String titulo,
        String descricao,
        String codigo,
        UUID diretoriaId,
        String diretoriaNome,
        UUID clienteId,
        String clienteNome,
        UUID projetoId,
        String projetoNome,
        UUID campanhaId,
        String campanhaNome,
        Prioridade prioridade,
        StatusDemanda status,
        LocalDate dataCriacao,
        LocalDate dataPrazo,
        LocalDate dataEntregaReal,
        BigDecimal valor,
        String observacoes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}