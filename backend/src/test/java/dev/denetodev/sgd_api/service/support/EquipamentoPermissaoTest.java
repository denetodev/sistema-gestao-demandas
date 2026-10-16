package dev.denetodev.sgd_api.service.support;

import dev.denetodev.sgd_api.entity.Area;
import dev.denetodev.sgd_api.entity.PapelEquipamentos;
import dev.denetodev.sgd_api.entity.PerfilPessoa;
import dev.denetodev.sgd_api.entity.Pessoa;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;

class EquipamentoPermissaoTest {

    private Pessoa pessoa(PerfilPessoa perfil, PapelEquipamentos papel) {
        Pessoa p = new Pessoa("Fulano", new Area(null, "Equipe"));
        p.setPerfil(perfil);
        p.setPapelEquipamentos(papel);
        return p;
    }

    @Test
    void adminEGestorVeemETemTudoMesmoSemPapel() {
        for (PerfilPessoa perfil : new PerfilPessoa[]{PerfilPessoa.ADMIN, PerfilPessoa.GESTOR}) {
            Pessoa p = pessoa(perfil, null);
            assertTrue(EquipamentoPermissao.temAcesso(p));
            assertTrue(EquipamentoPermissao.gerencia(p));
            assertTrue(EquipamentoPermissao.opera(p));
        }
    }

    @Test
    void profissionalSemPapelNaoEntraNemParaConsultar() {
        Pessoa p = pessoa(PerfilPessoa.PROFISSIONAL, null);
        assertFalse(EquipamentoPermissao.temAcesso(p));
        assertFalse(EquipamentoPermissao.opera(p));
        assertFalse(EquipamentoPermissao.gerencia(p));
        assertThrows(AccessDeniedException.class, () -> EquipamentoPermissao.exigirAcesso(p));
        assertThrows(AccessDeniedException.class, () -> EquipamentoPermissao.exigirOperacao(p));
        assertThrows(AccessDeniedException.class, () -> EquipamentoPermissao.exigirGerencia(p));
    }

    @Test
    void operadorDeEstudioEntraEOperaMasNaoGerencia() {
        Pessoa p = pessoa(PerfilPessoa.PROFISSIONAL, PapelEquipamentos.OPERADOR_DE_ESTUDIO);
        assertTrue(EquipamentoPermissao.temAcesso(p));
        assertTrue(EquipamentoPermissao.opera(p));
        assertFalse(EquipamentoPermissao.gerencia(p));
        assertThrows(AccessDeniedException.class, () -> EquipamentoPermissao.exigirGerencia(p));
    }

    @Test
    void gestorDeEquipamentosEntraGerenciaEOpera() {
        Pessoa p = pessoa(PerfilPessoa.PROFISSIONAL, PapelEquipamentos.GESTOR_DE_EQUIPAMENTOS);
        assertTrue(EquipamentoPermissao.temAcesso(p));
        assertTrue(EquipamentoPermissao.gerencia(p));
        assertTrue(EquipamentoPermissao.opera(p));
    }

    @Test
    void aEquipeNaoDaAcessoSozinha() {
        // o acesso por equipe saiu: a marca na equipe, mesmo que ainda exista no banco, não vale mais
        Pessoa p = pessoa(PerfilPessoa.PROFISSIONAL, null);
        assertFalse(EquipamentoPermissao.temAcesso(p));
    }

    @Test
    void visualizadorNuncaEntraNemComPapel() {
        Pessoa p = pessoa(PerfilPessoa.VISUALIZADOR, PapelEquipamentos.GESTOR_DE_EQUIPAMENTOS);
        assertFalse(EquipamentoPermissao.temAcesso(p));
        assertFalse(EquipamentoPermissao.gerencia(p));
        assertFalse(EquipamentoPermissao.opera(p));
    }
}
