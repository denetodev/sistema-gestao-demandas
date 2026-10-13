package dev.denetodev.sgd_api.dto.response;

import dev.denetodev.sgd_api.entity.StatusPessoa;

import java.util.UUID;

/** O que o Gestor/Admin precisa ver para decidir se a conta nova é mesmo a pessoa do cadastro importado. */
public record VinculoSugeridoResponse(
        UUID pessoaId,
        String nome,
        String nomeExibicao,
        String equipe,
        String cargo,
        String chaveC,
        StatusPessoa status,
        boolean jaTemConta,
        long demandas,
        long atividades,
        long pecas,
        /** Nome digitado por quem criou a conta, para comparar com o do cadastro importado. */
        String nomeInformado,
        String emailConta,
        /** Motivo pelo qual não dá para confirmar agora (ex.: pessoa inativa); nulo quando pode confirmar. */
        String impedimento
) {
}
