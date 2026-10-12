package dev.denetodev.sgd_api.service.support;

import dev.denetodev.sgd_api.entity.Area;
import dev.denetodev.sgd_api.entity.EscopoListagem;
import dev.denetodev.sgd_api.entity.PerfilPessoa;
import dev.denetodev.sgd_api.entity.Pessoa;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FiltroDemandaTest {

    // ---- busca de texto ----

    @Test
    void buscaIgnoraMaiusculasEAcentos() {
        assertEquals("%captacao agro%", BuscaTexto.padraoLike("Captação AGRO"));
        assertEquals("%acao%", BuscaTexto.padraoLike("  AÇÃO "));
    }

    @Test
    void buscaVaziaOuSoEspacosNaoFiltra() {
        assertNull(BuscaTexto.padraoLike(null));
        assertNull(BuscaTexto.padraoLike("   "));
    }

    @Test
    void buscaNaoDeixaOUsuarioUsarCoringaDoBanco() {
        assertEquals("%100\\%%", BuscaTexto.padraoLike("100%"));
        assertEquals("%a\\_b%", BuscaTexto.padraoLike("a_b"));
        assertEquals("%a\\\\b%", BuscaTexto.padraoLike("a\\b"));
    }

    @Test
    void buscaGrandeDemaisEhCortada() {
        String enorme = "a".repeat(500);
        assertEquals(BuscaTexto.MAXIMO + 2, BuscaTexto.padraoLike(enorme).length()); // % + texto + %
    }

    @Test
    void asTabelasDeAcentosTemOMesmoTamanho() {
        assertEquals(BuscaTexto.ACENTOS.length(), BuscaTexto.SEM_ACENTOS.length());
    }

    // ---- ordenação ----

    @Test
    void ordenacaoPermitidaPassa() {
        Pageable p = OrdenacaoDemanda.sanear(PageRequest.of(2, 30, Sort.by(Sort.Direction.ASC, "titulo")));
        assertEquals(2, p.getPageNumber());
        assertEquals(30, p.getPageSize());
        assertEquals(Sort.Direction.ASC, p.getSort().getOrderFor("titulo").getDirection());
    }

    @Test
    void ordenacaoDesconhecidaVoltaAoPadraoEmVezDeEstourar() {
        Pageable p = OrdenacaoDemanda.sanear(PageRequest.of(0, 10, Sort.by("senha")));
        assertNull(p.getSort().getOrderFor("senha"));
        assertEquals(Sort.Direction.DESC, p.getSort().getOrderFor("dataCriacao").getDirection());
    }

    @Test
    void semOrdenacaoUsaAsMaisRecentesPrimeiro() {
        Pageable p = OrdenacaoDemanda.sanear(PageRequest.of(0, 10));
        assertEquals(Sort.Direction.DESC, p.getSort().getOrderFor("dataCriacao").getDirection());
        assertEquals(Sort.Direction.DESC, p.getSort().getOrderFor("createdAt").getDirection()); // desempate
    }

    @Test
    void tamanhoDePaginaEhLimitado() {
        assertEquals(100, OrdenacaoDemanda.sanear(PageRequest.of(0, 5000)).getPageSize());
    }

    // ---- escopos ----

    private Pessoa pessoa(PerfilPessoa perfil, Area area, Area referencia) {
        Pessoa p = mock(Pessoa.class);
        when(p.getPerfil()).thenReturn(perfil);
        when(p.getArea()).thenReturn(area);
        when(p.getReferenciaArea()).thenReturn(referencia);
        return p;
    }

    private Area area() {
        Area a = mock(Area.class);
        when(a.getId()).thenReturn(UUID.randomUUID());
        return a;
    }

    @Test
    void equipeEstaAbertaATodoProfissionalQueTemArea() {
        PermissaoService permissao = new PermissaoService(null);
        Pessoa comum = pessoa(PerfilPessoa.PROFISSIONAL, area(), null);

        assertDoesNotThrow(() -> permissao.validarEscopo(comum, EscopoListagem.EQUIPE));
        assertDoesNotThrow(() -> permissao.validarEscopo(comum, EscopoListagem.DIRETORIA));
        assertDoesNotThrow(() -> permissao.validarEscopo(comum, EscopoListagem.MINHAS));
    }

    @Test
    void todasContinuaSoParaGestorEAdmin() {
        PermissaoService permissao = new PermissaoService(null);

        assertThrows(AccessDeniedException.class,
                () -> permissao.validarEscopo(pessoa(PerfilPessoa.PROFISSIONAL, area(), null), EscopoListagem.TODAS));
        assertThrows(AccessDeniedException.class,
                () -> permissao.validarEscopo(pessoa(PerfilPessoa.VISUALIZADOR, area(), null), EscopoListagem.TODAS));
        assertDoesNotThrow(
                () -> permissao.validarEscopo(pessoa(PerfilPessoa.GESTOR, area(), null), EscopoListagem.TODAS));
        assertDoesNotThrow(
                () -> permissao.validarEscopo(pessoa(PerfilPessoa.ADMIN, area(), null), EscopoListagem.TODAS));
    }

    @Test
    void areaDaEquipeEhAReferenciadaOuAPropria() {
        PermissaoService permissao = new PermissaoService(null);
        Area propria = area();
        Area referenciada = area();

        assertEquals(propria, permissao.areaDaEquipe(pessoa(PerfilPessoa.PROFISSIONAL, propria, null)));
        assertEquals(referenciada, permissao.areaDaEquipe(pessoa(PerfilPessoa.PROFISSIONAL, propria, referenciada)));
    }
}
