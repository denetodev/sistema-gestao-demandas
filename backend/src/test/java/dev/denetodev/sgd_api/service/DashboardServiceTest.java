package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.response.DashboardResponse;
import dev.denetodev.sgd_api.entity.*;
import dev.denetodev.sgd_api.repository.*;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardServiceTest {

    private static final LocalDate HOJE = LocalDate.of(2026, 10, 5);
    private static final YearMonth MES = YearMonth.of(2026, 10);

    private PessoaRepository pessoaRepository;
    private PessoaDemandaRepository pessoaDemandaRepository;
    private DemandaRepository demandaRepository;
    private AtividadeRepository atividadeRepository;
    private PecaRepository pecaRepository;
    private DashboardService service;

    private final Diretoria diretoria = diretoria("COE/CRM");
    private final Area design = area(diretoria, "Design");
    private final Area html = area(diretoria, "HTML");

    @BeforeEach
    void setUp() {
        pessoaRepository = mock(PessoaRepository.class);
        pessoaDemandaRepository = mock(PessoaDemandaRepository.class);
        demandaRepository = mock(DemandaRepository.class);
        atividadeRepository = mock(AtividadeRepository.class);
        pecaRepository = mock(PecaRepository.class);
        service = new DashboardService(
                mock(CurrentPessoaResolver.class), pessoaRepository, pessoaDemandaRepository,
                demandaRepository, mock(DiretoriaRepository.class), atividadeRepository, pecaRepository);
    }

    @Test
    void profissionalNaoPodePedirDiretoriaNemDetalharPorPessoa() {
        Pessoa ana = pessoa("Ana", design, PerfilPessoa.PROFISSIONAL, null);

        assertThrows(AccessDeniedException.class,
                () -> service.montar(ana, VisaoDashboard.DIRETORIA, null, null, false, MES, HOJE));
        assertThrows(AccessDeniedException.class,
                () -> service.montar(ana, VisaoDashboard.MINHA, null, null, true, MES, HOJE));
        assertThrows(AccessDeniedException.class,
                () -> service.montar(ana, VisaoDashboard.EQUIPE, null, null, false, MES, HOJE));
    }

    @Test
    void visualizadorNaoTemMeusNumeros() {
        Pessoa vis = pessoa("Vera", design, PerfilPessoa.VISUALIZADOR, null);

        assertThrows(AccessDeniedException.class,
                () -> service.montar(vis, VisaoDashboard.MINHA, null, null, false, MES, HOJE));
    }

    @Test
    void referenciaNaoVeColegaDeOutraArea() {
        Pessoa ref = pessoa("Carla", design, PerfilPessoa.PROFISSIONAL, design);
        Pessoa diego = pessoa("Diego", design, PerfilPessoa.PROFISSIONAL, null);
        Pessoa bruno = pessoa("Bruno", html, PerfilPessoa.PROFISSIONAL, null);
        when(pessoaRepository.findAll()).thenReturn(List.of(ref, diego, bruno));

        assertThrows(AccessDeniedException.class,
                () -> service.montar(ref, VisaoDashboard.EQUIPE, bruno.getId(), null, false, MES, HOJE));
    }

    @Test
    void visaoPadraoDaReferenciaEEquipeEValorSomaPecasDoMes() {
        Pessoa ref = pessoa("Carla", design, PerfilPessoa.PROFISSIONAL, design);
        Pessoa diego = pessoa("Diego", design, PerfilPessoa.PROFISSIONAL, null);
        when(pessoaRepository.findAll()).thenReturn(List.of(ref, diego));
        when(pessoaDemandaRepository.findVigentesPorPessoas(anyCollection())).thenReturn(List.of());
        List<Object[]> atividades = List.of(
                new Object[]{diego.getId(), LocalDate.of(2026, 10, 2), 3L},
                new Object[]{diego.getId(), LocalDate.of(2026, 9, 20), 2L});
        List<Peca> pecas = List.of(
                peca(diego, "1500.00", LocalDate.of(2026, 10, 1)),
                peca(diego, "700.00", LocalDate.of(2026, 3, 1)));
        when(atividadeRepository.contarPorPessoaEDia(anyCollection(), any(), any())).thenReturn(atividades);
        when(pecaRepository.findProduzidasNoPeriodo(anyCollection(), any(), any(), any(), any(), any()))
                .thenReturn(pecas);

        DashboardResponse r = service.montar(ref, null, null, null, false, MES, HOJE);

        assertEquals(VisaoDashboard.EQUIPE, r.visao());
        assertEquals(2, r.resumo().pessoas());
        assertEquals(3L, r.resumo().atividadesNoMes());
        assertEquals(0, new BigDecimal("1500.00").compareTo(r.resumo().valorNoMes()));
        assertEquals(0, new BigDecimal("2200.00").compareTo(r.resumo().valorNoAno()));
        assertEquals(2, r.calendario().size());
        assertTrue(r.porArea().isEmpty());
    }

    @Test
    void gestorVePorAreaEPorPessoaQuandoPedido() {
        Pessoa gestor = pessoa("Gina", design, PerfilPessoa.GESTOR, null);
        Pessoa ana = pessoa("Ana", design, PerfilPessoa.PROFISSIONAL, null);
        Pessoa bruno = pessoa("Bruno", html, PerfilPessoa.PROFISSIONAL, null);
        when(pessoaRepository.findAll()).thenReturn(List.of(gestor, ana, bruno));
        when(demandaRepository.findAll()).thenReturn(List.of());

        DashboardResponse r = service.montar(gestor, null, null, null, true, MES, HOJE);

        assertEquals(VisaoDashboard.DIRETORIA, r.visao());
        assertEquals(2, r.resumo().pessoas()); // gestor não entra nos agregados
        assertEquals(2, r.porArea().size());
        assertEquals(List.of("Ana", "Bruno"), r.porPessoa().stream().map(DashboardResponse.LinhaPessoa::nome).toList());
    }

    // ---- helpers ----

    private static Diretoria diretoria(String nome) {
        Diretoria d = mock(Diretoria.class);
        when(d.getId()).thenReturn(UUID.randomUUID());
        when(d.getNome()).thenReturn(nome);
        return d;
    }

    private static Area area(Diretoria diretoria, String nome) {
        Area a = mock(Area.class);
        when(a.getId()).thenReturn(UUID.randomUUID());
        when(a.getNome()).thenReturn(nome);
        when(a.getDiretoria()).thenReturn(diretoria);
        return a;
    }

    private static Pessoa pessoa(String nome, Area area, PerfilPessoa perfil, Area referencia) {
        Pessoa p = mock(Pessoa.class);
        when(p.getId()).thenReturn(UUID.randomUUID());
        when(p.getNome()).thenReturn(nome);
        when(p.getArea()).thenReturn(area);
        when(p.getPerfil()).thenReturn(perfil);
        when(p.getReferenciaArea()).thenReturn(referencia);
        when(p.getStatus()).thenReturn(StatusPessoa.ATIVO);
        when(p.getAprovadoEm()).thenReturn(java.time.OffsetDateTime.now());
        return p;
    }

    private static Peca peca(Pessoa autor, String total, LocalDate data) {
        Peca p = mock(Peca.class);
        when(p.getPessoa()).thenReturn(autor);
        when(p.getValorTotal()).thenReturn(new BigDecimal(total));
        when(p.getDataEntrega()).thenReturn(data);
        return p;
    }
}
