package dev.denetodev.sgd_api.service.support;

import dev.denetodev.sgd_api.entity.Emprestimo;
import dev.denetodev.sgd_api.entity.EmprestimoItem;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Conta quanto de um equipamento está ocupado dentro de um período. Um uso ocupa de {@code inicio} (inclusive)
 * até {@code fim} (exclusive), então devolver às 12h e retirar às 12h não conflitam.
 *
 * Para lote (por quantidade) o que vale é o PICO: o maior total ocupado ao mesmo tempo, não a soma de tudo
 * que encosta no período. Retirado e vencido não tem fim: segue com a pessoa até devolver, então bloqueia
 * qualquer reserva futura daquele equipamento.
 */
public final class DisponibilidadePeriodo {

    private DisponibilidadePeriodo() {
    }

    public record Uso(UUID emprestimoId, Instant inicio, Instant fim, int quantidade) {
        boolean encosta(Instant de, Instant ate) {
            return inicio.isBefore(ate) && fim.isAfter(de);
        }
    }

    /** Transforma um item de empréstimo em uso; o fim some (Instant.MAX) quando está retirado e já venceu. */
    public static Uso de(EmprestimoItem item, OffsetDateTime agora) {
        Emprestimo e = item.getEmprestimo();
        return new Uso(e.getId(), e.inicioOcupacao().toInstant(), e.fimOcupacao(agora), item.getQuantidade());
    }

    /** Maior quantidade ocupada ao mesmo tempo em [de, ate). */
    public static int pico(Collection<Uso> usos, Instant de, Instant ate) {
        List<Uso> relevantes = usos.stream().filter(u -> u.encosta(de, ate)).toList();
        if (relevantes.isEmpty()) {
            return 0;
        }
        List<Instant> pontos = new ArrayList<>();
        pontos.add(de);
        relevantes.stream().map(Uso::inicio).filter(i -> i.isAfter(de) && i.isBefore(ate)).forEach(pontos::add);
        int maior = 0;
        for (Instant p : pontos) {
            int naquele = 0;
            for (Uso u : relevantes) {
                if (!u.inicio().isAfter(p) && u.fim().isAfter(p)) {
                    naquele += u.quantidade();
                }
            }
            maior = Math.max(maior, naquele);
        }
        return maior;
    }

    /** Quanto sobra em todo o período (o pior momento manda). Nunca negativo. */
    public static int livre(int total, Collection<Uso> usos, Instant de, Instant ate) {
        return Math.max(0, total - pico(usos, de, ate));
    }

    /** O primeiro uso (por início) que encosta no período, para explicar o conflito a quem está pedindo. */
    public static Uso primeiroQueEncosta(Collection<Uso> usos, Instant de, Instant ate) {
        return usos.stream().filter(u -> u.encosta(de, ate)).min((a, b) -> a.inicio().compareTo(b.inicio())).orElse(null);
    }
}
