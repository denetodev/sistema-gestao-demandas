package dev.denetodev.sgd_api.dto.response;

import java.util.UUID;

/**
 * Quem está no time da demanda: o suficiente para o grupo de fotos da lista e para agrupar o quadro por equipe e
 * núcleo (a equipe e o núcleo são os da pessoa; o núcleo é nulo se a equipe ainda não está ligada a um).
 */
public record ParticipanteResumo(
        UUID pessoaId, String nome, String fotoUrl, UUID areaId, String areaNome, String diretoriaNome, UUID nucleoId,
        String nucleoNome
) {
}
