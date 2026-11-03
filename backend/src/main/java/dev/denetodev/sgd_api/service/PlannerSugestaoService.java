package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.response.PlannerSugestaoResponse;
import dev.denetodev.sgd_api.entity.Demanda;
import dev.denetodev.sgd_api.entity.Diretoria;
import dev.denetodev.sgd_api.entity.PapelPessoaDemanda;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.entity.PessoaDemanda;
import dev.denetodev.sgd_api.entity.PlannerSugestao;
import dev.denetodev.sgd_api.exception.EstadoInvalidoException;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.DemandaRepository;
import dev.denetodev.sgd_api.repository.DiretoriaRepository;
import dev.denetodev.sgd_api.repository.PessoaDemandaRepository;
import dev.denetodev.sgd_api.repository.PlannerSugestaoRepository;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * As sugestões vindas do Planner: cada pessoa vê, aceita ou recusa só as suas. Aceitar é o que faz a tarefa virar
 * demanda do sistema (ou coloca a pessoa na demanda que já existe com o mesmo código).
 */
@Service
@Transactional
public class PlannerSugestaoService {

    /** De onde vem o que o Planner traz: marca a origem das demandas criadas por aqui. */
    static final String ORIGEM = "planner";

    private final CurrentPessoaResolver currentPessoaResolver;
    private final PlannerSugestaoRepository sugestaoRepository;
    private final DemandaRepository demandaRepository;
    private final DiretoriaRepository diretoriaRepository;
    private final PessoaDemandaRepository pessoaDemandaRepository;
    private final EntityManager entityManager;
    private final Clock clock;
    private final String diretoriaPadrao;

    public PlannerSugestaoService(CurrentPessoaResolver currentPessoaResolver, PlannerSugestaoRepository sugestaoRepository,
                                  DemandaRepository demandaRepository, DiretoriaRepository diretoriaRepository,
                                  PessoaDemandaRepository pessoaDemandaRepository, EntityManager entityManager, Clock clock,
                                  @Value("${app.planner.diretoria:COE/CRM}") String diretoriaPadrao) {
        this.currentPessoaResolver = currentPessoaResolver;
        this.sugestaoRepository = sugestaoRepository;
        this.demandaRepository = demandaRepository;
        this.diretoriaRepository = diretoriaRepository;
        this.pessoaDemandaRepository = pessoaDemandaRepository;
        this.entityManager = entityManager;
        this.clock = clock;
        this.diretoriaPadrao = diretoriaPadrao;
    }

    @Transactional(readOnly = true)
    public List<PlannerSugestaoResponse> minhas(Jwt jwt) {
        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        return sugestaoRepository.pendentesDa(usuario.getId()).stream().map(this::paraResponse).toList();
    }

    /** Aceita: devolve a demanda em que a pessoa entrou (criada agora ou já existente). */
    public UUID aceitar(Jwt jwt, UUID id) {
        return aceitar(currentPessoaResolver.resolver(jwt), id);
    }

    UUID aceitar(Pessoa usuario, UUID id) {
        PlannerSugestao sugestao = pendenteDa(usuario, id);
        OffsetDateTime agora = OffsetDateTime.now(clock);

        Demanda demanda;
        PapelPessoaDemanda papel = PapelPessoaDemanda.PARTICIPANTE;
        String codigo = sugestao.getCodigo();
        if (codigo != null) {
            // dois aceites ao mesmo tempo da mesma tarefa não podem criar a demanda duas vezes
            entityManager.createNativeQuery("select pg_advisory_xact_lock(hashtext(:codigo))")
                    .setParameter("codigo", codigo.toLowerCase())
                    .getSingleResult();
        }
        List<Demanda> existentes = codigo == null ? List.of() : demandaRepository.findByCodigoIgnoreCase(codigo);
        if (!existentes.isEmpty()) {
            demanda = existentes.get(0);
        } else {
            Diretoria diretoria = diretoriaRepository.findByNomeIgnoreCase(diretoriaPadrao)
                    .orElseThrow(() -> new EstadoInvalidoException("A diretoria padrão do Planner (" + diretoriaPadrao + ") não existe"));
            Demanda nova = new Demanda(sugestao.getTitulo(), diretoria);
            nova.setCodigo(codigo);
            nova.setSourceSystem(ORIGEM);
            demanda = demandaRepository.save(nova);
            // quem aceita primeiro e cria a demanda entra como Responsável principal, como em qualquer demanda nova
            papel = PapelPessoaDemanda.RESPONSAVEL_PRINCIPAL;
        }

        boolean jaNoTime = pessoaDemandaRepository
                .findByPessoaIdAndDemandaIdAndDataSaidaIsNull(usuario.getId(), demanda.getId()).isPresent();
        if (!jaNoTime) {
            PessoaDemanda vinculo = new PessoaDemanda(usuario, demanda);
            vinculo.registrarAdicao(usuario, agora);
            vinculo.setPapel(papel);
            pessoaDemandaRepository.save(vinculo);
        }
        sugestao.aceitar(demanda, agora);
        return demanda.getId();
    }

    public void recusar(Jwt jwt, UUID id) {
        recusar(currentPessoaResolver.resolver(jwt), id);
    }

    void recusar(Pessoa usuario, UUID id) {
        pendenteDa(usuario, id).recusar(OffsetDateTime.now(clock));
    }

    @Transactional(readOnly = true)
    public long contarPendentes(Pessoa usuario) {
        return sugestaoRepository.countByPessoaIdAndStatus(usuario.getId(), PlannerSugestao.Status.PENDENTE);
    }

    /** Só a própria pessoa decide, e só enquanto está pendente. Sugestão de outra pessoa se comporta como inexistente. */
    private PlannerSugestao pendenteDa(Pessoa usuario, UUID id) {
        PlannerSugestao sugestao = sugestaoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sugestão não encontrada: " + id));
        if (!sugestao.getPessoa().getId().equals(usuario.getId())) {
            throw new AccessDeniedException("Essa sugestão é de outra pessoa");
        }
        if (sugestao.getStatus() != PlannerSugestao.Status.PENDENTE) {
            throw new EstadoInvalidoException("Essa sugestão já foi decidida");
        }
        return sugestao;
    }

    private PlannerSugestaoResponse paraResponse(PlannerSugestao s) {
        Demanda existente = s.getCodigo() == null ? null : demandaRepository.findByCodigoIgnoreCase(s.getCodigo()).stream().findFirst().orElse(null);
        return new PlannerSugestaoResponse(s.getId(), s.getCodigo(), s.getTitulo(), s.getBucket(), s.getEtiqueta(),
                s.getCriadaNoPlanner(), existente == null ? null : existente.getId(), existente == null ? null : existente.getTitulo());
    }
}
