package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.ProjetoRequest;
import dev.denetodev.sgd_api.dto.response.ProjetoResponse;
import dev.denetodev.sgd_api.entity.Demandante;
import dev.denetodev.sgd_api.entity.Diretoria;
import dev.denetodev.sgd_api.entity.Projeto;
import dev.denetodev.sgd_api.entity.StatusProjeto;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.DemandanteRepository;
import dev.denetodev.sgd_api.repository.DiretoriaRepository;
import dev.denetodev.sgd_api.repository.ProjetoRepository;
import dev.denetodev.sgd_api.service.support.Resolvers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ProjetoService {

    private final ProjetoRepository projetoRepository;
    private final DemandanteRepository demandanteRepository;
    private final DiretoriaRepository diretoriaRepository;

    public ProjetoService(ProjetoRepository projetoRepository, DemandanteRepository demandanteRepository, DiretoriaRepository diretoriaRepository) {
        this.projetoRepository = projetoRepository;
        this.demandanteRepository = demandanteRepository;
        this.diretoriaRepository = diretoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjetoResponse> listarTodas() {
        return projetoRepository.findAll().stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProjetoResponse buscarPorId(UUID id) {
        return paraResponse(buscarEntidade(id));
    }

    public ProjetoResponse criar(ProjetoRequest request) {
        Projeto projeto = new Projeto(request.nome());
        projeto.setDemandante(Resolvers.resolverOuNulo(request.demandanteId(), demandanteRepository, "Demandante"));
        projeto.setDiretoria(Resolvers.resolverOuNulo(request.diretoriaId(), diretoriaRepository, "Diretoria"));
        projeto.setDescricao(request.descricao());
        if (request.status() != null) {
            projeto.setStatus(request.status());
        }
        return paraResponse(projetoRepository.save(projeto));
    }

    public ProjetoResponse atualizar(UUID id, ProjetoRequest request) {
        Projeto projeto = buscarEntidade(id);
        projeto.setNome(request.nome());
        projeto.setDemandante(Resolvers.resolverOuNulo(request.demandanteId(), demandanteRepository, "Demandante"));
        projeto.setDiretoria(Resolvers.resolverOuNulo(request.diretoriaId(), diretoriaRepository, "Diretoria"));
        projeto.setDescricao(request.descricao());
        if (request.status() != null) {
            projeto.setStatus(request.status());
        }
        return paraResponse(projeto);
    }

    public void encerrar(UUID id) {
        buscarEntidade(id).setStatus(StatusProjeto.ENCERRADO);
    }

    private Projeto buscarEntidade(UUID id) {
        return projetoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Projeto não encontrado: " + id));
    }

    private ProjetoResponse paraResponse(Projeto p) {
        Demandante demandante = p.getDemandante();
        Diretoria diretoria = p.getDiretoria();
        return new ProjetoResponse(
                p.getId(), p.getNome(),
                demandante != null ? demandante.getId() : null,
                demandante != null ? demandante.getNome() : null,
                diretoria != null ? diretoria.getId() : null,
                diretoria != null ? diretoria.getNome() : null,
                p.getDescricao(), p.getStatus(),
                p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}