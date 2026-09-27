package dev.denetodev.sgd_api.service.support;

import dev.denetodev.sgd_api.entity.Area;
import dev.denetodev.sgd_api.entity.EscopoListagem;
import dev.denetodev.sgd_api.entity.Evidencia;
import dev.denetodev.sgd_api.entity.PerfilPessoa;
import dev.denetodev.sgd_api.entity.Pessoa;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class PermissaoService {

    public boolean podeGerenciar(Pessoa usuario, Pessoa donoDoItem) {
        if (ehAdminOuGestor(usuario)) {
            return true;
        }
        if (donoDoItem != null && donoDoItem.getId().equals(usuario.getId())) {
            return true;
        }
        return usuario.getReferenciaArea() != null
                && donoDoItem != null
                && usuario.getReferenciaArea().getId().equals(donoDoItem.getArea().getId());
    }

    public boolean podeGerenciarTipoPeca(Pessoa usuario, Area area) {
        if (ehAdminOuGestor(usuario)) {
            return true;
        }
        return usuario.getReferenciaArea() != null
                && usuario.getReferenciaArea().getId().equals(area.getId());
    }

    public boolean podeGerenciarEvidencia(Pessoa usuario, Evidencia evidencia) {
        boolean pelaAtividade = evidencia.getAtividade() != null
                && podeGerenciar(usuario, evidencia.getAtividade().getPessoa());
        boolean pelaPeca = evidencia.getPeca() != null
                && podeGerenciar(usuario, evidencia.getPeca().getPessoa());
        return pelaAtividade || pelaPeca;
    }

    /** Valida se o perfil/função do usuário permite pedir esse escopo —
     * lança 403 quando não permite. MINHAS e DIRETORIA são livres pra
     * qualquer autenticado. */
    public void validarEscopo(Pessoa usuario, EscopoListagem escopo) {
        if (escopo == EscopoListagem.EQUIPE && usuario.getReferenciaArea() == null) {
            throw new AccessDeniedException("Escopo 'equipe' exige ser Referência de uma Área");
        }
        if (escopo == EscopoListagem.TODAS && !ehAdminOuGestor(usuario)) {
            throw new AccessDeniedException("Escopo 'todas' restrito a ADMIN/GESTOR");
        }
    }

    private boolean ehAdminOuGestor(Pessoa usuario) {
        return usuario.getPerfil() == PerfilPessoa.ADMIN || usuario.getPerfil() == PerfilPessoa.GESTOR;
    }
}