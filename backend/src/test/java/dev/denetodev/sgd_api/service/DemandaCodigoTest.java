package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.DemandaRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** O código da demanda é livre (a critério de quem cria): só se limpa espaço e se limita o tamanho. */
class DemandaCodigoTest {

    @Test
    void codigoSemEspacosNasPontasEVazioViraNulo() {
        assertEquals("BC2609AX", DemandaService.normalizarCodigo("  BC2609AX "));
        assertEquals("OS 123/26", DemandaService.normalizarCodigo("OS 123/26")); // formato livre
        assertNull(DemandaService.normalizarCodigo("   "));
        assertNull(DemandaService.normalizarCodigo(""));
        assertNull(DemandaService.normalizarCodigo(null));
    }

    @Test
    void codigoComMaisDe40CaracteresEhRecusadoNaValidacaoDaRequisicao() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

        assertTrue(validator.validate(requisicao("BC2609AX")).isEmpty());
        assertTrue(validator.validate(requisicao(null)).isEmpty());
        assertFalse(validator.validate(requisicao("x".repeat(41))).isEmpty());
    }

    private DemandaRequest requisicao(String codigo) {
        return new DemandaRequest("Título", null, codigo, UUID.randomUUID(), null, null, null, null, null, null, null);
    }
}
