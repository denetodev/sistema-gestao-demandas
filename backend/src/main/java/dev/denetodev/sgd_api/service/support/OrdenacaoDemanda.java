package dev.denetodev.sgd_api.service.support;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Set;

/**
 * Página e ordenação da lista de demandas: só ordena por colunas conhecidas (um nome inventado na URL
 * não derruba a requisição), por padrão as mais recentes primeiro, e limita o tamanho da página.
 */
public final class OrdenacaoDemanda {

    public static final int TAMANHO_PADRAO = 10;
    public static final int TAMANHO_MAXIMO = 100;

    private static final Set<String> PERMITIDAS =
            Set.of("dataCriacao", "createdAt", "titulo", "dataPrazo", "status", "prioridade");

    private OrdenacaoDemanda() {
    }

    public static Pageable sanear(Pageable pedido) {
        if (pedido == null || pedido.isUnpaged()) {
            return PageRequest.of(0, TAMANHO_PADRAO, padrao());
        }
        int tamanho = Math.max(1, Math.min(pedido.getPageSize(), TAMANHO_MAXIMO));
        List<Sort.Order> ordens = pedido.getSort().stream().filter(o -> PERMITIDAS.contains(o.getProperty())).toList();
        Sort sort = ordens.isEmpty() ? padrao() : Sort.by(ordens);
        if (sort.getOrderFor("createdAt") == null) {
            sort = sort.and(Sort.by(Sort.Direction.DESC, "createdAt")); // desempate estável entre páginas
        }
        return PageRequest.of(pedido.getPageNumber(), tamanho, sort);
    }

    private static Sort padrao() {
        return Sort.by(Sort.Order.desc("dataCriacao"), Sort.Order.desc("createdAt"));
    }
}
