package dev.denetodev.sgd_api.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CpfTest {

    @Test
    void normalizaRemovendoPontuacao() {
        assertEquals("52998224725", Cpf.normalizar("529.982.247-25"));
        assertNull(Cpf.normalizar("  "));
        assertNull(Cpf.normalizar(null));
    }

    @Test
    void aceitaCpfComDigitosVerificadoresCorretos() {
        assertTrue(Cpf.valido("52998224725"));
        assertTrue(Cpf.valido("11144477735"));
    }

    @Test
    void rejeitaDigitoErradoSequenciaRepetidaETamanhoErrado() {
        assertFalse(Cpf.valido("52998224726"));
        assertFalse(Cpf.valido("11111111111"));
        assertFalse(Cpf.valido("5299822472"));
        assertFalse(Cpf.valido("abc"));
        assertFalse(Cpf.valido(null));
    }

    @Test
    void mascaraEscondeOInicioEOFim() {
        assertEquals("***.982.247-**", Cpf.mascarar("52998224725"));
        assertNull(Cpf.mascarar(null));
        assertNull(Cpf.mascarar("123"));
    }
}
