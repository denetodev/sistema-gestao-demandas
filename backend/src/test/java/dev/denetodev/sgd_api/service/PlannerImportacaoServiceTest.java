package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.response.PlannerSincronizacaoResponse;
import dev.denetodev.sgd_api.entity.Area;
import dev.denetodev.sgd_api.entity.PerfilPessoa;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.planner.PlannerExcelLeitor;
import dev.denetodev.sgd_api.planner.PlannerFonte.PlanoPlanner;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import dev.denetodev.sgd_api.service.support.PermissaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Quem pode enviar a planilha do Planner (Admin, Gestor e Referência de equipe) e o que é recusado antes de ler. */
class PlannerImportacaoServiceTest {

    private CurrentPessoaResolver resolver;
    private PlannerExcelLeitor leitor;
    private PlannerSyncService sync;
    private PlannerImportacaoService service;
    private final Jwt jwt = mock(Jwt.class);

    @BeforeEach
    void preparar() {
        resolver = mock(CurrentPessoaResolver.class);
        leitor = mock(PlannerExcelLeitor.class);
        sync = mock(PlannerSyncService.class);
        service = new PlannerImportacaoService(resolver, new PermissaoService(mock(dev.denetodev.sgd_api.repository.PessoaDemandaRepository.class)), leitor, sync);
    }

    private Pessoa pessoa(PerfilPessoa perfil, boolean referencia) {
        Pessoa p = new Pessoa("Fulano", mock(Area.class));
        p.setPerfil(perfil);
        if (referencia) {
            p.setReferenciaArea(mock(Area.class));
        }
        when(resolver.resolver(jwt)).thenReturn(p);
        return p;
    }

    private MockMultipartFile xlsx() {
        return new MockMultipartFile("arquivo", "Construção HOUSE.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[]{1, 2, 3});
    }

    @Test
    void adminGestorEReferenciaEnviam() {
        PlanoPlanner plano = new PlanoPlanner("planilha", Map.of(), Map.of(), List.of());
        PlannerSincronizacaoResponse resposta = new PlannerSincronizacaoResponse(5, 2, 1, List.of(), List.of());
        when(leitor.ler(any())).thenReturn(plano);
        when(sync.importar(plano)).thenReturn(resposta);

        for (Pessoa quem : List.of(pessoa(PerfilPessoa.ADMIN, false), pessoa(PerfilPessoa.GESTOR, false), pessoa(PerfilPessoa.PROFISSIONAL, true))) {
            when(resolver.resolver(jwt)).thenReturn(quem);
            assertSame(resposta, service.importar(jwt, xlsx()));
        }
    }

    @Test
    void profissionalComumEVisualizadorNaoEnviam() {
        for (Pessoa quem : List.of(pessoa(PerfilPessoa.PROFISSIONAL, false), pessoa(PerfilPessoa.VISUALIZADOR, false))) {
            when(resolver.resolver(jwt)).thenReturn(quem);
            assertThrows(AccessDeniedException.class, () -> service.importar(jwt, xlsx()));
        }
        verifyNoInteractions(leitor, sync);
    }

    @Test
    void recusaArquivoVazioOuQueNaoEXlsxSemLer() {
        pessoa(PerfilPessoa.GESTOR, false);

        assertThrows(IllegalArgumentException.class,
                () -> service.importar(jwt, new MockMultipartFile("arquivo", "a.xlsx", "x", new byte[0])));
        assertThrows(IllegalArgumentException.class,
                () -> service.importar(jwt, new MockMultipartFile("arquivo", "plano.csv", "text/csv", new byte[]{1})));
        verifyNoInteractions(leitor, sync);
    }
}
