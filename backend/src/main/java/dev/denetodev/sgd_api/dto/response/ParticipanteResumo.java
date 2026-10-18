package dev.denetodev.sgd_api.dto.response;

import java.util.UUID;

/** Quem está no time da demanda, o suficiente para o grupo de fotos na lista. */
public record ParticipanteResumo(UUID pessoaId, String nome, String fotoUrl) {
}
