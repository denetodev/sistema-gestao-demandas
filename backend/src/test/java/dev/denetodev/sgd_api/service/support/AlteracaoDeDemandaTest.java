package dev.denetodev.sgd_api.service.support;

import dev.denetodev.sgd_api.entity.Area;
import dev.denetodev.sgd_api.entity.Demanda;
import dev.denetodev.sgd_api.entity.PerfilPessoa;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.entity.PessoaDemanda;
import dev.denetodev.sgd_api.repository.PessoaDemandaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Só quem participa da demanda altera algo nela (mais Referência da área do time, Gestor e Admin). */
class AlteracaoDeDemandaTest {

    private final UUID demandaId = UUID.randomUUID();
    private PessoaDemandaRepository vinculos;
    private PermissaoService permissao;
    private Area areaA;
    private Area areaB;
    private Demanda demanda;

    @BeforeEach
    void setUp() {
        vinculos = mock(PessoaDemandaRepository.class);
        permissao = new PermissaoService(vinculos);
        areaA = area();
        areaB = area();
        demanda = mock(Demanda.class);
        when(demanda.getId()).thenReturn(demandaId);
        when(vinculos.findByDemandaId(demandaId)).thenReturn(List.of());
    }

    private Area area() {
        Area a = mock(Area.class);
        when(a.getId()).thenReturn(UUID.randomUUID());
        return a;
    }

    private Pessoa pessoa(PerfilPessoa perfil, Area area) {
        Pessoa p = mock(Pessoa.class);
        when(p.getId()).thenReturn(UUID.randomUUID());
        when(p.getPerfil()).thenReturn(perfil);
        when(p.getArea()).thenReturn(area);
        return p;
    }

    private PessoaDemanda noTime(Pessoa pessoa, LocalDate saida) {
        PessoaDemanda v = new PessoaDemanda(pessoa, demanda);
        v.setDataSaida(saida);
        return v;
    }

    private void time(PessoaDemanda... vs) {
        when(vinculos.findByDemandaId(demandaId)).thenReturn(List.of(vs));
    }

    @Test
    void participanteAtivoAltera() {
        Pessoa ana = pessoa(PerfilPessoa.PROFISSIONAL, areaA);
        time(noTime(ana, null));

        assertTrue(permissao.podeAlterarDemanda(ana, demandaId));
        assertDoesNotThrow(() -> permissao.exigirPodeAlterarDemanda(ana, demandaId));
    }

    @Test
    void profissionalForaDoTimeNaoAltera() {
        Pessoa ana = pessoa(PerfilPessoa.PROFISSIONAL, areaA);
        Pessoa bia = pessoa(PerfilPessoa.PROFISSIONAL, areaB);
        time(noTime(bia, null));

        assertFalse(permissao.podeAlterarDemanda(ana, demandaId));
        assertThrows(AccessDeniedException.class, () -> permissao.exigirPodeAlterarDemanda(ana, demandaId));
    }

    @Test
    void quemJaSaiuDoTimeNaoAltera() {
        Pessoa ana = pessoa(PerfilPessoa.PROFISSIONAL, areaA);
        time(noTime(ana, LocalDate.of(2026, 9, 1)));

        assertFalse(permissao.podeAlterarDemanda(ana, demandaId));
    }

    @Test
    void demandaSemTimeSoEAlteradaPelaGestao() {
        Pessoa ana = pessoa(PerfilPessoa.PROFISSIONAL, areaA);
        assertFalse(permissao.podeAlterarDemanda(ana, demandaId));
        assertTrue(permissao.podeAlterarDemanda(pessoa(PerfilPessoa.GESTOR, areaB), demandaId));
        assertTrue(permissao.podeAlterarDemanda(pessoa(PerfilPessoa.ADMIN, areaB), demandaId));
    }

    @Test
    void gestorEAdminAlteramQualquerDemanda() {
        Pessoa bia = pessoa(PerfilPessoa.PROFISSIONAL, areaB);
        time(noTime(bia, null));

        assertTrue(permissao.podeAlterarDemanda(pessoa(PerfilPessoa.GESTOR, areaA), demandaId));
        assertTrue(permissao.podeAlterarDemanda(pessoa(PerfilPessoa.ADMIN, areaA), demandaId));
    }

    @Test
    void referenciaDaAreaAlteraSeAlguemDaAreaEstaNoTime() {
        Pessoa referencia = pessoa(PerfilPessoa.PROFISSIONAL, areaA);
        when(referencia.getReferenciaArea()).thenReturn(areaA);
        Pessoa colega = pessoa(PerfilPessoa.PROFISSIONAL, areaA);
        time(noTime(colega, null));

        assertTrue(permissao.podeAlterarDemanda(referencia, demandaId));
    }

    @Test
    void referenciaNaoAlteraDemandaSemNinguemDaSuaAreaNoTime() {
        Pessoa referencia = pessoa(PerfilPessoa.PROFISSIONAL, areaA);
        when(referencia.getReferenciaArea()).thenReturn(areaA);
        Pessoa deOutraArea = pessoa(PerfilPessoa.PROFISSIONAL, areaB);
        time(noTime(deOutraArea, null));

        assertFalse(permissao.podeAlterarDemanda(referencia, demandaId));
    }

    @Test
    void referenciaNaoContaOColegaQueJaSaiuDoTime() {
        Pessoa referencia = pessoa(PerfilPessoa.PROFISSIONAL, areaA);
        when(referencia.getReferenciaArea()).thenReturn(areaA);
        Pessoa colega = pessoa(PerfilPessoa.PROFISSIONAL, areaA);
        time(noTime(colega, LocalDate.of(2026, 9, 1)));

        assertFalse(permissao.podeAlterarDemanda(referencia, demandaId));
    }

    @Test
    void visualizadorNuncaAltera_mesmoQueConsteNoTime() {
        Pessoa leitor = pessoa(PerfilPessoa.VISUALIZADOR, areaA);
        time(noTime(leitor, null));

        assertFalse(permissao.podeAlterarDemanda(leitor, demandaId));
    }
}
