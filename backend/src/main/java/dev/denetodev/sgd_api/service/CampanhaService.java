package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.CampanhaRequest;
import dev.denetodev.sgd_api.dto.response.CampanhaResponse;
import dev.denetodev.sgd_api.entity.Campanha;
import dev.denetodev.sgd_api.entity.Projeto;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.CampanhaRepository;
import dev.denetodev.sgd_api.repository.ProjetoRepository;
import dev.denetodev.sgd_api.service.support.Resolvers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CampanhaService {

    private final CampanhaRepository campanhaRepository;
    private final ProjetoRepository projetoRepository;

    public CampanhaService(CampanhaRepository campanhaRepository, ProjetoRepository projetoRepository) {
        this.campanhaRepository = campanhaRepository;
        this.projetoRepository = projetoRepository;
    }

    @Transactional(readOnly = true)
    public List<CampanhaResponse> listarTodas() {
        return campanhaRepository.findAll().stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public CampanhaResponse buscarPorId(UUID id) {
        return paraResponse(buscarEntidade(id));
    }

    public CampanhaResponse criar(CampanhaRequest request) {
        Campanha campanha = new Campanha(request.nome());
        campanha.setProjeto(Resolvers.resolverOuNulo(request.projetoId(), projetoRepository, "Projeto"));
        campanha.setCodigo(request.codigo());
        campanha.setDataInicio(request.dataInicio());
        campanha.setDataFim(request.dataFim());
        return paraResponse(campanhaRepository.save(campanha));
    }

    public CampanhaResponse atualizar(UUID id, CampanhaRequest request) {
        Campanha campanha = buscarEntidade(id);
        campanha.setNome(request.nome());
        campanha.setProjeto(Resolvers.resolverOuNulo(request.projetoId(), projetoRepository, "Projeto"));
        campanha.setCodigo(request.codigo());
        campanha.setDataInicio(request.dataInicio());
        campanha.setDataFim(request.dataFim());
        return paraResponse(campanha);
    }

    public void remover(UUID id) {
        // delete de verdade — sem campo de arquivamento na tabela.
        // Se a campanha estiver em uso por alguma Demanda, a FK do Postgres
        // recusa e o GlobalExceptionHandler traduz pra 409 automaticamente.
        campanhaRepository.delete(buscarEntidade(id));
    }

    private Campanha buscarEntidade(UUID id) {
        return campanhaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Campanha não encontrada: " + id));
    }

    private CampanhaResponse paraResponse(Campanha c) {
        Projeto projeto = c.getProjeto();
        return new CampanhaResponse(
                c.getId(), c.getNome(),
                projeto != null ? projeto.getId() : null,
                projeto != null ? projeto.getNome() : null,
                c.getCodigo(), c.getDataInicio(), c.getDataFim(),
                c.getCreatedAt(), c.getUpdatedAt()
        );
    }
}