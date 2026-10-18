package dev.denetodev.sgd_api.service.support;

import dev.denetodev.sgd_api.entity.Area;
import dev.denetodev.sgd_api.entity.EscopoListagem;
import dev.denetodev.sgd_api.entity.Evidencia;
import dev.denetodev.sgd_api.entity.PerfilPessoa;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.entity.PessoaDemanda;
import dev.denetodev.sgd_api.repository.PessoaDemandaRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Component
public class PermissaoService {

    private final PessoaDemandaRepository pessoaDemandaRepository;

    public PermissaoService(PessoaDemandaRepository pessoaDemandaRepository) {
        this.pessoaDemandaRepository = pessoaDemandaRepository;
    }

    /**
     * Só quem participa da demanda altera algo nela: participante ativo, Referência da área de alguém
     * do time ativo, Gestor e Admin. Visualizador nunca altera.
     */
    public boolean podeAlterarDemanda(Pessoa usuario, UUID demandaId) {
        if (usuario.getPerfil() == PerfilPessoa.VISUALIZADOR) {
            return false;
        }
        if (ehAdminOuGestor(usuario)) {
            return true;
        }
        return podeAlterarDemanda(usuario, pessoaDemandaRepository.findByDemandaId(demandaId));
    }

    /** Mesma regra, com o time da demanda já carregado (evita uma consulta por demanda ao listar). */
    public boolean podeAlterarDemanda(Pessoa usuario, Collection<PessoaDemanda> vinculos) {
        if (usuario.getPerfil() == PerfilPessoa.VISUALIZADOR) {
            return false;
        }
        if (ehAdminOuGestor(usuario)) {
            return true;
        }
        List<PessoaDemanda> time = vinculos.stream()
                .filter(v -> v.getDataSaida() == null)
                .toList();
        boolean participa = time.stream().anyMatch(v -> v.getPessoa().getId().equals(usuario.getId()));
        if (participa) {
            return true;
        }
        return usuario.getReferenciaArea() != null && time.stream().anyMatch(v ->
                v.getPessoa().getArea() != null
                        && v.getPessoa().getArea().getId().equals(usuario.getReferenciaArea().getId()));
    }

    public void exigirPodeAlterarDemanda(Pessoa usuario, UUID demandaId) {
        if (!podeAlterarDemanda(usuario, demandaId)) {
            throw new AccessDeniedException("Só quem participa da demanda pode alterá-la");
        }
    }

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
     * lança 403 quando não permite. MINHAS, EQUIPE, NUCLEO e DIRETORIA são livres
     * para qualquer vinculado; TODAS é de Gestor e Admin. */
    public void validarEscopo(Pessoa usuario, EscopoListagem escopo) {
        if (escopo == EscopoListagem.EQUIPE && areaDaEquipe(usuario) == null) {
            throw new AccessDeniedException("Escopo 'equipe' exige ter uma equipe (área) definida");
        }
        if (escopo == EscopoListagem.NUCLEO && (usuario.getArea() == null || usuario.getArea().getNucleo() == null)) {
            throw new AccessDeniedException("Escopo 'núcleo' exige que a sua equipe esteja ligada a um núcleo");
        }
        if (escopo == EscopoListagem.TODAS && !ehAdminOuGestor(usuario)) {
            throw new AccessDeniedException("Escopo 'todas' restrito a ADMIN/GESTOR");
        }
    }

    /**
     * A equipe da pessoa: a área que ela referencia (se for Referência) ou, senão, a própria área.
     * É o que o botão "Minha equipe" mostra.
     */
    public Area areaDaEquipe(Pessoa usuario) {
        return usuario.getReferenciaArea() != null ? usuario.getReferenciaArea() : usuario.getArea();
    }

    /**
     * Quem cria e edita projeto e campanha: Admin, Gestor e a Referência de equipe (Profissional com referencia_area).
     * Profissional comum e Visualizador não.
     */
    public boolean podeGerenciarProjetosECampanhas(Pessoa usuario) {
        if (ehAdminOuGestor(usuario)) {
            return true;
        }
        return usuario.getPerfil() == PerfilPessoa.PROFISSIONAL && usuario.getReferenciaArea() != null;
    }

    public void exigirGerenciarProjetosECampanhas(Pessoa usuario) {
        if (!podeGerenciarProjetosECampanhas(usuario)) {
            throw new AccessDeniedException("Só Gestor, Admin ou Referência de equipe criam e editam projetos e campanhas");
        }
    }

    private boolean ehAdminOuGestor(Pessoa usuario) {
        return usuario.getPerfil() == PerfilPessoa.ADMIN || usuario.getPerfil() == PerfilPessoa.GESTOR;
    }
}