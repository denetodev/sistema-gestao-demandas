package dev.denetodev.sgd_api.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record EquipamentoRequest(
        @NotNull(message = "categoriaId é obrigatório") UUID categoriaId,
        @NotBlank(message = "nome é obrigatório")
        @Size(max = 160, message = "nome deve ter no máximo 160 caracteres") String nome,
        @Size(max = 60, message = "patrimônio deve ter no máximo 60 caracteres") String patrimonio,
        @Size(max = 80, message = "número de série deve ter no máximo 80 caracteres") String numeroSerie,
        /** Só vale para categoria por quantidade; no controle individual é sempre 1. */
        @Min(value = 1, message = "quantidade deve ser pelo menos 1") Integer quantidadeTotal,
        @Size(max = 120, message = "localização deve ter no máximo 120 caracteres") String localizacao,
        String observacoes
) {
}
