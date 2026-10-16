package dev.denetodev.sgd_api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Corrige os dados de um empréstimo que ainda espera autorização. Os equipamentos e o termo não mudam aqui:
 * para trocar o que sai, cancele e registre de novo (o OK do termo vale para aquela lista).
 */
public record EmprestimoEdicaoRequest(
        @NotBlank(message = "nome de quem retira é obrigatório")
        @Size(max = 160, message = "nome deve ter no máximo 160 caracteres") String retiranteNome,
        String retiranteChaveC,
        @Size(max = 160, message = "contato deve ter no máximo 160 caracteres") String retiranteContato,
        String finalidade,
        boolean externo,
        @Size(max = 160, message = "local de uso deve ter no máximo 160 caracteres") String localUso,
        /** Vazio mantém a retirada prevista atual. */
        OffsetDateTime retiradaPrevista,
        @NotNull(message = "data prevista de devolução é obrigatória") OffsetDateTime previstaDevolucao,
        UUID demandaId
) {
}
