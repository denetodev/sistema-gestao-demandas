package dev.denetodev.sgd_api.service.support;

import dev.denetodev.sgd_api.entity.Area;
import dev.denetodev.sgd_api.entity.PapelEquipamentos;
import dev.denetodev.sgd_api.entity.PerfilPessoa;
import dev.denetodev.sgd_api.entity.Pessoa;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.junit.jupiter.api.Assertions.*;

class EquipamentoPermissaoTest {

    private Pessoa pessoa(PerfilPessoa perfil, boolean equipeComAcesso, PapelEquipamentos papel) {
        Area area = new Area(null, "Equipe");
        area.setAcessaEquipamentos(equipeComAcesso);
        Pessoa p = new Pessoa("Fulano", area);
        p.setPerfil(perfil);
        p.setPapelEquipamentos(papel);
        return p;
    }

    @Test
    void adminEGestorTemTudo() {
        for (PerfilPessoa perfil : new PerfilPessoa[]{PerfilPessoa.ADMIN, PerfilPessoa.GESTOR}) {
            Pessoa p = pessoa(perfil, false, null);
            assertTrue(EquipamentoPermissao.temAcesso(p));
            assertTrue(EquipamentoPermissao.gerencia(p));
            assertTrue(EquipamentoPermissao.opera(p));
        }
    }

    @Test
    void profissionalDeEquipeLiberadaSoConsultaSemPapel() {
        Pessoa p = pessoa(PerfilPessoa.PROFISSIONAL, true, null);
        assertTrue(EquipamentoPermissao.temAcesso(p));
        assertFalse(EquipamentoPermissao.opera(p));
        assertFalse(EquipamentoPermissao.gerencia(p));
        assertThrows(AccessDeniedException.class, () -> EquipamentoPermissao.exigirOperacao(p));
        assertThrows(AccessDeniedException.class, () -> EquipamentoPermissao.exigirGerencia(p));
    }

    @Test
    void profissionalDeEquipeNaoLiberadaSemPapelNaoEntra() {
        Pessoa p = pessoa(PerfilPessoa.PROFISSIONAL, false, null);
        assertFalse(EquipamentoPermissao.temAcesso(p));
        assertThrows(AccessDeniedException.class, () -> EquipamentoPermissao.exigirAcesso(p));
    }

    @Test
    void operadorDeEstudioOperaMasNaoGerencia() {
        Pessoa p = pessoa(PerfilPessoa.PROFISSIONAL, true, PapelEquipamentos.OPERADOR_DE_ESTUDIO);
        assertTrue(EquipamentoPermissao.opera(p));
        assertFalse(EquipamentoPermissao.gerencia(p));
    }

    @Test
    void gestorDeEquipamentosGerenciaEOpera() {
        Pessoa p = pessoa(PerfilPessoa.PROFISSIONAL, true, PapelEquipamentos.GESTOR_DE_EQUIPAMENTOS);
        assertTrue(EquipamentoPermissao.gerencia(p));
        assertTrue(EquipamentoPermissao.opera(p));
    }

    @Test
    void papelBastaParaTerAcessoMesmoSeAEquipeNaoForLiberada() {
        Pessoa p = pessoa(PerfilPessoa.PROFISSIONAL, false, PapelEquipamentos.OPERADOR_DE_ESTUDIO);
        assertTrue(EquipamentoPermissao.temAcesso(p));
        assertTrue(EquipamentoPermissao.opera(p));
    }

    @Test
    void visualizadorNuncaEntraNemComPapelOuEquipeLiberada() {
        Pessoa p = pessoa(PerfilPessoa.VISUALIZADOR, true, PapelEquipamentos.GESTOR_DE_EQUIPAMENTOS);
        assertFalse(EquipamentoPermissao.temAcesso(p));
        assertFalse(EquipamentoPermissao.gerencia(p));
        assertFalse(EquipamentoPermissao.opera(p));
    }
}
