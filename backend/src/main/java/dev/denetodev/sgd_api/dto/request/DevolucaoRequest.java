package dev.denetodev.sgd_api.dto.request;

import java.util.List;
import java.util.UUID;

/** itemIds nulo ou vazio devolve tudo que ainda está pendente; senão, só os itens informados (devolução parcial). */
public record DevolucaoRequest(
        List<UUID> itemIds,
        String observacoes
) {
}
