package dev.denetodev.sgd_api.dto.response;

/**
 * Contadores para o aviso no menu. Quem opera ou gerencia vê tudo; quem só consulta recebe só os atrasados
 * (é o que ele enxerga na lista de "quem está com quê").
 */
public record ResumoEmprestimosResponse(long aguardandoAutorizacao, long aguardandoExterno, long atrasados) {
}
