package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.SalvarRelatorioRequest;
import dev.denetodev.sgd_api.dto.response.RelatorioResponse;
import dev.denetodev.sgd_api.entity.*;
import dev.denetodev.sgd_api.exception.EstadoInvalidoException;
import dev.denetodev.sgd_api.repository.*;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RelatorioMensalServiceTest {

    private static final YearMonth ATUAL = YearMonth.of(2026, 10);
    private static final YearMonth MARCO = YearMonth.of(2026, 3);

    private PessoaRepository pessoaRepository;
    private AtividadeRepository atividadeRepository;
    private PecaRepository pecaRepository;
    private RelatorioMensalRepository relatorioRepository;
    private RelatorioMensalService service;

    /** "Banco" em memória de um único relatório. */
    private final RelatorioMensal[] guardado = new RelatorioMensal[1];

    private Pessoa ana;
    private Pessoa gestor;
    private Atividade atv1;
    private Atividade atv2;

    @BeforeEach
    void setUp() {
        pessoaRepository = mock(PessoaRepository.class);
        atividadeRepository = mock(AtividadeRepository.class);
        pecaRepository = mock(PecaRepository.class);
        relatorioRepository = mock(RelatorioMensalRepository.class);
        guardado[0] = null;

        when(relatorioRepository.findByPessoaIdAndMes(any(), any()))
                .thenAnswer(inv -> Optional.ofNullable(guardado[0]));
        when(relatorioRepository.save(any(RelatorioMensal.class))).thenAnswer(inv -> {
            guardado[0] = inv.getArgument(0);
            return guardado[0];
        });

        service = new RelatorioMensalService(
                mock(CurrentPessoaResolver.class), pessoaRepository, atividadeRepository, pecaRepository,
                relatorioRepository, JsonMapper.builder().build());

        Diretoria diretoria = mock(Diretoria.class);
        when(diretoria.getNome()).thenReturn("COE/CRM");
        Area area = mock(Area.class);
        when(area.getNome()).thenReturn("HTML");
        when(area.getDiretoria()).thenReturn(diretoria);

        ana = pessoa("Ana", area, PerfilPessoa.PROFISSIONAL);
        gestor = pessoa("Gina", area, PerfilPessoa.GESTOR);

        atv1 = atividade("Briefing", LocalDate.of(2026, 3, 4));
        atv2 = atividade("Produção", LocalDate.of(2026, 3, 18));
        when(atividadeRepository.findByPessoa_IdAndDataRealizacaoBetweenOrderByDataRealizacaoAsc(any(), any(), any()))
                .thenReturn(List.of(atv1, atv2));
        when(pecaRepository.findProduzidasNoPeriodo(anyCollection(), any(), any(), any(), any(), any()))
                .thenReturn(List.of());
    }

    @Test
    void relatorioAbertoTrazTodasAsAtividadesIncluidas() {
        RelatorioResponse r = service.buscar(ana, MARCO, null, ATUAL);

        assertEquals(StatusRelatorio.ABERTO, r.status());
        assertEquals(2, r.atividades().size());
        assertTrue(r.atividades().stream().allMatch(RelatorioResponse.ItemAtividade::incluida));
        assertTrue(r.podeEditar());
    }

    @Test
    void salvarGuardaExclusoesEAprovarCongelaSoOQueFicou() {
        service.salvar(ana, MARCO, new SalvarRelatorioRequest("Mês de férias parciais", List.of(atv2.getId())), ATUAL);

        RelatorioResponse aprovado = service.aprovar(ana, MARCO, ATUAL);

        assertEquals(StatusRelatorio.APROVADO, aprovado.status());
        assertEquals(1, aprovado.atividades().size());
        assertEquals("Briefing", aprovado.atividades().get(0).tipo());
        assertEquals("Mês de férias parciais", aprovado.observacoes());

        // depois de aprovado, uma atividade nova no mês não muda o relatório assinado
        Atividade nova = atividade("Revisão", LocalDate.of(2026, 3, 25));
        when(atividadeRepository.findByPessoa_IdAndDataRealizacaoBetweenOrderByDataRealizacaoAsc(any(), any(), any()))
                .thenReturn(List.of(atv1, atv2, nova));

        RelatorioResponse lido = service.buscar(ana, MARCO, null, ATUAL);
        assertEquals(StatusRelatorio.APROVADO, lido.status());
        assertEquals(1, lido.atividades().size());
        assertFalse(lido.podeEditar());
        assertTrue(lido.podeReabrir());
    }

    @Test
    void relatorioAprovadoNaoAceitaEdicaoMasReabreEVoltaAoVivo() {
        service.aprovar(ana, MARCO, ATUAL);

        assertThrows(EstadoInvalidoException.class,
                () -> service.salvar(ana, MARCO, new SalvarRelatorioRequest(null, List.of()), ATUAL));

        RelatorioResponse reaberto = service.reabrir(ana, MARCO, null, ATUAL);

        assertEquals(StatusRelatorio.ABERTO, reaberto.status());
        assertNull(guardado[0].getSnapshotJson());
        assertEquals(2, reaberto.atividades().size());
        assertThrows(EstadoInvalidoException.class, () -> service.reabrir(ana, MARCO, null, ATUAL));
    }

    @Test
    void gestorVeEReabreRelatorioDeOutraPessoaMasNaoEdita() {
        when(pessoaRepository.findById(ana.getId())).thenReturn(Optional.of(ana));
        service.aprovar(ana, MARCO, ATUAL);

        RelatorioResponse visto = service.buscar(gestor, MARCO, ana.getId(), ATUAL);
        assertEquals("Ana", visto.pessoaNome());
        assertFalse(visto.podeEditar());
        assertTrue(visto.podeReabrir());

        assertEquals(StatusRelatorio.ABERTO, service.reabrir(gestor, MARCO, ana.getId(), ATUAL).status());
    }

    @Test
    void profissionalNaoVeRelatorioDeOutraPessoa() {
        Pessoa outra = pessoa("Bia", null, PerfilPessoa.PROFISSIONAL);

        assertThrows(AccessDeniedException.class, () -> service.buscar(ana, MARCO, outra.getId(), ATUAL));
        assertThrows(AccessDeniedException.class, () -> service.reabrir(ana, MARCO, outra.getId(), ATUAL));
        assertThrows(AccessDeniedException.class, () -> service.resumo(ana, MARCO, ATUAL));
    }

    @Test
    void visualizadorNaoTemRelatorioEMesFuturoEhRecusado() {
        Pessoa vis = pessoa("Vera", null, PerfilPessoa.VISUALIZADOR);

        assertThrows(AccessDeniedException.class, () -> service.buscar(vis, MARCO, null, ATUAL));
        assertThrows(IllegalArgumentException.class, () -> service.buscar(ana, YearMonth.of(2026, 11), null, ATUAL));
    }

    @Test
    void valorDeReferenciaSomaAsPecasDoMes() {
        Peca p = mock(Peca.class);
        Demanda demanda = mock(Demanda.class);
        TipoPeca tipo = mock(TipoPeca.class);
        when(demanda.getTitulo()).thenReturn("Campanha X");
        when(tipo.getNome()).thenReturn("KV");
        when(p.getId()).thenReturn(UUID.randomUUID());
        when(p.getDemanda()).thenReturn(demanda);
        when(p.getTipoPeca()).thenReturn(tipo);
        when(p.getNome()).thenReturn("KV v1");
        when(p.getQuantidade()).thenReturn(2);
        when(p.getValorUnitario()).thenReturn(new BigDecimal("1500.00"));
        when(p.getValorTotal()).thenReturn(new BigDecimal("3000.00"));
        when(pecaRepository.findProduzidasNoPeriodo(anyCollection(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(p));

        RelatorioResponse r = service.buscar(ana, MARCO, null, ATUAL);

        assertEquals(1, r.pecas().size());
        assertEquals(0, new BigDecimal("3000.00").compareTo(r.valorReferencia()));
    }

    // ---- helpers ----

    private static Pessoa pessoa(String nome, Area area, PerfilPessoa perfil) {
        Pessoa p = mock(Pessoa.class);
        UUID id = UUID.randomUUID();
        when(p.getId()).thenReturn(id);
        when(p.getNome()).thenReturn(nome);
        when(p.getArea()).thenReturn(area);
        when(p.getPerfil()).thenReturn(perfil);
        return p;
    }

    private static Atividade atividade(String tipoNome, LocalDate data) {
        Atividade a = mock(Atividade.class);
        TipoAtividade tipo = mock(TipoAtividade.class);
        when(tipo.getNome()).thenReturn(tipoNome);
        when(a.getId()).thenReturn(UUID.randomUUID());
        when(a.getTipoAtividade()).thenReturn(tipo);
        when(a.getDataRealizacao()).thenReturn(data);
        return a;
    }
}
