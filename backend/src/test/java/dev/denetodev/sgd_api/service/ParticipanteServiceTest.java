package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.AtualizarPapelRequest;
import dev.denetodev.sgd_api.dto.request.ParticipanteRequest;
import dev.denetodev.sgd_api.dto.response.ParticipanteResponse;
import dev.denetodev.sgd_api.entity.*;
import dev.denetodev.sgd_api.repository.DemandaRepository;
import dev.denetodev.sgd_api.repository.PessoaDemandaRepository;
import dev.denetodev.sgd_api.repository.PessoaRepository;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ParticipanteServiceTest {

    private final UUID demandaId = UUID.randomUUID();

    private DemandaRepository demandaRepository;
    private PessoaRepository pessoaRepository;
    private PessoaDemandaRepository vinculos;
    private ParticipanteService service;

    private Area design;
    private Area html;

    @BeforeEach
    void setUp() {
        demandaRepository = mock(DemandaRepository.class);
        pessoaRepository = mock(PessoaRepository.class);
        vinculos = mock(PessoaDemandaRepository.class);
        service = new ParticipanteService(demandaRepository, pessoaRepository, vinculos, mock(CurrentPessoaResolver.class));

        Demanda demanda = mock(Demanda.class);
        when(demanda.getId()).thenReturn(demandaId);
        when(demandaRepository.findById(demandaId)).thenReturn(Optional.of(demanda));
        when(vinculos.save(any(PessoaDemanda.class))).thenAnswer(inv -> inv.getArgument(0));
        when(vinculos.findByPessoaIdAndDemandaIdAndDataSaidaIsNull(any(), any())).thenReturn(Optional.empty());

        design = area();
        html = area();
    }

    @Test
    void profissionalComumSoMudaOProprioPapel() {
        Pessoa ana = pessoa(PerfilPessoa.PROFISSIONAL, design, null);
        Pessoa bruno = pessoa(PerfilPessoa.PROFISSIONAL, design, null);
        PessoaDemanda dela = vinculo(ana, PapelPessoaDemanda.PARTICIPANTE);
        PessoaDemanda dele = vinculo(bruno, PapelPessoaDemanda.PARTICIPANTE);

        ParticipanteResponse r = service.atualizarPapel(ana, demandaId, dela.getId(), new AtualizarPapelRequest(PapelPessoaDemanda.REVISOR));
        assertEquals(PapelPessoaDemanda.REVISOR, r.papel());

        assertThrows(AccessDeniedException.class,
                () -> service.atualizarPapel(ana, demandaId, dele.getId(), new AtualizarPapelRequest(PapelPessoaDemanda.REVISOR)));
    }

    @Test
    void ninguemSeTornaResponsavelPrincipalSozinho() {
        Pessoa ana = pessoa(PerfilPessoa.PROFISSIONAL, design, null);
        PessoaDemanda dela = vinculo(ana, PapelPessoaDemanda.PARTICIPANTE);

        assertThrows(AccessDeniedException.class, () -> service.atualizarPapel(
                ana, demandaId, dela.getId(), new AtualizarPapelRequest(PapelPessoaDemanda.RESPONSAVEL_PRINCIPAL)));
    }

    @Test
    void responsavelPrincipalGerenciaQualquerUmEPassaOPapelAdiante() {
        Pessoa lider = pessoa(PerfilPessoa.PROFISSIONAL, design, null);
        Pessoa bruno = pessoa(PerfilPessoa.PROFISSIONAL, html, null);
        PessoaDemanda doLider = vinculo(lider, PapelPessoaDemanda.RESPONSAVEL_PRINCIPAL);
        PessoaDemanda dele = vinculo(bruno, PapelPessoaDemanda.PARTICIPANTE);
        when(vinculos.findByPessoaIdAndDemandaIdAndDataSaidaIsNull(lider.getId(), demandaId)).thenReturn(Optional.of(doLider));

        ParticipanteResponse r = service.atualizarPapel(lider, demandaId, dele.getId(),
                new AtualizarPapelRequest(PapelPessoaDemanda.RESPONSAVEL_PRINCIPAL));

        assertEquals(PapelPessoaDemanda.RESPONSAVEL_PRINCIPAL, r.papel());
    }

    @Test
    void referenciaGerenciaSoPessoasDaPropriaArea() {
        Pessoa ref = pessoa(PerfilPessoa.PROFISSIONAL, design, design);
        Pessoa colega = pessoa(PerfilPessoa.PROFISSIONAL, design, null);
        Pessoa deOutraArea = pessoa(PerfilPessoa.PROFISSIONAL, html, null);
        PessoaDemanda vColega = vinculo(colega, PapelPessoaDemanda.APOIO);
        PessoaDemanda vOutra = vinculo(deOutraArea, PapelPessoaDemanda.APOIO);

        assertDoesNotThrow(() -> service.remover(ref, demandaId, vColega.getId()));
        assertNotNull(vColega.getDataSaida());
        assertThrows(AccessDeniedException.class, () -> service.remover(ref, demandaId, vOutra.getId()));
    }

    @Test
    void referenciaAdicionaPessoaDaPropriaAreaMasNaoDeOutra() {
        Pessoa ref = pessoa(PerfilPessoa.PROFISSIONAL, design, design);
        Pessoa colega = pessoa(PerfilPessoa.PROFISSIONAL, design, null);
        Pessoa deOutraArea = pessoa(PerfilPessoa.PROFISSIONAL, html, null);
        when(pessoaRepository.findById(colega.getId())).thenReturn(Optional.of(colega));
        when(pessoaRepository.findById(deOutraArea.getId())).thenReturn(Optional.of(deOutraArea));

        assertNotNull(service.adicionar(ref, demandaId, new ParticipanteRequest(colega.getId(), PapelPessoaDemanda.APOIO)));
        assertThrows(AccessDeniedException.class,
                () -> service.adicionar(ref, demandaId, new ParticipanteRequest(deOutraArea.getId(), PapelPessoaDemanda.APOIO)));
    }

    @Test
    void gestorFazTudoEVisualizadorNada() {
        Pessoa gestor = pessoa(PerfilPessoa.GESTOR, design, null);
        Pessoa vis = pessoa(PerfilPessoa.VISUALIZADOR, design, null);
        Pessoa bruno = pessoa(PerfilPessoa.PROFISSIONAL, html, null);
        PessoaDemanda dele = vinculo(bruno, PapelPessoaDemanda.PARTICIPANTE);
        PessoaDemanda doVis = vinculo(vis, PapelPessoaDemanda.PARTICIPANTE);

        assertEquals(PapelPessoaDemanda.RESPONSAVEL_PRINCIPAL, service.atualizarPapel(
                gestor, demandaId, dele.getId(), new AtualizarPapelRequest(PapelPessoaDemanda.RESPONSAVEL_PRINCIPAL)).papel());
        assertThrows(AccessDeniedException.class, () -> service.remover(vis, demandaId, doVis.getId()));
    }

    // ---- helpers ----

    private static Area area() {
        Area a = mock(Area.class);
        when(a.getId()).thenReturn(UUID.randomUUID());
        return a;
    }

    private static Pessoa pessoa(PerfilPessoa perfil, Area area, Area referencia) {
        Pessoa p = mock(Pessoa.class);
        when(p.getId()).thenReturn(UUID.randomUUID());
        when(p.getNome()).thenReturn("Pessoa");
        when(p.getPerfil()).thenReturn(perfil);
        when(p.getArea()).thenReturn(area);
        when(p.getReferenciaArea()).thenReturn(referencia);
        return p;
    }

    /** Vínculo real (para o papel mudar de verdade) com id fixo no repositório. */
    private PessoaDemanda vinculo(Pessoa pessoa, PapelPessoaDemanda papel) {
        Demanda demanda = demandaRepository.findById(demandaId).orElseThrow();
        PessoaDemanda real = new PessoaDemanda(pessoa, demanda);
        real.setPapel(papel);
        PessoaDemanda espiao = org.mockito.Mockito.spy(real);
        UUID id = UUID.randomUUID();
        org.mockito.Mockito.doReturn(id).when(espiao).getId();
        when(vinculos.findById(id)).thenReturn(Optional.of(espiao));
        return espiao;
    }
}
