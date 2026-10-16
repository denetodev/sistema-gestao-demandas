package dev.denetodev.sgd_api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record EmprestimoRequest(
        @NotBlank(message = "nome de quem retira é obrigatório")
        @Size(max = 160, message = "nome deve ter no máximo 160 caracteres") String retiranteNome,
        /** Opcional: C ou F + 7 dígitos. Quem retira não precisa ter conta no sistema. */
        String retiranteChaveC,
        @Size(max = 160, message = "contato deve ter no máximo 160 caracteres") String retiranteContato,
        /** Opcional: quando quem retira tem conta no sistema. */
        UUID retirantePessoaId,
        String finalidade,
        /** Sai do prédio. */
        boolean externo,
        @Size(max = 160, message = "local de uso deve ter no máximo 160 caracteres") String localUso,
        @NotNull(message = "data prevista de devolução é obrigatória") OffsetDateTime previstaDevolucao,
        UUID demandaId,
        UUID atividadeId,
        @NotEmpty(message = "escolha pelo menos um equipamento") List<@Valid Item> itens,
        /** O OK do termo na tela: sem ele o empréstimo não é registrado. */
        boolean termoAceito
) {
    public record Item(
            @NotNull(message = "equipamentoId é obrigatório") UUID equipamentoId,
            @Min(value = 1, message = "quantidade deve ser pelo menos 1") Integer quantidade
    ) {
    }
}
