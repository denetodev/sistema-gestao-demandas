package dev.denetodev.sgd_api.dto.response;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Uma mudança de status da demanda, vinda da auditoria. */
public record HistoricoStatusResponse(
        OffsetDateTime data,
        String de,
        String para,
        UUID porId,
        String porNome,
        String motivo
) {
}
