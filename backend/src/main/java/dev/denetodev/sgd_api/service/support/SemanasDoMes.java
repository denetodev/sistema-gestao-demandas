package dev.denetodev.sgd_api.service.support;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * As semanas do relatório mensal (Semana 1, 2...). Cada semana vai de segunda a domingo, cortada nas pontas do mês.
 * Um pedaço só de sábado e domingo no começo do mês se junta à primeira semana útil, e no fim do mês à última,
 * para nunca existir uma "semana" sem dia de trabalho.
 */
public final class SemanasDoMes {

    public record Semana(int numero, LocalDate de, LocalDate ate) {
        public boolean contem(LocalDate dia) {
            return !dia.isBefore(de) && !dia.isAfter(ate);
        }
    }

    private SemanasDoMes() {
    }

    public static List<Semana> de(YearMonth mes) {
        List<LocalDate[]> blocos = new ArrayList<>();
        LocalDate inicio = mes.atDay(1);
        LocalDate fimDoMes = mes.atEndOfMonth();
        while (!inicio.isAfter(fimDoMes)) {
            LocalDate domingo = inicio.with(java.time.temporal.TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
            LocalDate fim = domingo.isAfter(fimDoMes) ? fimDoMes : domingo;
            blocos.add(new LocalDate[]{inicio, fim});
            inicio = fim.plusDays(1);
        }
        if (blocos.size() > 1 && soFimDeSemana(blocos.get(0))) {
            blocos.get(1)[0] = blocos.get(0)[0];
            blocos.remove(0);
        }
        if (blocos.size() > 1 && soFimDeSemana(blocos.get(blocos.size() - 1))) {
            blocos.get(blocos.size() - 2)[1] = blocos.get(blocos.size() - 1)[1];
            blocos.remove(blocos.size() - 1);
        }
        List<Semana> semanas = new ArrayList<>();
        for (int i = 0; i < blocos.size(); i++) {
            semanas.add(new Semana(i + 1, blocos.get(i)[0], blocos.get(i)[1]));
        }
        return semanas;
    }

    /** A semana do dia dado, ou a última se ele estiver fora (não deveria acontecer dentro do mês). */
    public static Semana daData(List<Semana> semanas, LocalDate dia) {
        return semanas.stream().filter(s -> s.contem(dia)).findFirst().orElse(semanas.get(semanas.size() - 1));
    }

    private static boolean soFimDeSemana(LocalDate[] bloco) {
        for (LocalDate d = bloco[0]; !d.isAfter(bloco[1]); d = d.plusDays(1)) {
            if (d.getDayOfWeek() != DayOfWeek.SATURDAY && d.getDayOfWeek() != DayOfWeek.SUNDAY) {
                return false;
            }
        }
        return true;
    }
}
