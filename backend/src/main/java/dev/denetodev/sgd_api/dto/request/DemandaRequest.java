package dev.denetodev.sgd_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

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

        String observacoes,

        @Size(max = 500, message = "linkExterno deve ter no máximo 500 caracteres")
        String linkExterno
) {
}