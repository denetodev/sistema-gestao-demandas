package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.DemandaRequest;
import dev.denetodev.sgd_api.dto.response.DemandaResponse;
import dev.denetodev.sgd_api.dto.response.HistoricoStatusResponse;
import dev.denetodev.sgd_api.entity.*;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.*;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import dev.denetodev.sgd_api.service.support.PermissaoService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional
public class DemandaService {

    private final DemandaRepository demandaRepository;
    private final DiretoriaRepository diretoriaRepository;
    private final DemandanteRepository demandanteRepository;
    private final ProjetoRepository projetoRepository;
    private final CampanhaRepository campanhaRepository;
    private final PecaRepository pecaRepository;
    private final PessoaDemandaRepository pessoaDemandaRepository;
    private final CurrentPessoaResolver currentPessoaResolver;
    private final PermissaoService permissaoService;
    private final AuditoriaRepository auditoriaRepository;
    private final PessoaRepository pessoaRepository;

    public DemandaService(
            DemandaRepository demandaRepository,
            DiretoriaRepository diretoriaRepository,
            DemandanteRepository demandanteRepository,
            ProjetoRepository projetoRepository,
            CampanhaRepository campanhaRepository,
            PecaRepository pecaRepository,
            PessoaDemandaRepository pessoaDemandaRepository,
            CurrentPessoaResolver currentPessoaResolver,
            PermissaoService permissaoService,
            AuditoriaRepository auditoriaRepository,
            PessoaRepository pessoaRepository
    ) {
        this.demandaRepository = demandaRepository;
        this.diretoriaRepository = diretoriaRepository;
        this.demandanteRepository = demandanteRepository;
        this.projetoRepository = projetoRepository;
        this.campanhaRepository = campanhaRepository;
        this.pecaRepository = pecaRepository;
        this.pessoaDemandaRepository = pessoaDemandaRepository;
        this.currentPessoaResolver = currentPessoaResolver;
        this.permissaoService = permissaoService;
        this.auditoriaRepository = auditoriaRepository;
        this.pessoaRepository = pessoaRepository;
    }

    @Transactional(readOnly = true)
    public Page<DemandaResponse> listarComEscopo(Jwt jwt, EscopoListagem escopo, Pageable pageable) {
        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        permissaoService.validarEscopo(usuario, escopo);

        Page<Demanda> pagina = switch (escopo) {
            case MINHAS -> demandaRepository.findMinhas(usuario.getId(), pageable);
            case EQUIPE -> demandaRepository.findEquipe(usuario.getReferenciaArea().getId(), pageable);
            case DIRETORIA -> demandaRepository.findByDiretoriaId(usuario.getArea().getDiretoria().getId(), pageable);
            case TODAS -> demandaRepository.findAll(pageable);
        };

        return pagina.map(this::paraResponse);
    }

    @Transactional(readOnly = true)
    public DemandaResponse buscarPorId(UUID id) {
        Demanda demanda = demandaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Demanda não encontrada: " + id));
        return paraResponse(demanda);
    }

    public DemandaResponse criar(Jwt jwt, DemandaRequest request) {
        Diretoria diretoria = diretoriaRepository.findById(request.diretoriaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Diretoria não encontrada: " + request.diretoriaId()));

        Demanda demanda = new Demanda(request.titulo(), diretoria);
        demanda.setDescricao(request.descricao());
        demanda.setCodigo(request.codigo());
        demanda.setDataPrazo(request.dataPrazo());
        demanda.setObservacoes(request.observacoes());
        demanda.setLinkExterno(normalizarLink(request.linkExterno()));

        if (request.prioridade() != null) {
            demanda.setPrioridade(request.prioridade());
        }

        if (request.demandanteId() != null) {
            Demandante demandante = demandanteRepository.findById(request.demandanteId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Demandante não encontrado: " + request.demandanteId()));
            demanda.setDemandante(demandante);
        }

        if (request.projetoId() != null) {
            Projeto projeto = projetoRepository.findById(request.projetoId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto não encontrado: " + request.projetoId()));
            demanda.setProjeto(projeto);
        }

        if (request.campanhaId() != null) {
            Campanha campanha = campanhaRepository.findById(request.campanhaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Campanha não encontrada: " + request.campanhaId()));
            demanda.setCampanha(campanha);
        }

        Demanda salva = demandaRepository.save(demanda);

        // quem cria a demanda entra no time como Responsável principal
        Pessoa criador = currentPessoaResolver.resolver(jwt);
        PessoaDemanda vinculo = new PessoaDemanda(criador, salva);
        vinculo.setPapel(PapelPessoaDemanda.RESPONSAVEL_PRINCIPAL);
        pessoaDemandaRepository.save(vinculo);

        return paraResponse(salva);
    }

    public DemandaResponse atualizar(UUID id, DemandaRequest request) {
        Demanda demanda = demandaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Demanda não encontrada: " + id));

        Diretoria diretoria = diretoriaRepository.findById(request.diretoriaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Diretoria não encontrada: " + request.diretoriaId()));

        demanda.setTitulo(request.titulo());
        demanda.setDescricao(request.descricao());
        demanda.setCodigo(request.codigo());
        demanda.setDiretoria(diretoria);
        demanda.setDataPrazo(request.dataPrazo());
        demanda.setObservacoes(request.observacoes());
        demanda.setLinkExterno(normalizarLink(request.linkExterno()));
        demanda.setPrioridade(request.prioridade() != null ? request.prioridade() : demanda.getPrioridade());

        demanda.setDemandante(resolverOuNulo(request.demandanteId(), demandanteRepository, "Demandante"));
        demanda.setProjeto(resolverOuNulo(request.projetoId(), projetoRepository, "Projeto"));
        demanda.setCampanha(resolverOuNulo(request.campanhaId(), campanhaRepository, "Campanha"));

        return paraResponse(demanda);
    }

    public DemandaResponse atualizarStatus(Jwt jwt, UUID id, StatusDemanda novoStatus, String motivo) {
        return atualizarStatus(currentPessoaResolver.resolver(jwt), id, novoStatus, motivo);
    }

    /**
     * Muda o status e registra na auditoria quem mudou, de quê para quê e o motivo.
     * Visualizador não altera; cancelar a demanda inteira é de Gestor, Admin ou do Responsável principal.
     */
    DemandaResponse atualizarStatus(Pessoa usuario, UUID id, StatusDemanda novoStatus, String motivo) {
        Demanda demanda = demandaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Demanda não encontrada: " + id));

        if (usuario.getPerfil() == PerfilPessoa.VISUALIZADOR) {
            throw new AccessDeniedException("Visualizador não altera demandas");
        }
        if (novoStatus == StatusDemanda.CANCELADA && !podeCancelar(usuario, id)) {
            throw new AccessDeniedException("Só Gestor, Admin ou o Responsável principal cancelam a demanda");
        }

        StatusDemanda anterior = demanda.getStatus();
        if (anterior != novoStatus) {
            demanda.setStatus(novoStatus);
            Auditoria registro = new Auditoria("demanda", id, "status", anterior.name(), novoStatus.name(), usuario.getId());
            registro.setMotivo(motivo == null || motivo.isBlank() ? null : motivo.trim());
            auditoriaRepository.save(registro);
        }

        return paraResponse(demanda);
    }

    public void cancelar(Jwt jwt, UUID id) {
        atualizarStatus(jwt, id, StatusDemanda.CANCELADA, null);
    }

    @Transactional(readOnly = true)
    public List<HistoricoStatusResponse> historicoDeStatus(UUID id) {
        if (!demandaRepository.existsById(id)) {
            throw new RecursoNaoEncontradoException("Demanda não encontrada: " + id);
        }
        List<Auditoria> registros = auditoriaRepository
                .findByTabelaAndRegistroIdAndCampoOrderByCreatedAtDesc("demanda", id, "status");
        Map<UUID, String> nomes = new HashMap<>();
        pessoaRepository.findAllById(registros.stream().map(Auditoria::getAlteradoPor).filter(Objects::nonNull).distinct().toList())
                .forEach(p -> nomes.put(p.getId(), p.getNome()));

        return registros.stream()
                .map(r -> new HistoricoStatusResponse(
                        r.getCreatedAt(), r.getValorAnterior(), r.getValorNovo(),
                        r.getAlteradoPor(), r.getAlteradoPor() != null ? nomes.get(r.getAlteradoPor()) : null,
                        r.getMotivo()))
                .toList();
    }

    private boolean podeCancelar(Pessoa usuario, UUID demandaId) {
        if (usuario.getPerfil() == PerfilPessoa.ADMIN || usuario.getPerfil() == PerfilPessoa.GESTOR) {
            return true;
        }
        return pessoaDemandaRepository.findByPessoaIdAndDemandaIdAndDataSaidaIsNull(usuario.getId(), demandaId)
                .map(v -> v.getPapel() == PapelPessoaDemanda.RESPONSAVEL_PRINCIPAL)
                .orElse(false);
    }

    private DemandaResponse paraResponse(Demanda demanda) {
        Demandante demandante = demanda.getDemandante();
        Projeto projeto = demanda.getProjeto();
        Campanha campanha = demanda.getCampanha();

        List<Peca> pecas = pecaRepository.findByDemandaId(demanda.getId());
        // Valor vem das peças. O campo manual só vale para linhas importadas da planilha
        // (source_system preenchido) ainda sem peças; a API não grava mais esse campo.
        BigDecimal valorCalculado = !pecas.isEmpty()
                ? pecas.stream().map(Peca::getValorTotal).reduce(BigDecimal.ZERO, BigDecimal::add)
                : (demanda.getSourceSystem() != null ? demanda.getValor() : null);

        return new DemandaResponse(
                demanda.getId(),
                demanda.getTitulo(),
                demanda.getDescricao(),
                demanda.getCodigo(),
                demanda.getDiretoria().getId(),
                demanda.getDiretoria().getNome(),
                demandante != null ? demandante.getId() : null,
                demandante != null ? demandante.getNome() : null,
                projeto != null ? projeto.getId() : null,
                projeto != null ? projeto.getNome() : null,
                campanha != null ? campanha.getId() : null,
                campanha != null ? campanha.getNome() : null,
                demanda.getPrioridade(),
                demanda.getStatus(),
                demanda.getDataCriacao(),
                demanda.getDataPrazo(),
                demanda.getDataEntregaReal(),
                demanda.getValor(),
                demanda.getObservacoes(),
                demanda.getLinkExterno(),
                valorCalculado,
                demanda.getCreatedAt(),
                demanda.getUpdatedAt()
        );
    }

    /** Link para Planner/ClickUp: vazio vira nulo; só aceita http(s). */
    private String normalizarLink(String link) {
        if (link == null || link.isBlank()) {
            return null;
        }
        String limpo = link.trim();
        String minusculo = limpo.toLowerCase();
        if (!minusculo.startsWith("http://") && !minusculo.startsWith("https://")) {
            throw new IllegalArgumentException("linkExterno deve começar com http:// ou https://");
        }
        return limpo;
    }

    private <T> T resolverOuNulo(UUID id, org.springframework.data.jpa.repository.JpaRepository<T, UUID> repository, String nomeEntidade) {
        if (id == null) {
            return null;
        }
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(nomeEntidade + " não encontrado: " + id));
    }
}