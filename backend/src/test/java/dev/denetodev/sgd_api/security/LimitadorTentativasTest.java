package dev.denetodev.sgd_api.security;

import dev.denetodev.sgd_api.exception.TentativasExcedidasException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LimitadorTentativasTest {

    /** Relógio que a gente avança na mão. */
    private static class RelogioManual extends Clock {
        private Instant agora = Instant.parse("2026-10-13T12:00:00Z");

        void avancar(Duration d) { agora = agora.plus(d); }

        @Override public java.time.ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zone) { return this; }
        @Override public Instant instant() { return agora; }
    }

    @Test
    void barraDepoisDoLimiteEVoltaQuandoAJanelaPassa() {
        RelogioManual relogio = new RelogioManual();
        LimitadorTentativas limitador = new LimitadorTentativas(2, Duration.ofHours(1), relogio);

        limitador.registrar("conta-a");
        limitador.registrar("conta-a");
        assertThrows(TentativasExcedidasException.class, () -> limitador.registrar("conta-a"));

        relogio.avancar(Duration.ofMinutes(61));
        assertDoesNotThrow(() -> limitador.registrar("conta-a"));
    }

    @Test
    void cadaChaveTemOSeuProprioLimite() {
        LimitadorTentativas limitador = new LimitadorTentativas(1, Duration.ofHours(1), new RelogioManual());

        limitador.registrar("conta-a");
        assertDoesNotThrow(() -> limitador.registrar("conta-b"));
        assertThrows(TentativasExcedidasException.class, () -> limitador.registrar("conta-a"));
    }
}
