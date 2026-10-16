package dev.denetodev.sgd_api.service.support;

import dev.denetodev.sgd_api.service.support.DisponibilidadePeriodo.Uso;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class DisponibilidadePeriodoTest {

    private static final Instant BASE = Instant.parse("2026-10-20T12:00:00Z");

    private static Instant h(int horas) {
        return BASE.plusSeconds(horas * 3600L);
    }

    private static Uso uso(int de, int ate, int quantidade) {
        return new Uso(UUID.randomUUID(), h(de), h(ate), quantidade);
    }

    @Test
    void semUsoTudoEstaLivre() {
        assertEquals(1, DisponibilidadePeriodo.livre(1, List.of(), h(0), h(2)));
        assertEquals(10, DisponibilidadePeriodo.livre(10, List.of(), h(0), h(2)));
    }

    @Test
    void usoQueSobrepoeOcupaAPecaIndividual() {
        assertEquals(0, DisponibilidadePeriodo.livre(1, List.of(uso(1, 5, 1)), h(0), h(2)));
        assertEquals(0, DisponibilidadePeriodo.livre(1, List.of(uso(1, 5, 1)), h(2), h(8)));
        assertEquals(0, DisponibilidadePeriodo.livre(1, List.of(uso(1, 5, 1)), h(2), h(3))); // dentro do uso
        assertEquals(0, DisponibilidadePeriodo.livre(1, List.of(uso(3, 4, 1)), h(0), h(8))); // uso dentro do pedido
    }

    @Test
    void periodosQueApenasSeEncostamNaoConflitam() {
        // devolve às 5h e outro retira às 5h: o fim é exclusivo
        assertEquals(1, DisponibilidadePeriodo.livre(1, List.of(uso(0, 5, 1)), h(5), h(8)));
        assertEquals(1, DisponibilidadePeriodo.livre(1, List.of(uso(5, 8, 1)), h(0), h(5)));
    }

    @Test
    void periodosSeparadosNaoConflitam() {
        assertEquals(1, DisponibilidadePeriodo.livre(1, List.of(uso(10, 12, 1)), h(0), h(2)));
    }

    @Test
    void loteContaOPicoESoNaoAsomaDeTudoQueEncosta() {
        // lote de 10: 4 unidades das 0 às 4 e 4 unidades das 4 às 8 nunca passam de 4 ao mesmo tempo
        List<Uso> usos = List.of(uso(0, 4, 4), uso(4, 8, 4));
        assertEquals(6, DisponibilidadePeriodo.livre(10, usos, h(0), h(8)));
        // 4 + 4 ao mesmo tempo: pico de 8
        List<Uso> juntos = List.of(uso(0, 8, 4), uso(2, 6, 4));
        assertEquals(2, DisponibilidadePeriodo.livre(10, juntos, h(0), h(8)));
        assertEquals(6, DisponibilidadePeriodo.livre(10, juntos, h(0), h(2)));
    }

    @Test
    void picoNoMeioDoPeriodoPedidoContaMesmoQueNaoSejaNoComeco() {
        List<Uso> usos = List.of(uso(3, 5, 7));
        assertEquals(3, DisponibilidadePeriodo.livre(10, usos, h(0), h(8)));
        assertEquals(10, DisponibilidadePeriodo.livre(10, usos, h(0), h(3)));
    }

    @Test
    void retiradoEVencidoNaoTemFimEBloqueiaOFuturo() {
        Uso semFim = new Uso(UUID.randomUUID(), h(-48), Instant.MAX, 1);
        assertEquals(0, DisponibilidadePeriodo.livre(1, List.of(semFim), h(100), h(110)));
    }

    @Test
    void nuncaDevolveNegativo() {
        assertEquals(0, DisponibilidadePeriodo.livre(1, List.of(uso(0, 4, 1), uso(0, 4, 1)), h(0), h(4)));
    }

    @Test
    void explicaOConflitoPeloPrimeiroUsoQueEncosta() {
        Uso tarde = uso(6, 8, 1);
        Uso cedo = uso(1, 3, 1);
        assertSame(cedo, DisponibilidadePeriodo.primeiroQueEncosta(List.of(tarde, cedo), h(0), h(9)));
        assertNull(DisponibilidadePeriodo.primeiroQueEncosta(List.of(tarde), h(0), h(2)));
    }
}
