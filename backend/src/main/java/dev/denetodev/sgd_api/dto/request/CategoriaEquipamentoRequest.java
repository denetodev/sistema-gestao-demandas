package dev.denetodev.sgd_api.dto.request;

import dev.denetodev.sgd_api.entity.ControleEquipamento;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoriaEquipamentoRequest(
        @NotBlank(message = "nome é obrigatório")
        @Size(max = 80, message = "nome deve ter no máximo 80 caracteres") String nome,
        @NotNull(message = "controle é obrigatório (INDIVIDUAL ou QUANTIDADE)") ControleEquipamento controle
) {
}
