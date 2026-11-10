package dev.denetodev.sgd_api.service.support;

import dev.denetodev.sgd_api.service.support.SemanasDoMes.Semana;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SemanasDoMesTest {

    @Test
    void agosto2026ComecaNoSabadoEEntraNaPrimeiraSemanaUtil() {
        List<Semana> s = SemanasDoMes.de(YearMonth.of(2026, 8));

        assertEquals(5, s.size());
        assertEquals(LocalDate.of(2026, 8, 1), s.get(0).de());
        assertEquals(LocalDate.of(2026, 8, 9), s.get(0).ate());
        assertEquals(LocalDate.of(2026, 8, 31), s.get(4).ate());
        assertEquals(LocalDate.of(2026, 8, 24), s.get(3).de());
    }

    @Test
    void outubro2026TemCincoSemanasCortadasNasPontas() {
        List<Semana> s = SemanasDoMes.de(YearMonth.of(2026, 10));

        assertEquals(5, s.size());
        assertEquals(LocalDate.of(2026, 10, 1), s.get(0).de());
        assertEquals(LocalDate.of(2026, 10, 4), s.get(0).ate());
        assertEquals(LocalDate.of(2026, 10, 26), s.get(4).de());
        assertEquals(LocalDate.of(2026, 10, 31), s.get(4).ate());
    }

    @Test
    void fimDeSemanaSoNoFinalDoMesSeJuntaAUltimaSemana() {
        // maio de 2026 termina num domingo; novembro de 2025 termina num domingo; fevereiro de 2026 fecha no sábado
        List<Semana> s = SemanasDoMes.de(YearMonth.of(2025, 11));

        assertEquals(LocalDate.of(2025, 11, 30), s.get(s.size() - 1).ate());
        assertEquals(LocalDate.of(2025, 11, 24), s.get(s.size() - 1).de());
    }

    @Test
    void cadaDiaDoMesPertenceAUmaSemana() {
        YearMonth mes = YearMonth.of(2026, 3);
        List<Semana> semanas = SemanasDoMes.de(mes);
        for (int d = 1; d <= mes.lengthOfMonth(); d++) {
            LocalDate dia = mes.atDay(d);
            assertEquals(1, semanas.stream().filter(sm -> sm.contem(dia)).count(), dia.toString());
        }
    }
}
