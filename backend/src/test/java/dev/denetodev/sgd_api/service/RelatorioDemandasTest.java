package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.SalvarRelatorioRequest;
import dev.denetodev.sgd_api.dto.response.RelatorioResponse;
import dev.denetodev.sgd_api.dto.response.RelatorioResponse.ItemDemanda;
import dev.denetodev.sgd_api.entity.*;
import dev.denetodev.sgd_api.repository.*;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

/** As demandas do relatório mensal: o que entra, as peças por tipo, as evidências e o que o congelamento mantém. */
class RelatorioDemandasTest {

    private static final YearMonth OUTUBRO = YearMonth.of(2026, 10);
    private static final YearMonth ATUAL = YearMonth.of(2026, 11);

    private AtividadeRepository atividadeRepository;
    private PecaRepository pecaRepository;
    private EvidenciaRepository evidenciaRepository;
    private RelatorioMensalService service;
    private final RelatorioMensal[] guardado = new RelatorioMensal[1];

    private Pessoa thais;
    private Demanda demanda;
    private Atividade atvNaDemanda;
    private Atividade atvAvulsa;

    @BeforeEach
    void setUp() {
        atividadeRepository = mock(AtividadeRepository.class);
        pecaRepository = mock(PecaRepository.class);
        evidenciaRepository = mock(EvidenciaRepository.class);
        RelatorioMensalRepository relatorioRepository = mock(RelatorioMensalRepository.class);
        guardado[0] = null;
        when(relatorioRepository.findByPessoaIdAndMes(any(), any())).thenAnswer(inv -> Optional.ofNullable(guardado[0]));
        when(relatorioRepository.save(any(RelatorioMensal.class))).thenAnswer(inv -> {
            guardado[0] = inv.getArgument(0);
            return guardado[0];
        });
        service = new RelatorioMensalService(mock(CurrentPessoaResolver.class), mock(PessoaRepository.class),
                atividadeRepository, pecaRepository, evidenciaRepository, relatorioRepository, JsonMapper.builder().build());

        Diretoria diretoria = mock(Diretoria.class);
        when(diretoria.getNome()).thenReturn("COE/CRM");
        Area area = mock(Area.class);
        when(area.getNome()).thenReturn("Design");
        when(area.getDiretoria()).thenReturn(diretoria);
        Cargo cargo = mock(Cargo.class);
        when(cargo.getNome()).thenReturn("Designer III - Sênior");
        thais = mock(Pessoa.class);
        when(thais.getId()).thenReturn(UUID.randomUUID());
        when(thais.getNome()).thenReturn("Thaís");
        when(thais.getArea()).thenReturn(area);
        when(thais.getCargo()).thenReturn(cargo);
        when(thais.getPerfil()).thenReturn(PerfilPessoa.PROFISSIONAL);

        Demandante demandante = mock(Demandante.class);
        when(demandante.getNome()).thenReturn("Flavia");
        demanda = mock(Demanda.class);
        when(demanda.getId()).thenReturn(UUID.randomUUID());
        when(demanda.getCodigo()).thenReturn("BC2610AC");
        when(demanda.getTitulo()).thenReturn("Boletim Exclusivo");
        when(demanda.getDemandante()).thenReturn(demandante);
        when(demanda.getDiretoria()).thenReturn(diretoria);
        when(demanda.getLinkExterno()).thenReturn("https://planner/x");

        atvNaDemanda = atividade(demanda, LocalDate.of(2026, 10, 2));
        atvAvulsa = atividade(null, LocalDate.of(2026, 10, 5));
        when(atividadeRepository.findByPessoa_IdAndDataRealizacaoBetweenOrderByDataRealizacaoAsc(any(), any(), any()))
                .thenReturn(List.of(atvNaDemanda, atvAvulsa));
    }

    @Test
    void agrupaPorDemandaComPecasPorTipoEEvidencias() {
        Peca email = peca("E-mail", 2);
        Peca banner = peca("Banner", 2);
        when(pecaRepository.findProduzidasNoPeriodo(anyCollection(), any(), any(), any(), any(), any()))
                .thenReturn(List.of(email, banner));
        Evidencia link = mock(Evidencia.class);
        when(link.getId()).thenReturn(UUID.randomUUID());
        when(link.getAtividade()).thenReturn(atvNaDemanda);
        when(link.getTipo()).thenReturn(TipoEvidencia.LINK);
        when(link.getConteudo()).thenReturn("https://sharepoint/arte.png");
        when(evidenciaRepository.findByAtividadeIdIn(anyCollection())).thenReturn(List.of(link));

        RelatorioResponse r = service.buscar(thais, OUTUBRO, null, ATUAL);

        assertEquals(ModeloRelatorio.DESIGNER, r.modelo());
        assertEquals(1, r.demandas().size(), "a atividade avulsa não vira demanda");
        ItemDemanda d = r.demandas().get(0);
        assertEquals("BC2610AC", d.codigo());
        assertEquals("Flavia", d.demandante());
        assertEquals("COE/CRM", d.diretoria());
        assertEquals(4, d.quantidadePecas());
        assertEquals(List.of("E-mail", "Banner"), d.pecas().stream().map(RelatorioResponse.ResumoPeca::tipo).toList());
        assertEquals(1, d.evidencias().size());
        assertEquals("https://sharepoint/arte.png", d.evidencias().get(0).link());
    }

    @Test
    void aprovarMantemSoADemandaComAtividadeIncluidaOuPeca() {
        when(pecaRepository.findProduzidasNoPeriodo(anyCollection(), any(), any(), any(), any(), any())).thenReturn(List.of());
        service.salvar(thais, OUTUBRO, new SalvarRelatorioRequest(null, List.of(atvNaDemanda.getId())), ATUAL);

        RelatorioResponse aprovado = service.aprovar(thais, OUTUBRO, ATUAL);

        assertTrue(aprovado.demandas().isEmpty(), "sem atividade incluída e sem peça a demanda sai do relatório");
        assertEquals(ModeloRelatorio.DESIGNER, aprovado.modelo());
    }

    @Test
    void modeloSegueOCargoEDepoisAEquipe() {
        assertEquals(ModeloRelatorio.DESIGNER, ModeloRelatorio.de("Designer III - Sênior", null));
        assertEquals(ModeloRelatorio.HTML, ModeloRelatorio.de("Técnico Especialista em Construção de HTML", "Qualquer"));
        assertEquals(ModeloRelatorio.HTML, ModeloRelatorio.de(null, "HTML"));
        assertEquals(ModeloRelatorio.DESIGNER, ModeloRelatorio.de(null, "Design"));
        assertEquals(ModeloRelatorio.PADRAO, ModeloRelatorio.de("Videomaker", "Motion"));
        assertEquals(ModeloRelatorio.PADRAO, ModeloRelatorio.de(null, null));
    }

    private static Atividade atividade(Demanda demanda, LocalDate data) {
        Atividade a = mock(Atividade.class);
        TipoAtividade tipo = mock(TipoAtividade.class);
        when(tipo.getNome()).thenReturn("Criação");
        when(a.getId()).thenReturn(UUID.randomUUID());
        when(a.getDemanda()).thenReturn(demanda);
        when(a.getTipoAtividade()).thenReturn(tipo);
        when(a.getDataRealizacao()).thenReturn(data);
        return a;
    }

    private Peca peca(String tipoNome, int quantidade) {
        Peca p = mock(Peca.class);
        TipoPeca tipo = mock(TipoPeca.class);
        when(tipo.getNome()).thenReturn(tipoNome);
        when(p.getId()).thenReturn(UUID.randomUUID());
        when(p.getDemanda()).thenReturn(demanda);
        when(p.getTipoPeca()).thenReturn(tipo);
        when(p.getQuantidade()).thenReturn(quantidade);
        when(p.getNome()).thenReturn(tipoNome);
        when(p.getValorUnitario()).thenReturn(BigDecimal.TEN);
        when(p.getValorTotal()).thenReturn(BigDecimal.TEN.multiply(BigDecimal.valueOf(quantidade)));
        when(p.getDataEntrega()).thenReturn(LocalDate.of(2026, 10, 3));
        return p;
    }
}
