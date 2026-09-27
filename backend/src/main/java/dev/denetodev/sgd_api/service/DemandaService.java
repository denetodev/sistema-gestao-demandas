package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.DemandaRequest;
import dev.denetodev.sgd_api.dto.response.DemandaResponse;
import dev.denetodev.sgd_api.entity.*;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.*;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import dev.denetodev.sgd_api.service.support.PermissaoService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
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

    public DemandaService(
            DemandaRepository demandaRepository,
            DiretoriaRepository diretoriaRepository,
            DemandanteRepository demandanteRepository,
            ProjetoRepository projetoRepository,
            CampanhaRepository campanhaRepository,
            PecaRepository pecaRepository,
            PessoaDemandaRepository pessoaDemandaRepository,
            CurrentPessoaResolver currentPessoaResolver,
            PermissaoService permissaoService
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

    public DemandaResponse criar(DemandaRequest request) {
        Diretoria diretoria = diretoriaRepository.findById(request.diretoriaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Diretoria não encontrada: " + request.diretoriaId()));

        Demanda demanda = new Demanda(request.titulo(), diretoria);
        demanda.setDescricao(request.descricao());
        demanda.setCodigo(request.codigo());
        demanda.setDataPrazo(request.dataPrazo());
        demanda.setValor(request.valor());
        demanda.setObservacoes(request.observacoes());

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
        demanda.setValor(request.valor());
        demanda.setObservacoes(request.observacoes());
        demanda.setPrioridade(request.prioridade() != null ? request.prioridade() : demanda.getPrioridade());

        demanda.setDemandante(resolverOuNulo(request.demandanteId(), demandanteRepository, "Demandante"));
        demanda.setProjeto(resolverOuNulo(request.projetoId(), projetoRepository, "Projeto"));
        demanda.setCampanha(resolverOuNulo(request.campanhaId(), campanhaRepository, "Campanha"));

        return paraResponse(demanda);
    }

    public DemandaResponse atualizarStatus(UUID id, StatusDemanda novoStatus) {
        Demanda demanda = demandaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Demanda não encontrada: " + id));

        demanda.setStatus(novoStatus);
        // TODO(M3): registrar em auditoria (usuário, data, status anterior, motivo)

        return paraResponse(demanda);
    }

    public void cancelar(UUID id) {
        atualizarStatus(id, StatusDemanda.CANCELADA);
    }

    private DemandaResponse paraResponse(Demanda demanda) {
        Demandante demandante = demanda.getDemandante();
        Projeto projeto = demanda.getProjeto();
        Campanha campanha = demanda.getCampanha();

        List<Peca> pecas = pecaRepository.findByDemandaId(demanda.getId());
        BigDecimal valorCalculado = pecas.isEmpty()
                ? null
                : pecas.stream().map(Peca::getValorTotal).reduce(BigDecimal.ZERO, BigDecimal::add);

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
                valorCalculado,
                demanda.getCreatedAt(),
                demanda.getUpdatedAt()
        );
    }

    private <T> T resolverOuNulo(UUID id, org.springframework.data.jpa.repository.JpaRepository<T, UUID> repository, String nomeEntidade) {
        if (id == null) {
            return null;
        }
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(nomeEntidade + " não encontrado: " + id));
    }
}