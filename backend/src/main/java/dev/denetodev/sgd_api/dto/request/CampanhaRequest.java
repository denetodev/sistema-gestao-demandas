package dev.denetodev.sgd_api.dto.request;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;
import java.util.UUID;

public record CampanhaRequest(
        @NotBlank(message = "nome é obrigatório") String nome,
        UUID projetoId,
        String codigo,
        LocalDate dataInicio,
        LocalDate dataFim
) {
}
