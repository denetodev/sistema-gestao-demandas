package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.entity.Demanda;
import dev.denetodev.sgd_api.entity.Diretoria;
import dev.denetodev.sgd_api.entity.PapelPessoaDemanda;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.entity.PessoaDemanda;
import dev.denetodev.sgd_api.entity.PlannerSugestao;
import dev.denetodev.sgd_api.exception.EstadoInvalidoException;
import dev.denetodev.sgd_api.repository.DemandaRepository;
import dev.denetodev.sgd_api.repository.DiretoriaRepository;
import dev.denetodev.sgd_api.repository.PessoaDemandaRepository;
import dev.denetodev.sgd_api.repository.PlannerSugestaoRepository;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Aceitar a sugestão cria a demanda (ou entra na que já existe) e só a própria pessoa decide. */
class PlannerSugestaoServiceTest {

    private PlannerSugestaoRepository sugestaoRepository;
    private DemandaRepository demandaRepository;
    private DiretoriaRepository diretoriaRepository;
    private PessoaDemandaRepository pessoaDemandaRepository;
    private PlannerSugestaoService service;
    private Pessoa carol;
    private Pessoa outra;
    private Diretoria crm;

    @BeforeEach
    void preparar() {
        sugestaoRepository = mock(PlannerSugestaoRepository.class);
        demandaRepository = mock(DemandaRepository.class);
        diretoriaRepository = mock(DiretoriaRepository.class);
        pessoaDemandaRepository = mock(PessoaDemandaRepository.class);
        EntityManager em = mock(EntityManager.class);
        Query consulta = mock(Query.class);
        when(em.createNativeQuery(anyString())).thenReturn(consulta);
        when(consulta.setParameter(anyString(), any())).thenReturn(consulta);
        service = new PlannerSugestaoService(mock(CurrentPessoaResolver.class), sugestaoRepository, demandaRepository,
                diretoriaRepository, pessoaDemandaRepository, em,
                Clock.fixed(Instant.parse("2026-11-03T12:00:00Z"), ZoneOffset.UTC), "COE/CRM");
        carol = pessoa();
        outra = pessoa();
        crm = mock(Diretoria.class);
        when(diretoriaRepository.findByNomeIgnoreCase("COE/CRM")).thenReturn(Optional.of(crm));
        when(demandaRepository.findByCodigoIgnoreCase(anyString())).thenReturn(List.of());
        when(demandaRepository.save(any(Demanda.class))).thenAnswer(i -> i.getArgument(0));
    }

    private Pessoa pessoa() {
        Pessoa p = mock(Pessoa.class);
        when(p.getId()).thenReturn(UUID.randomUUID());
        return p;
    }

    private PlannerSugestao sugestao(Pessoa dona, String codigo) {
        PlannerSugestao s = new PlannerSugestao("plano1", "t1", dona, codigo, codigo + "_email_BBFriday", "HTML Entrada", "HTML - Carol", null);
        UUID id = UUID.randomUUID();
        org.springframework.test.util.ReflectionTestUtils.setField(s, "id", id);
        when(sugestaoRepository.findById(id)).thenReturn(Optional.of(s));
        return s;
    }

    @Test
    void aceitarSemDemandaNoSistemaCriaAPlannerComoResponsavelPrincipal() {
        PlannerSugestao s = sugestao(carol, "BC2610FN");

        service.aceitar(carol, s.getId());

        ArgumentCaptor<Demanda> criada = ArgumentCaptor.forClass(Demanda.class);
        verify(demandaRepository).save(criada.capture());
        assertEquals("BC2610FN", criada.getValue().getCodigo());
        assertEquals("planner", criada.getValue().getSourceSystem());
        ArgumentCaptor<PessoaDemanda> vinculo = ArgumentCaptor.forClass(PessoaDemanda.class);
        verify(pessoaDemandaRepository).save(vinculo.capture());
        assertEquals(PapelPessoaDemanda.RESPONSAVEL_PRINCIPAL, vinculo.getValue().getPapel());
        assertEquals(PlannerSugestao.Status.ACEITA, s.getStatus());
    }

    @Test
    void aceitarComDemandaJaExistenteSoColocaAPessoaNoTime() {
        Demanda existente = mock(Demanda.class);
        when(existente.getId()).thenReturn(UUID.randomUUID());
        when(demandaRepository.findByCodigoIgnoreCase("BC2610FN")).thenReturn(List.of(existente));
        PlannerSugestao s = sugestao(carol, "BC2610FN");

        service.aceitar(carol, s.getId());

        verify(demandaRepository, never()).save(any());
        ArgumentCaptor<PessoaDemanda> vinculo = ArgumentCaptor.forClass(PessoaDemanda.class);
        verify(pessoaDemandaRepository).save(vinculo.capture());
        assertEquals(PapelPessoaDemanda.PARTICIPANTE, vinculo.getValue().getPapel());
        assertSame(existente, s.getDemanda());
    }

    @Test
    void aceitarNaoDuplicaQuemJaEstaNoTime() {
        Demanda existente = mock(Demanda.class);
        when(existente.getId()).thenReturn(UUID.randomUUID());
        when(demandaRepository.findByCodigoIgnoreCase("BC2610FN")).thenReturn(List.of(existente));
        when(pessoaDemandaRepository.findByPessoaIdAndDemandaIdAndDataSaidaIsNull(carol.getId(), existente.getId()))
                .thenReturn(Optional.of(mock(PessoaDemanda.class)));
        PlannerSugestao s = sugestao(carol, "BC2610FN");

        service.aceitar(carol, s.getId());

        verify(pessoaDemandaRepository, never()).save(any());
        assertEquals(PlannerSugestao.Status.ACEITA, s.getStatus());
    }

    @Test
    void recusarSoMarcaADecisaoENaoCriaNada() {
        PlannerSugestao s = sugestao(carol, "BC2610FN");

        service.recusar(carol, s.getId());

        assertEquals(PlannerSugestao.Status.RECUSADA, s.getStatus());
        verify(demandaRepository, never()).save(any());
        verify(pessoaDemandaRepository, never()).save(any());
    }

    @Test
    void sugestaoDeOutraPessoaNaoPodeSerDecidida() {
        PlannerSugestao s = sugestao(carol, "BC2610FN");

        assertThrows(AccessDeniedException.class, () -> service.aceitar(outra, s.getId()));
        assertThrows(AccessDeniedException.class, () -> service.recusar(outra, s.getId()));
        assertEquals(PlannerSugestao.Status.PENDENTE, s.getStatus());
    }

    @Test
    void sugestaoJaDecididaNaoDecideDeNovo() {
        PlannerSugestao s = sugestao(carol, "BC2610FN");
        service.recusar(carol, s.getId());

        assertThrows(EstadoInvalidoException.class, () -> service.aceitar(carol, s.getId()));
    }

    @Test
    void semDiretoriaPadraoNaoCriaEAvisa() {
        when(diretoriaRepository.findByNomeIgnoreCase("COE/CRM")).thenReturn(Optional.empty());
        PlannerSugestao s = sugestao(carol, "BC2610FN");

        assertThrows(EstadoInvalidoException.class, () -> service.aceitar(carol, s.getId()));
        assertEquals(PlannerSugestao.Status.PENDENTE, s.getStatus());
    }
}
