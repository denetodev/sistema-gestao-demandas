package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.response.HistoricoStatusResponse;
import dev.denetodev.sgd_api.entity.*;
import dev.denetodev.sgd_api.repository.*;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import dev.denetodev.sgd_api.service.support.PermissaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

class DemandaServiceStatusTest {

    private final UUID demandaId = UUID.randomUUID();

    private DemandaRepository demandaRepository;
    private AuditoriaRepository auditoriaRepository;
    private PessoaRepository pessoaRepository;
    private PessoaDemandaRepository vinculos;
    private DemandaService service;
    private Demanda demanda;

    @BeforeEach
    void setUp() {
        demandaRepository = mock(DemandaRepository.class);
        auditoriaRepository = mock(AuditoriaRepository.class);
        pessoaRepository = mock(PessoaRepository.class);
        vinculos = mock(PessoaDemandaRepository.class);
        service = new DemandaService(
                demandaRepository, mock(DiretoriaRepository.class), mock(DemandanteRepository.class),
                mock(ProjetoRepository.class), mock(CampanhaRepository.class), mock(PecaRepository.class),
                vinculos, mock(CurrentPessoaResolver.class), mock(PermissaoService.class),
                auditoriaRepository, pessoaRepository);

        Diretoria diretoria = mock(Diretoria.class);
        when(diretoria.getId()).thenReturn(UUID.randomUUID());
        when(diretoria.getNome()).thenReturn("COE/CRM");
        demanda = new Demanda("Campanha X", diretoria);
        when(demandaRepository.findById(demandaId)).thenReturn(Optional.of(demanda));
        when(demandaRepository.existsById(demandaId)).thenReturn(true);
        when(vinculos.findByPessoaIdAndDemandaIdAndDataSaidaIsNull(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    void mudancaDeStatusFicaNaAuditoriaComQuemEMotivo() {
        Pessoa ana = pessoa(PerfilPessoa.PROFISSIONAL);

        service.atualizarStatus(ana, demandaId, StatusDemanda.EM_APROVACAO, "  Enviado ao demandante ");

        assertEquals(StatusDemanda.EM_APROVACAO, demanda.getStatus());
        ArgumentCaptor<Auditoria> captor = ArgumentCaptor.forClass(Auditoria.class);
        verify(auditoriaRepository).save(captor.capture());
        Auditoria a = captor.getValue();
        assertEquals("demanda", a.getTabela());
        assertEquals("status", a.getCampo());
        assertEquals("NAO_INICIADA", a.getValorAnterior());
        assertEquals("EM_APROVACAO", a.getValorNovo());
        assertEquals(ana.getId(), a.getAlteradoPor());
        assertEquals("Enviado ao demandante", a.getMotivo());
    }

    @Test
    void mesmoStatusNaoGeraAuditoria() {
        service.atualizarStatus(pessoa(PerfilPessoa.PROFISSIONAL), demandaId, StatusDemanda.NAO_INICIADA, null);

        verify(auditoriaRepository, never()).save(any());
    }

    @Test
    void visualizadorNaoMudaStatus() {
        assertThrows(AccessDeniedException.class,
                () -> service.atualizarStatus(pessoa(PerfilPessoa.VISUALIZADOR), demandaId, StatusDemanda.CONCLUIDA, null));
        assertEquals(StatusDemanda.NAO_INICIADA, demanda.getStatus());
    }

    @Test
    void cancelarSoGestorAdminOuResponsavelPrincipal() {
        Pessoa comum = pessoa(PerfilPessoa.PROFISSIONAL);
        assertThrows(AccessDeniedException.class,
                () -> service.atualizarStatus(comum, demandaId, StatusDemanda.CANCELADA, null));

        Pessoa gestor = pessoa(PerfilPessoa.GESTOR);
        service.atualizarStatus(gestor, demandaId, StatusDemanda.CANCELADA, "Demandante desistiu");
        assertEquals(StatusDemanda.CANCELADA, demanda.getStatus());

        // responsável principal também pode (volta a demanda antes)
        demanda.setStatus(StatusDemanda.EM_ANDAMENTO);
        Pessoa principal = pessoa(PerfilPessoa.PROFISSIONAL);
        PessoaDemanda vinculo = mock(PessoaDemanda.class);
        when(vinculo.getPapel()).thenReturn(PapelPessoaDemanda.RESPONSAVEL_PRINCIPAL);
        when(vinculos.findByPessoaIdAndDemandaIdAndDataSaidaIsNull(principal.getId(), demandaId)).thenReturn(Optional.of(vinculo));
        service.atualizarStatus(principal, demandaId, StatusDemanda.CANCELADA, null);
        assertEquals(StatusDemanda.CANCELADA, demanda.getStatus());
    }

    @Test
    void historicoTraduzOsRegistrosComONomeDeQuemMudou() {
        Pessoa ana = pessoa(PerfilPessoa.PROFISSIONAL);
        when(ana.getNome()).thenReturn("Ana");
        Auditoria registro = mock(Auditoria.class);
        when(registro.getCreatedAt()).thenReturn(OffsetDateTime.parse("2026-10-05T12:00:00Z"));
        when(registro.getValorAnterior()).thenReturn("NAO_INICIADA");
        when(registro.getValorNovo()).thenReturn("EM_ANDAMENTO");
        UUID anaId = ana.getId();
        when(registro.getAlteradoPor()).thenReturn(anaId);
        when(registro.getMotivo()).thenReturn(null);
        when(auditoriaRepository.findByTabelaAndRegistroIdAndCampoOrderByCreatedAtDesc("demanda", demandaId, "status"))
                .thenReturn(List.of(registro));
        when(pessoaRepository.findAllById(anyCollection())).thenReturn(List.of(ana));

        List<HistoricoStatusResponse> h = service.historicoDeStatus(demandaId);

        assertEquals(1, h.size());
        assertEquals("Ana", h.get(0).porNome());
        assertEquals("EM_ANDAMENTO", h.get(0).para());
    }

    private static Pessoa pessoa(PerfilPessoa perfil) {
        Pessoa p = mock(Pessoa.class);
        when(p.getId()).thenReturn(UUID.randomUUID());
        when(p.getPerfil()).thenReturn(perfil);
        return p;
    }
}
