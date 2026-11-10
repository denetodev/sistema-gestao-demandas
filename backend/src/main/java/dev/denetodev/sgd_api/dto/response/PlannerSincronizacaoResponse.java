package dev.denetodev.sgd_api.dto.response;

import java.util.List;

/**
 * O que uma rodada de sincronização com o Planner encontrou.
 *
 * @param tarefasLidas         tarefas que o Graph devolveu
 * @param tarefasNaJanela      as que ainda estão abertas e foram criadas dentro da janela de dias
 * @param sugestoesNovas       sugestões criadas agora
 * @param etiquetasSemCadastro etiquetas de gente ("CRIATIVO - Fulano") que ninguém da equipe usa (podem ser de outras equipes do CRM)
 * @param nomesAmbiguos        etiquetas ou nomes que batem com mais de uma pessoa e por isso ficaram de fora
 */
public record PlannerSincronizacaoResponse(int tarefasLidas, int tarefasNaJanela, int sugestoesNovas,
                                           List<String> etiquetasSemCadastro, List<String> nomesAmbiguos) {
}
