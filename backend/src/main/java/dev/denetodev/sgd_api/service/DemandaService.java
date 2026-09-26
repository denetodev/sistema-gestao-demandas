package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.DemandaRequest;
import dev.denetodev.sgd_api.dto.response.DemandaResponse;
import dev.denetodev.sgd_api.entity.*;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    public DemandaService(
            DemandaRepository demandaRepository,
            DiretoriaRepository diretoriaRepository,
            DemandanteRepository demandanteRepository,
            ProjetoRepository projetoRepository,
            CampanhaRepository campanhaRepository
    ) {
        this.demandaRepository = demandaRepository;
        this.diretoriaRepository = diretoriaRepository;
        this.demandanteRepository = demandanteRepository;
        this.projetoRepository = projetoRepository;
        this.campanhaRepository = campanhaRepository;
    }

    @Transactional(readOnly = true)
    public List<DemandaResponse> listarTodas() {
        return demandaRepository.findAll().stream()
                .map(this::paraResponse)
                .toList();
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

        // PUT é substituição completa: demandante/projeto/campanha são
        // resolvidos e ATRIBUÍDOS SEMPRE — inclusive limpando (null) se o
        // request não trouxer o id, diferente do criar() (onde "ausente"
        // simplesmente não seta nada, porque no create já nasce null).
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
        // quando existir usuário autenticado pra atribuir a mudança.

        return paraResponse(demanda);
    }

    public void cancelar(UUID id) {
        atualizarStatus(id, StatusDemanda.CANCELADA);
    }

    private DemandaResponse paraResponse(Demanda demanda) {
        Demandante demandante = demanda.getDemandante();
        Projeto projeto = demanda.getProjeto();
        Campanha campanha = demanda.getCampanha();

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