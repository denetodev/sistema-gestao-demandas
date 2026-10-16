package dev.denetodev.sgd_api.dto.response;

import dev.denetodev.sgd_api.entity.MomentoFoto;

import java.time.OffsetDateTime;
import java.util.UUID;

/** url é assinada e de curta duração (o bucket é privado): peça a lista de novo quando ela vencer. */
public record FotoEmprestimoResponse(
        UUID id, MomentoFoto momento, String url, int validadeSegundos,
        int tamanho, String enviadaPorNome, OffsetDateTime createdAt
) {
}
