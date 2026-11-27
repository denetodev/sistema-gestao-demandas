package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.DemandaRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import dev.denetodev.sgd_api.entity.Demanda;
import dev.denetodev.sgd_api.entity.StatusDemanda;
import dev.denetodev.sgd_api.repository.*;
import java.util.List;
import java.util.UUID;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

    @Test
    void porCodigoAchaAsDemandasQueJaUsamOCodigoEIgnoraCodigoVazio() {
        DemandaRepository repo = mock(DemandaRepository.class);
        Demanda existente = mock(Demanda.class);
        UUID id = UUID.randomUUID();
        when(existente.getId()).thenReturn(id);
        when(existente.getCodigo()).thenReturn("BC2611EG");
        when(existente.getTitulo()).thenReturn("CUSTEIO_26_27");
        when(existente.getStatus()).thenReturn(StatusDemanda.CONCLUIDA);
        when(repo.findByCodigoIgnoreCase("BC2611EG")).thenReturn(List.of(existente));
        DemandaService service = new DemandaService(repo, mock(DiretoriaRepository.class), mock(DemandanteRepository.class),
                mock(ProjetoRepository.class), mock(CampanhaRepository.class), mock(PecaRepository.class),
                mock(PessoaDemandaRepository.class), null, null, mock(AuditoriaRepository.class), mock(PessoaRepository.class));

        var achadas = service.porCodigo("  bc2611eg ".toUpperCase().trim());

        assertEquals(1, achadas.size());
        assertEquals(id, achadas.get(0).id());
        assertEquals("CUSTEIO_26_27", achadas.get(0).titulo());
        assertTrue(service.porCodigo("   ").isEmpty());
        assertTrue(service.porCodigo(null).isEmpty());
    }

    private DemandaRequest requisicao(String codigo) {
        return new DemandaRequest("Título", null, codigo, UUID.randomUUID(), null, null, null, null, null, null, null, null);
    }
}
