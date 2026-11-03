package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.response.PlannerSincronizacaoResponse;
import dev.denetodev.sgd_api.entity.Demanda;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.entity.PessoaDemanda;
import dev.denetodev.sgd_api.entity.PlannerSugestao;
import dev.denetodev.sgd_api.exception.EstadoInvalidoException;
import dev.denetodev.sgd_api.planner.PlannerFonte;
import dev.denetodev.sgd_api.planner.PlannerFonte.PlanoPlanner;
import dev.denetodev.sgd_api.planner.PlannerFonte.TarefaPlanner;
import dev.denetodev.sgd_api.repository.DemandaRepository;
import dev.denetodev.sgd_api.repository.PessoaDemandaRepository;
import dev.denetodev.sgd_api.repository.PessoaRepository;
import dev.denetodev.sgd_api.repository.PlannerSugestaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Quem recebe sugestão do Planner: só quem tem a etiqueta na tarefa, uma vez só e sem repetir o que já está no sistema. */
class PlannerSyncServiceTest {

    private static final OffsetDateTime AGORA = OffsetDateTime.of(2026, 11, 3, 12, 0, 0, 0, ZoneOffset.UTC);

    private PlannerFonte fonte;
    private PessoaRepository pessoaRepository;
    private DemandaRepository demandaRepository;
    private PessoaDemandaRepository pessoaDemandaRepository;
    private PlannerSugestaoRepository sugestaoRepository;
    private PlannerSyncService service;
    private Pessoa carol;
    private Pessoa thais;

    @BeforeEach
    void preparar() {
        fonte = mock(PlannerFonte.class);
        pessoaRepository = mock(PessoaRepository.class);
        demandaRepository = mock(DemandaRepository.class);
        pessoaDemandaRepository = mock(PessoaDemandaRepository.class);
        sugestaoRepository = mock(PlannerSugestaoRepository.class);
        service = new PlannerSyncService(fonte, pessoaRepository, demandaRepository, pessoaDemandaRepository,
                sugestaoRepository, Clock.fixed(Instant.parse("2026-11-03T12:00:00Z"), ZoneOffset.UTC), true, 30);

        carol = pessoa("HTML - Carol");
        thais = pessoa("CRIATIVO - Thaís");
        when(pessoaRepository.findByEtiquetaPlannerIsNotNull()).thenReturn(List.of(carol, thais));
        when(sugestaoRepository.chavesJaExistentes(anyCollection())).thenReturn(List.of());
        when(demandaRepository.findByCodigoIgnoreCase(anyString())).thenReturn(List.of());
        when(fonte.configurada()).thenReturn(true);
    }

    private Pessoa pessoa(String etiqueta) {
        Pessoa p = mock(Pessoa.class);
        when(p.getId()).thenReturn(UUID.randomUUID());
        when(p.getEtiquetaPlanner()).thenReturn(etiqueta);
        when(p.getAprovadoEm()).thenReturn(AGORA.minusDays(100));
        return p;
    }

    private PlanoPlanner plano(TarefaPlanner... tarefas) {
        return new PlanoPlanner("plano1",
                Map.of("category1", "HTML - Carol", "category2", "CRIATIVO - Thais", "category3", "URGENTE",
                        "category4", "CRIATIVO - Gabriel"),
                Map.of("b1", "HTML para Aprovação"), List.of(tarefas));
    }

    private TarefaPlanner tarefa(String id, String titulo, int pct, OffsetDateTime criada, String... categorias) {
        return new TarefaPlanner(id, titulo, "b1", criada, pct, List.of(categorias));
    }

    @SuppressWarnings("unchecked")
    private List<PlannerSugestao> salvas() {
        ArgumentCaptor<List<PlannerSugestao>> c = ArgumentCaptor.forClass(List.class);
        verify(sugestaoRepository).saveAll(c.capture());
        return c.getValue();
    }

    @Test
    void cadaPessoaMarcadaRecebeUmaSugestaoComOCodigoDoTitulo() {
        when(fonte.planos()).thenReturn(List.of("plano1"));
        when(fonte.ler("plano1")).thenReturn(plano(
                tarefa("t1", "BC2610FN_email_SS06_BBFriday", 0, AGORA.minusDays(2), "category1", "category2", "category3")));

        PlannerSincronizacaoResponse r = service.sincronizar();

        List<PlannerSugestao> salvas = salvas();
        assertEquals(2, salvas.size());
        assertEquals(2, r.sugestoesNovas());
        assertTrue(salvas.stream().allMatch(s -> "BC2610FN".equals(s.getCodigo())));
        assertEquals("HTML para Aprovação", salvas.get(0).getBucket());
        assertEquals(List.of(), r.etiquetasSemCadastro());
    }

    @Test
    void ignoraTarefaConcluidaEAntigaDemais() {
        when(fonte.planos()).thenReturn(List.of("plano1"));
        when(fonte.ler("plano1")).thenReturn(plano(
                tarefa("t1", "BC2610AA concluída", 100, AGORA.minusDays(2), "category1"),
                tarefa("t2", "BC2609ZZ velha", 0, AGORA.minusDays(90), "category1")));

        PlannerSincronizacaoResponse r = service.sincronizar();

        assertEquals(0, r.sugestoesNovas());
        assertEquals(0, r.tarefasNaJanela());
        assertEquals(2, r.tarefasLidas());
    }

    @Test
    void naoRecriaSugestaoQueJaExisteNemDeQuemRecusou() {
        when(fonte.planos()).thenReturn(List.of("plano1"));
        when(fonte.ler("plano1")).thenReturn(plano(tarefa("t1", "BC2610FN x", 0, AGORA.minusDays(2), "category1")));
        String jaExistente = "t1|" + carol.getId();
        when(sugestaoRepository.chavesJaExistentes(anyCollection())).thenReturn(List.of(jaExistente));

        assertEquals(0, service.sincronizar().sugestoesNovas());
    }

    @Test
    void naoSugereQuandoAPessoaJaEstaNoTimeDaDemandaDoMesmoCodigo() {
        Demanda demanda = mock(Demanda.class);
        when(demanda.getId()).thenReturn(UUID.randomUUID());
        when(demandaRepository.findByCodigoIgnoreCase("BC2610FN")).thenReturn(List.of(demanda));
        when(pessoaDemandaRepository.findByPessoaIdAndDemandaIdAndDataSaidaIsNull(carol.getId(), demanda.getId()))
                .thenReturn(Optional.of(mock(PessoaDemanda.class)));
        when(fonte.planos()).thenReturn(List.of("plano1"));
        when(fonte.ler("plano1")).thenReturn(plano(tarefa("t1", "BC2610FN x", 0, AGORA.minusDays(2), "category1", "category2")));

        service.sincronizar();

        List<PlannerSugestao> salvas = salvas();
        assertEquals(1, salvas.size(), "só a Thaís, que ainda não está no time");
        assertEquals(thais.getId(), salvas.get(0).getPessoa().getId());
    }

    @Test
    void etiquetaDeGenteSemCadastroApareceNoRelatorioMasUrgenteNao() {
        when(fonte.planos()).thenReturn(List.of("plano1"));
        when(fonte.ler("plano1")).thenReturn(plano(tarefa("t1", "BC2610FN x", 0, AGORA.minusDays(2), "category3", "category4")));

        PlannerSincronizacaoResponse r = service.sincronizar();

        assertEquals(List.of("CRIATIVO - Gabriel"), r.etiquetasSemCadastro());
        assertEquals(0, r.sugestoesNovas());
    }

    @Test
    void semConfiguracaoNaoTentaLer() {
        when(fonte.configurada()).thenReturn(false);

        assertThrows(EstadoInvalidoException.class, () -> service.sincronizar());
        verify(fonte, never()).ler(any());
    }

    @Test
    void codigoSaiDoComecoDoTituloComQualquerSeparador() {
        assertEquals("BC2610FN", PlannerSyncService.extrairCodigo("BC2610FN_email_SS06_BBFriday"));
        assertEquals("BC2610FO", PlannerSyncService.extrairCodigo("bc2610fo - Email Super Sexta"));
        assertNull(PlannerSyncService.extrairCodigo("Atualizar layout"));
        assertNull(PlannerSyncService.extrairCodigo(null));
    }

    @Test
    void etiquetaComparaSemAcentoMaiusculaOuTipoDeHifen() {
        assertEquals(PlannerSyncService.normalizar("CRIATIVO - Cícero"), PlannerSyncService.normalizar("criativo – cicero"));
        assertEquals(PlannerSyncService.normalizar("HTML - Neto"), PlannerSyncService.normalizar("  html-Neto "));
        assertNotEquals(PlannerSyncService.normalizar("HTML - Neto"), PlannerSyncService.normalizar("CRIATIVO - Neto"));
    }

    @Test
    void pessoaSemAprovacaoNaoRecebeSugestao() {
        Pessoa pendente = pessoa("HTML - Carol");
        when(pendente.getAprovadoEm()).thenReturn(null);
        when(pessoaRepository.findByEtiquetaPlannerIsNotNull()).thenReturn(List.of(pendente));
        when(fonte.planos()).thenReturn(List.of("plano1"));
        when(fonte.ler("plano1")).thenReturn(plano(tarefa("t1", "BC2610FN x", 0, AGORA.minusDays(2), "category1")));

        assertEquals(0, service.sincronizar().sugestoesNovas());
        verify(sugestaoRepository).saveAll(eq(List.of()));
    }
}
