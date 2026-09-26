package dev.denetodev.sgd_api.service.support;

import dev.denetodev.sgd_api.entity.Area;
import dev.denetodev.sgd_api.entity.Evidencia;
import dev.denetodev.sgd_api.entity.PerfilPessoa;
import dev.denetodev.sgd_api.entity.Pessoa;
import org.springframework.stereotype.Component;

@Component
public class PermissaoService {

    /**
     * Pode gerenciar (criar em nome de/editar/cancelar) um item cujo
     * "dono" é donoDoItem — nulo quando o item não tem profissional
     * associado (ex.: atividade avulsa, sem pessoa vinculada).
     */
    public boolean podeGerenciar(Pessoa usuario, Pessoa donoDoItem) {
        if (ehAdminOuGestor(usuario)) {
            return true;
        }
        if (donoDoItem != null && donoDoItem.getId().equals(usuario.getId())) {
            return true; // é o próprio dono
        }
        return usuario.getReferenciaArea() != null
                && donoDoItem != null
                && usuario.getReferenciaArea().getId().equals(donoDoItem.getArea().getId());
    }

    /** Pode incluir/editar/excluir um Tipo de Peça de uma Área específica. */
    public boolean podeGerenciarTipoPeca(Pessoa usuario, Area area) {
        if (ehAdminOuGestor(usuario)) {
            return true;
        }
        return usuario.getReferenciaArea() != null
                && usuario.getReferenciaArea().getId().equals(area.getId());
    }

    /** Pode excluir uma Evidência: permitido se puder gerenciar a
     * Atividade OU a Peça a que ela está vinculada. */
    public boolean podeGerenciarEvidencia(Pessoa usuario, Evidencia evidencia) {
        boolean pelaAtividade = evidencia.getAtividade() != null
                && podeGerenciar(usuario, evidencia.getAtividade().getPessoa());
        boolean pelaPeca = evidencia.getPeca() != null
                && podeGerenciar(usuario, evidencia.getPeca().getPessoa());
        return pelaAtividade || pelaPeca;
    }

    private boolean ehAdminOuGestor(Pessoa usuario) {
        return usuario.getPerfil() == PerfilPessoa.ADMIN || usuario.getPerfil() == PerfilPessoa.GESTOR;
    }
}