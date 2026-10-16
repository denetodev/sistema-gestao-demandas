package dev.denetodev.sgd_api.service.support;

import dev.denetodev.sgd_api.entity.PapelEquipamentos;
import dev.denetodev.sgd_api.entity.PerfilPessoa;
import dev.denetodev.sgd_api.entity.Pessoa;
import org.springframework.security.access.AccessDeniedException;

/**
 * Regras do módulo de equipamentos, sem estado (usadas também para montar a resposta da própria pessoa).
 *
 * <ul>
 *   <li>Acesso (ver e usar): Admin e Gestor, que veem tudo; e Profissional que tenha um papel no módulo
 *       (Operador de estúdio ou Gestor de equipamentos). Sem papel, nem para consultar. Visualizador nunca.</li>
 *   <li>Gerencia (edita o inventário e autoriza ou recusa empréstimos): Admin, Gestor e Gestor de equipamentos.</li>
 *   <li>Opera (registra empréstimo, entrega e recebe a devolução): quem gerencia mais o Operador de estúdio.</li>
 * </ul>
 */
public final class EquipamentoPermissao {

    private EquipamentoPermissao() {
    }

    private static boolean ehGestao(Pessoa p) {
        return p.getPerfil() == PerfilPessoa.ADMIN || p.getPerfil() == PerfilPessoa.GESTOR;
    }

    public static boolean temAcesso(Pessoa p) {
        if (p.getPerfil() == PerfilPessoa.VISUALIZADOR) {
            return false;
        }
        return ehGestao(p) || p.getPapelEquipamentos() != null;
    }

    public static boolean gerencia(Pessoa p) {
        return temAcesso(p) && (ehGestao(p) || p.getPapelEquipamentos() == PapelEquipamentos.GESTOR_DE_EQUIPAMENTOS);
    }

    public static boolean opera(Pessoa p) {
        return gerencia(p) || (temAcesso(p) && p.getPapelEquipamentos() == PapelEquipamentos.OPERADOR_DE_ESTUDIO);
    }

    public static void exigirAcesso(Pessoa p) {
        if (!temAcesso(p)) {
            throw new AccessDeniedException("Sem acesso ao módulo de equipamentos");
        }
    }

    public static void exigirGerencia(Pessoa p) {
        if (!gerencia(p)) {
            throw new AccessDeniedException("Só Gestor de equipamentos, Gestor ou Admin pode fazer isso");
        }
    }

    public static void exigirOperacao(Pessoa p) {
        if (!opera(p)) {
            throw new AccessDeniedException("Só Operador de estúdio, Gestor de equipamentos, Gestor ou Admin pode fazer isso");
        }
    }
}
