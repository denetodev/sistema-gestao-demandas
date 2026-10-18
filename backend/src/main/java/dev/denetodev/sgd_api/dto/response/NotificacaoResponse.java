package dev.denetodev.sgd_api.dto.response;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * O que o sininho mostra. {@code naoLidas} soma as quantidades dos itens ainda sem ler: os avisos de pendência
 * (cadastros, empréstimos) continuam não lidos enquanto houver o que fazer; os de "te adicionaram" ficam lidos
 * depois que a pessoa abre o sininho.
 */
public record NotificacaoResponse(List<Item> itens, long naoLidas) {

    public enum Tipo { CADASTROS_PENDENTES, EMPRESTIMOS_AGUARDANDO, EMPRESTIMOS_ATRASADOS, ADICIONADO_EM_DEMANDA }

    public record Item(String id, Tipo tipo, String titulo, String texto, String link, long quantidade, boolean lida,
                       OffsetDateTime em) {
    }
}
