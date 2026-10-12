package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.AtividadeRequest;
import dev.denetodev.sgd_api.dto.request.DemandaRequest;
import dev.denetodev.sgd_api.dto.request.PecaRequest;
import dev.denetodev.sgd_api.entity.Area;
import dev.denetodev.sgd_api.entity.Demanda;
import dev.denetodev.sgd_api.entity.Diretoria;
import dev.denetodev.sgd_api.entity.PerfilPessoa;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.entity.PessoaDemanda;
import dev.denetodev.sgd_api.entity.StatusDemanda;
import dev.denetodev.sgd_api.repository.*;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import dev.denetodev.sgd_api.service.support.PermissaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Page;
import dev.denetodev.sgd_api.dto.response.DemandaResponse;
import dev.denetodev.sgd_api.entity.EscopoListagem;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Os serviços de fato aplicam "só quem participa da demanda altera algo nela", usando o
 * PermissaoService de verdade (a regra em si está em AlteracaoDeDemandaTest).
 */
class AlteracaoDeDemandaServicosTest {

    private final UUID demandaId = UUID.randomUUID();

    private PessoaDemandaRepository vinculos;
    private PessoaRepository pessoaRepository;
    private DemandaRepository demandaRepository;
    private CurrentPessoaResolver resolver;
    private DemandaService demandaService;
    private AtividadeService atividadeService;
    private PecaService pecaService;
    private Pessoa ana;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        vinculos = mock(PessoaDemandaRepository.class);
        pessoaRepository = mock(PessoaRepository.class);
        demandaRepository = mock(DemandaRepository.class);
        resolver = mock(CurrentPessoaResolver.class);
        jwt = mock(Jwt.class);
        PermissaoService permissao = new PermissaoService(vinculos);

        demandaService = new DemandaService(
                demandaRepository, mock(DiretoriaRepository.class), mock(DemandanteRepository.class),
                mock(ProjetoRepository.class), mock(CampanhaRepository.class), mock(PecaRepository.class),
                vinculos, resolver, permissao, mock(AuditoriaRepository.class), pessoaRepository);
        atividadeService = new AtividadeService(mock(AtividadeRepository.class), demandaRepository,
                mock(TipoAtividadeRepository.class), pessoaRepository, resolver, permissao);
        pecaService = new PecaService(mock(PecaRepository.class), demandaRepository,
                mock(TipoPecaRepository.class), pessoaRepository, resolver, permissao);

        Diretoria diretoria = mock(Diretoria.class);
        when(diretoria.getId()).thenReturn(UUID.randomUUID());
        Demanda demanda = new Demanda("Campanha X", diretoria);
        when(demandaRepository.findById(demandaId)).thenReturn(Optional.of(demanda));

        Area area = mock(Area.class);
        when(area.getId()).thenReturn(UUID.randomUUID());
        ana = mock(Pessoa.class);
        when(ana.getId()).thenReturn(UUID.randomUUID());
        when(ana.getPerfil()).thenReturn(PerfilPessoa.PROFISSIONAL);
        when(ana.getArea()).thenReturn(area);
        when(resolver.resolver(any())).thenReturn(ana);
        when(pessoaRepository.findById(ana.getId())).thenReturn(Optional.of(ana));
        when(vinculos.findByDemandaId(demandaId)).thenReturn(List.of()); // time vazio: Ana é de fora
    }

    private DemandaRequest edicao() {
        return new DemandaRequest("Novo título", null, null, UUID.randomUUID(), null, null, null, null, null, null, null);
    }

    @Test
    void foraDoTimeNaoEditaADemanda() {
        assertThrows(AccessDeniedException.class, () -> demandaService.atualizar(jwt, demandaId, edicao()));
    }

    @Test
    void foraDoTimeNaoMudaOStatus() {
        assertThrows(AccessDeniedException.class,
                () -> demandaService.atualizarStatus(jwt, demandaId, StatusDemanda.EM_ANDAMENTO, null));
    }

    @Test
    void foraDoTimeNaoLancaAtividadeNaDemanda() {
        AtividadeRequest req = new AtividadeRequest(demandaId, UUID.randomUUID(), ana.getId(), "x", null);
        assertThrows(AccessDeniedException.class, () -> atividadeService.criar(jwt, req));
    }

    @Test
    void foraDoTimeNaoLancaPecaNaDemanda() {
        PecaRequest req = new PecaRequest(demandaId, UUID.randomUUID(), "Peça", null, 1, ana.getId(), null);
        assertThrows(AccessDeniedException.class, () -> pecaService.criar(jwt, req));
    }

    @Test
    void atividadeSemDemandaNaoDependeDeTime() {
        AtividadeRequest req = new AtividadeRequest(null, UUID.randomUUID(), ana.getId(), "avulsa", null);
        try {
            atividadeService.criar(jwt, req);
        } catch (RuntimeException ignorada) {
            // o resto do fluxo usa repositórios falsos; só importa que não consultou o time da demanda
        }
        verify(vinculos, never()).findByDemandaId(any());
    }

    @Test
    void participanteEditaADemanda() {
        PessoaDemanda vinculo = new PessoaDemanda(ana, demandaRepository.findById(demandaId).orElseThrow());
        when(vinculos.findByDemandaId(demandaId)).thenReturn(List.of(vinculo));
        when(demandaRepository.findById(demandaId)).thenReturn(Optional.of(vinculo.getDemanda()));

        // não lança AccessDeniedException: o 404 do diretório inexistente mostra que passou da regra
        Throwable erro = null;
        try {
            demandaService.atualizar(jwt, demandaId, edicao());
        } catch (RuntimeException e) {
            erro = e;
        }
        if (erro instanceof AccessDeniedException) {
            throw (AccessDeniedException) erro;
        }
    }

    // ---- campo podeAlterar (a interface usa para esconder ou mostrar os botões) ----

    private Demanda demandaComId() {
        Diretoria diretoria = mock(Diretoria.class);
        when(diretoria.getId()).thenReturn(UUID.randomUUID());
        Demanda d = spy(new Demanda("Campanha X", diretoria));
        doReturn(demandaId).when(d).getId();
        when(demandaRepository.findById(demandaId)).thenReturn(Optional.of(d));
        return d;
    }

    @Test
    void detalheDizQueQuemEstaForaDoTimeNaoPodeAlterar() {
        demandaComId();
        assertFalse(demandaService.buscarPorId(jwt, demandaId).podeAlterar());
    }

    @Test
    void detalheDizQueParticipantePodeAlterar() {
        Demanda d = demandaComId();
        when(vinculos.findByDemandaId(demandaId)).thenReturn(List.of(new PessoaDemanda(ana, d)));
        assertTrue(demandaService.buscarPorId(jwt, demandaId).podeAlterar());
    }

    @Test
    void listaMarcaPodeAlterarSoNasDemandasDoTime() {
        Demanda d = demandaComId();
        Diretoria dirDaAna = mock(Diretoria.class);
        when(dirDaAna.getId()).thenReturn(UUID.randomUUID());
        when(ana.getArea().getDiretoria()).thenReturn(dirDaAna);
        when(demandaRepository.findByDiretoriaId(any(), any()))
                .thenReturn(new PageImpl<>(List.of(d), PageRequest.of(0, 20), 1));

        when(vinculos.findByDemandaIdIn(any())).thenReturn(List.of());
        Page<DemandaResponse> foraDoTime = demandaService.listarComEscopo(jwt, EscopoListagem.DIRETORIA, PageRequest.of(0, 20));
        assertFalse(foraDoTime.getContent().get(0).podeAlterar());

        when(vinculos.findByDemandaIdIn(any())).thenReturn(List.of(new PessoaDemanda(ana, d)));
        Page<DemandaResponse> noTime = demandaService.listarComEscopo(jwt, EscopoListagem.DIRETORIA, PageRequest.of(0, 20));
        assertTrue(noTime.getContent().get(0).podeAlterar());
    }
}
