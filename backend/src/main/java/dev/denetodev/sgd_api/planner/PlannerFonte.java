package dev.denetodev.sgd_api.planner;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

/**
 * De onde vêm as tarefas do Planner. A implementação real fala com o Microsoft Graph; nos testes entra uma fonte
 * de mentira, então a regra de quem recebe sugestão se testa sem rede.
 */
public interface PlannerFonte {

    /** Só leitura: o sistema nunca escreve no Planner. */
    PlanoPlanner ler(String planId);

    /** A fonte está configurada (tenant, aplicativo e plano)? Sem isso a sincronização nem tenta. */
    boolean configurada();

    /** Os planos que devem ser lidos. */
    List<String> planos();

    /**
     * Um plano lido: nomes das etiquetas (category1 a category25), nomes dos buckets e as tarefas.
     */
    record PlanoPlanner(String planId, Map<String, String> etiquetas, Map<String, String> buckets, List<TarefaPlanner> tarefas) {
    }

    /**
     * @param etiquetasAplicadas as chaves das etiquetas ligadas na tarefa (category1, category5...)
     */
    record TarefaPlanner(String id, String titulo, String bucketId, OffsetDateTime criadaEm, int percentualConcluido,
                         List<String> etiquetasAplicadas) {
    }
}
