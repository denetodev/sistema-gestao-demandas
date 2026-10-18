package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.CampanhaRequest;
import dev.denetodev.sgd_api.dto.request.ProjetoRequest;
import dev.denetodev.sgd_api.entity.Area;
import dev.denetodev.sgd_api.entity.PerfilPessoa;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.repository.CampanhaRepository;
import dev.denetodev.sgd_api.repository.DemandanteRepository;
import dev.denetodev.sgd_api.repository.DiretoriaRepository;
import dev.denetodev.sgd_api.repository.ProjetoRepository;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import dev.denetodev.sgd_api.service.support.PermissaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Criar e editar projeto e campanha: Admin, Gestor e Referência de equipe; Profissional comum e Visualizador não. */
class PermissaoServiceProjetosECampanhasTest {

    private final PermissaoService permissao = new PermissaoService(null);

    private CurrentPessoaResolver resolver;
    private CampanhaRepository campanhaRepository;
    private ProjetoRepository projetoRepository;
    private CampanhaService campanhaService;
    private ProjetoService projetoService;
    private final Jwt jwt = mock(Jwt.class);

    @BeforeEach
    void setUp() {
        resolver = mock(CurrentPessoaResolver.class);
        campanhaRepository = mock(CampanhaRepository.class);
        projetoRepository = mock(ProjetoRepository.class);
        campanhaService = new CampanhaService(campanhaRepository, projetoRepository, resolver, permissao);
        projetoService = new ProjetoService(projetoRepository, mock(DemandanteRepository.class),
                mock(DiretoriaRepository.class), resolver, permissao);
        when(campanhaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(projetoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Pessoa pessoa(PerfilPessoa perfil, boolean referencia) {
        Area area = referencia ? mock(Area.class) : null; // criada antes: mock dentro de when() quebra o stubbing
        Pessoa p = mock(Pessoa.class);
        when(p.getPerfil()).thenReturn(perfil);
        when(p.getReferenciaArea()).thenReturn(area);
        return p;
    }

    @Test
    void adminGestorEReferenciaPodem() {
        assertTrue(permissao.podeGerenciarProjetosECampanhas(pessoa(PerfilPessoa.ADMIN, false)));
        assertTrue(permissao.podeGerenciarProjetosECampanhas(pessoa(PerfilPessoa.GESTOR, false)));
        assertTrue(permissao.podeGerenciarProjetosECampanhas(pessoa(PerfilPessoa.PROFISSIONAL, true)));
    }

    @Test
    void profissionalComumEVisualizadorNaoPodem() {
        assertFalse(permissao.podeGerenciarProjetosECampanhas(pessoa(PerfilPessoa.PROFISSIONAL, false)));
        assertFalse(permissao.podeGerenciarProjetosECampanhas(pessoa(PerfilPessoa.VISUALIZADOR, false)));
        // nem uma referência gravada num Visualizador abre a porta
        assertFalse(permissao.podeGerenciarProjetosECampanhas(pessoa(PerfilPessoa.VISUALIZADOR, true)));
    }

    @Test
    void profissionalComumNaoCriaNemEditaCampanha() {
        Pessoa usuario = pessoa(PerfilPessoa.PROFISSIONAL, false);
        when(resolver.resolver(jwt)).thenReturn(usuario);
        var pedido = new CampanhaRequest("Campanha", null, null, null, null);

        assertThrows(AccessDeniedException.class, () -> campanhaService.criar(jwt, pedido));
        assertThrows(AccessDeniedException.class, () -> campanhaService.atualizar(jwt, java.util.UUID.randomUUID(), pedido));
        verify(campanhaRepository, never()).save(any());
    }

    @Test
    void profissionalComumNaoCriaNemEditaProjeto() {
        Pessoa usuario = pessoa(PerfilPessoa.PROFISSIONAL, false);
        when(resolver.resolver(jwt)).thenReturn(usuario);
        var pedido = new ProjetoRequest("Projeto", null, null, null, null);

        assertThrows(AccessDeniedException.class, () -> projetoService.criar(jwt, pedido));
        assertThrows(AccessDeniedException.class, () -> projetoService.atualizar(jwt, java.util.UUID.randomUUID(), pedido));
        verify(projetoRepository, never()).save(any());
    }

    @Test
    void referenciaDeEquipeCriaCampanha() {
        Pessoa usuario = pessoa(PerfilPessoa.PROFISSIONAL, true);
        when(resolver.resolver(jwt)).thenReturn(usuario);

        var criada = campanhaService.criar(jwt, new CampanhaRequest("Campanha da referência", null, null, null, null));

        assertEquals("Campanha da referência", criada.nome());
    }
}
