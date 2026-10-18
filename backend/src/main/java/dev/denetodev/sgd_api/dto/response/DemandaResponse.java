package dev.denetodev.sgd_api.dto.response;

import dev.denetodev.sgd_api.entity.Prioridade;
import dev.denetodev.sgd_api.entity.StatusDemanda;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record DemandaResponse(
        UUID id,
        String titulo,
        String descricao,
        String codigo,
        UUID diretoriaId,
        String diretoriaNome,
        UUID demandanteId,
        String demandanteNome,
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
        String linkExterno,
        BigDecimal valorCalculado,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        /** A pessoa logada pode alterar esta demanda (participa, Referência da área do time, Gestor ou Admin). */
        boolean podeAlterar,
        /** Quem está no time agora (sem quem já saiu); o Responsável principal vem primeiro. */
        List<ParticipanteResumo> participantes
) {
}