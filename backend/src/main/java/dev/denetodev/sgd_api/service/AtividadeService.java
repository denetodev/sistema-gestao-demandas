package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.AtividadeRequest;
import dev.denetodev.sgd_api.dto.response.AtividadeResponse;
import dev.denetodev.sgd_api.entity.Atividade;
import dev.denetodev.sgd_api.entity.Demanda;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.entity.TipoAtividade;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.AtividadeRepository;
import dev.denetodev.sgd_api.repository.DemandaRepository;
import dev.denetodev.sgd_api.repository.PessoaRepository;
import dev.denetodev.sgd_api.repository.TipoAtividadeRepository;
import dev.denetodev.sgd_api.service.support.Resolvers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AtividadeService {

    private final AtividadeRepository atividadeRepository;
    private final DemandaRepository demandaRepository;
    private final TipoAtividadeRepository tipoAtividadeRepository;
    private final PessoaRepository pessoaRepository;

    public AtividadeService(
            AtividadeRepository atividadeRepository,
            DemandaRepository demandaRepository,
            TipoAtividadeRepository tipoAtividadeRepository,
            PessoaRepository pessoaRepository
    ) {
        this.atividadeRepository = atividadeRepository;
        this.demandaRepository = demandaRepository;
        this.tipoAtividadeRepository = tipoAtividadeRepository;
        this.pessoaRepository = pessoaRepository;
    }

    @Transactional(readOnly = true)
    public List<AtividadeResponse> listarTodas() {
        return atividadeRepository.findAll().stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<AtividadeResponse> listarPorDemanda(UUID demandaId) {
        return atividadeRepository.findByDemandaId(demandaId).stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public AtividadeResponse buscarPorId(UUID id) {
        return paraResponse(buscarEntidade(id));
    }

    public AtividadeResponse criar(AtividadeRequest request) {
        TipoAtividade tipo = Resolvers.resolverObrigatorio(request.tipoAtividadeId(), tipoAtividadeRepository, "Tipo de atividade");

        Atividade atividade = new Atividade(tipo);
        atividade.setDemanda(Resolvers.resolverOuNulo(request.demandaId(), demandaRepository, "Demanda"));
        atividade.setPessoa(Resolvers.resolverOuNulo(request.pessoaId(), pessoaRepository, "Pessoa"));
        atividade.setDescricao(request.descricao());
        if (request.dataRealizacao() != null) {
            atividade.setDataRealizacao(request.dataRealizacao());
        }

        return paraResponse(atividadeRepository.save(atividade));
    }

    public AtividadeResponse atualizar(UUID id, AtividadeRequest request) {
        Atividade atividade = buscarEntidade(id);
        TipoAtividade tipo = Resolvers.resolverObrigatorio(request.tipoAtividadeId(), tipoAtividadeRepository, "Tipo de atividade");

        atividade.setTipoAtividade(tipo);
        atividade.setDemanda(Resolvers.resolverOuNulo(request.demandaId(), demandaRepository, "Demanda"));
        atividade.setPessoa(Resolvers.resolverOuNulo(request.pessoaId(), pessoaRepository, "Pessoa"));
        atividade.setDescricao(request.descricao());
        if (request.dataRealizacao() != null) {
            atividade.setDataRealizacao(request.dataRealizacao());
        }

        return paraResponse(atividade);
    }

    public void remover(UUID id) {
        atividadeRepository.delete(buscarEntidade(id));
    }

    private Atividade buscarEntidade(UUID id) {
        return atividadeRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Atividade não encontrada: " + id));
    }

    private AtividadeResponse paraResponse(Atividade a) {
        Demanda demanda = a.getDemanda();
        Pessoa pessoa = a.getPessoa();
        return new AtividadeResponse(
                a.getId(),
                demanda != null ? demanda.getId() : null,
                demanda != null ? demanda.getTitulo() : null,
                a.getTipoAtividade().getId(), a.getTipoAtividade().getNome(),
                pessoa != null ? pessoa.getId() : null,
                pessoa != null ? pessoa.getNome() : null,
                a.getDescricao(), a.getDataRealizacao(),
                a.getCreatedAt(), a.getUpdatedAt()
        );
    }
}