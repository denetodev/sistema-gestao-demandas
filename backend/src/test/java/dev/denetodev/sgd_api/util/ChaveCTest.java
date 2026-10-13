package dev.denetodev.sgd_api.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChaveCTest {

    @Test
    void normalizaEspacosEMaiuscula() {
        assertEquals("C1349844", ChaveC.normalizar("  c1349844 "));
        assertNull(ChaveC.normalizar("   "));
        assertNull(ChaveC.normalizar(null));
    }

    @Test
    void aceitaCouFseguidosDeSeteDigitos() {
        assertTrue(ChaveC.valido("C1349864"));
        assertTrue(ChaveC.valido("F0000001"));
    }

    @Test
    void recusaQualquerOutroFormato() {
        assertFalse(ChaveC.valido("c1349864")); // precisa vir normalizada
        assertFalse(ChaveC.valido("X1349864"));
        assertFalse(ChaveC.valido("C134986"));
        assertFalse(ChaveC.valido("C13498645"));
        assertFalse(ChaveC.valido("CC349864"));
        assertFalse(ChaveC.valido("12345678909"));
        assertFalse(ChaveC.valido(null));
    }
}
