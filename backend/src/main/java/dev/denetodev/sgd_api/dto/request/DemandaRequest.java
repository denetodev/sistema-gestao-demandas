package dev.denetodev.sgd_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;
import dev.denetodev.sgd_api.entity.Prioridade;

public record DemandaRequest(
        @NotBlank(message = "titulo é obrigatório")
        String titulo,

        String descricao,

        String codigo,

        @NotNull(message = "diretoriaId é obrigatório")
        UUID diretoriaId,

        UUID demandanteId,

        UUID projetoId,

        UUID campanhaId,

        Prioridade prioridade,

        LocalDate dataPrazo,

        String observacoes
) {
}