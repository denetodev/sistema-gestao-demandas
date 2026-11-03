package dev.denetodev.sgd_api.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Uma tarefa do Planner esperando a decisão da pessoa. Se a demanda já existe no sistema (mesmo código), aceitar só
 * coloca a pessoa no time dela; senão, aceitar cria a demanda.
 */
public record PlannerSugestaoResponse(
        UUID id, String codigo, String titulo, String bucket, String etiqueta, OffsetDateTime criadaNoPlanner,
        UUID demandaExistenteId, String demandaExistenteTitulo
) {
}
