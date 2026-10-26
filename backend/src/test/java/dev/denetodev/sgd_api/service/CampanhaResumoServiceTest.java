package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.response.CampanhaDetalheResponse;
import dev.denetodev.sgd_api.dto.response.CampanhaResumoResponse;
import dev.denetodev.sgd_api.entity.*;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.CampanhaRepository;
import dev.denetodev.sgd_api.repository.CampanhaResumoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Quantas demandas, pessoas e quanto valor uma campanha reuniu (demandas canceladas ficam de fora). */
class CampanhaResumoServiceTest {

    private CampanhaRepository campanhaRepository;
    private CampanhaResumoRepository consultas;
    private CampanhaResumoService service;
    private Campanha bbcast;
    private UUID bbcastId;
    private Pessoa ana;
    private Pessoa bia;

    @BeforeEach
    void setUp() {
        campanhaRepository = mock(CampanhaRepository.class);
        consultas = mock(CampanhaResumoRepository.class);
        service = new CampanhaResumoService(campanhaRepository, consultas);

        bbcastId = UUID.randomUUID();
        bbcast = campanha(bbcastId, "BBCast Agro");
        ana = pessoa("Ana");
        bia = pessoa("Bia");

        pecas();
        vinculos();
        atividades();
    }

    private Campanha campanha(UUID id, String nome) {
        Campanha c = mock(Campanha.class);
        when(c.getId()).thenReturn(id);
        when(c.getNome()).thenReturn(nome);
        return c;
    }

    private Pessoa pessoa(String nome) {
        Pessoa p = mock(Pessoa.class);
        when(p.getId()).thenReturn(UUID.randomUUID());
        when(p.getNome()).thenReturn(nome);
        when(p.getNomeExibicao()).thenReturn(null);
        when(p.getNomeExibido()).thenReturn(nome);
        return p;
    }

    private Demanda demanda(Campanha camp, String titulo, StatusDemanda status, LocalDate criada, String valorImportado) {
        Demanda d = mock(Demanda.class);
        when(d.getId()).thenReturn(UUID.randomUUID());
        when(d.getTitulo()).thenReturn(titulo);
        when(d.getStatus()).thenReturn(status);
        when(d.getDataCriacao()).thenReturn(criada);
        when(d.getCampanha()).thenReturn(camp);
        when(d.getSourceSystem()).thenReturn(valorImportado == null ? null : "planilha-multimidia-ugr");
        when(d.getValor()).thenReturn(valorImportado == null ? null : new BigDecimal(valorImportado));
        return d;
    }

    private Peca peca(Demanda d, Pessoa p, String valorTotal) {
        Peca peca = mock(Peca.class);
        when(peca.getDemanda()).thenReturn(d);
        when(peca.getPessoa()).thenReturn(p);
        when(peca.getValorTotal()).thenReturn(new BigDecimal(valorTotal));
        return peca;
    }

    private PessoaDemanda vinculo(Pessoa p, Demanda d) {
        PessoaDemanda v = mock(PessoaDemanda.class);
        when(v.getPessoa()).thenReturn(p);
        when(v.getDemanda()).thenReturn(d);
        return v;
    }

    private Atividade atividade(Demanda d, Pessoa p) {
        Atividade a = mock(Atividade.class);
        when(a.getDemanda()).thenReturn(d);
        when(a.getPessoa()).thenReturn(p);
        return a;
    }

    // as listas são montadas antes do when(): criar mocks dentro de outro when() confunde o Mockito
    private void pecas(Peca... itens) {
        List<Peca> lista = List.of(itens);
        when(consultas.pecasDasDemandas(any())).thenReturn(lista);
    }

    private void vinculos(PessoaDemanda... itens) {
        List<PessoaDemanda> lista = List.of(itens);
        when(consultas.vinculosDasDemandas(any())).thenReturn(lista);
    }

    private void atividades(Atividade... itens) {
        List<Atividade> lista = List.of(itens);
        when(consultas.atividadesDasDemandas(any())).thenReturn(lista);
    }

    @Test
    void contaDemandasPessoasValorPecasEAtividades_semAsCanceladas() {
        Demanda d1 = demanda(bbcast, "Captação", StatusDemanda.CONCLUIDA, LocalDate.of(2026, 1, 2), null);
        Demanda d2 = demanda(bbcast, "Edição", StatusDemanda.EM_ANDAMENTO, LocalDate.of(2026, 3, 5), null);
        Demanda cancelada = demanda(bbcast, "Cancelada", StatusDemanda.CANCELADA, LocalDate.of(2026, 2, 1), "9999.00");
        when(consultas.demandasComCampanha()).thenReturn(List.of(d1, d2, cancelada));
        pecas(
                peca(d1, ana, "1000.00"), peca(d2, bia, "500.50"), peca(cancelada, ana, "7777.00"));
        vinculos(
                vinculo(ana, d1), vinculo(bia, d2), vinculo(ana, d2), vinculo(bia, cancelada));
        atividades(
                atividade(d1, ana), atividade(d1, ana), atividade(d2, bia), atividade(cancelada, bia));

        CampanhaResumoResponse r = service.resumirTodas().get(0);

        assertEquals("BBCast Agro", r.nome());
        assertEquals(3, r.demandas());
        assertEquals(2, r.demandasAtivas()); // sem a cancelada
        assertEquals(2, r.pessoas()); // Ana e Bia, sem repetir
        assertEquals(0, new BigDecimal("1500.50").compareTo(r.valor())); // cancelada fora
        assertEquals(2, r.pecas());
        assertEquals(3, r.atividades());
        assertEquals(1, r.porStatus().get(StatusDemanda.CANCELADA));
        assertEquals(LocalDate.of(2026, 1, 2), r.primeiraDemanda());
        assertEquals(LocalDate.of(2026, 3, 5), r.ultimaDemanda());
    }

    @Test
    void demandaImportadaSemPecasContaOValorDaPlanilha() {
        Demanda d = demanda(bbcast, "Antiga", StatusDemanda.CONCLUIDA, LocalDate.of(2026, 1, 2), "2500.00");
        when(consultas.demandasComCampanha()).thenReturn(List.of(d));

        assertEquals(0, new BigDecimal("2500.00").compareTo(service.resumirTodas().get(0).valor()));
    }

    @Test
    void demandaCriadaNoSistemaSemPecasNaoTemValor() {
        Demanda d = demanda(bbcast, "Nova", StatusDemanda.NAO_INICIADA, LocalDate.of(2026, 5, 1), null);
        when(consultas.demandasComCampanha()).thenReturn(List.of(d));

        assertEquals(0, BigDecimal.ZERO.compareTo(service.resumirTodas().get(0).valor()));
    }

    @Test
    void campanhaSemDemandasNaoApareceNoResumoGeral() {
        when(consultas.demandasComCampanha()).thenReturn(List.of());
        assertTrue(service.resumirTodas().isEmpty());
    }

    @Test
    void listaOrdenadaPeloValorMaiorPrimeiro() {
        Campanha outra = campanha(UUID.randomUUID(), "Livecast");
        Demanda a = demanda(bbcast, "A", StatusDemanda.CONCLUIDA, LocalDate.of(2026, 1, 1), null);
        Demanda b = demanda(outra, "B", StatusDemanda.CONCLUIDA, LocalDate.of(2026, 1, 1), null);
        when(consultas.demandasComCampanha()).thenReturn(List.of(a, b));
        pecas(peca(a, ana, "100.00"), peca(b, ana, "900.00"));

        assertEquals(List.of("Livecast", "BBCast Agro"),
                service.resumirTodas().stream().map(CampanhaResumoResponse::nome).toList());
    }

    @Test
    void detalheListaCadaPessoaComSuaParteEAsDemandasDaCampanha() {
        Demanda d1 = demanda(bbcast, "Captação", StatusDemanda.CONCLUIDA, LocalDate.of(2026, 1, 2), null);
        Demanda d2 = demanda(bbcast, "Edição", StatusDemanda.CONCLUIDA, LocalDate.of(2026, 3, 5), null);
        when(campanhaRepository.findById(bbcastId)).thenReturn(Optional.of(bbcast));
        when(consultas.demandasDaCampanha(bbcastId)).thenReturn(List.of(d1, d2));
        pecas(
                peca(d1, ana, "1000.00"), peca(d2, ana, "200.00"), peca(d2, bia, "300.00"));
        vinculos(
                vinculo(ana, d1), vinculo(ana, d2), vinculo(bia, d2));
        atividades(
                atividade(d1, ana), atividade(d2, ana), atividade(d2, bia));

        CampanhaDetalheResponse r = service.detalhar(bbcastId);

        assertEquals(2, r.resumo().pessoas());
        assertEquals(2, r.demandas().size());
        assertEquals("Ana", r.pessoas().get(0).nome()); // maior valor primeiro
        assertEquals(2, r.pessoas().get(0).demandas());
        assertEquals(2, r.pessoas().get(0).atividades());
        assertEquals(0, new BigDecimal("1200.00").compareTo(r.pessoas().get(0).valor()));
        assertEquals("Bia", r.pessoas().get(1).nome());
        assertEquals(0, new BigDecimal("300.00").compareTo(r.pessoas().get(1).valor()));
    }

    @Test
    void detalheDeCampanhaInexistenteDa404() {
        when(campanhaRepository.findById(any())).thenReturn(Optional.empty());
        assertThrows(RecursoNaoEncontradoException.class, () -> service.detalhar(UUID.randomUUID()));
    }

    @Test
    void detalheDeCampanhaSemDemandasVemZerado() {
        when(campanhaRepository.findById(bbcastId)).thenReturn(Optional.of(bbcast));
        when(consultas.demandasDaCampanha(bbcastId)).thenReturn(List.of());

        CampanhaDetalheResponse r = service.detalhar(bbcastId);

        assertEquals(0, r.resumo().demandas());
        assertTrue(r.pessoas().isEmpty());
        assertTrue(r.demandas().isEmpty());
    }
}
