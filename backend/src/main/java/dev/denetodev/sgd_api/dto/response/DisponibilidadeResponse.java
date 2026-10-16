package dev.denetodev.sgd_api.dto.response;

import java.util.UUID;

/** Quanto do equipamento está livre durante todo o período consultado (o pior momento manda). */
public record DisponibilidadeResponse(UUID equipamentoId, int quantidadeDisponivel) {
}
