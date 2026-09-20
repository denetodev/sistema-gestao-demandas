package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.TipoAtividadeRequest;
import dev.denetodev.sgd_api.dto.response.TipoAtividadeResponse;
import dev.denetodev.sgd_api.entity.TipoAtividade;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.TipoAtividadeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TipoAtividadeService {

    private final TipoAtividadeRepository tipoAtividadeRepository;

    public TipoAtividadeService(TipoAtividadeRepository tipoAtividadeRepository) {
        this.tipoAtividadeRepository = tipoAtividadeRepository;
    }

    @Transactional(readOnly = true)
    public List<TipoAtividadeResponse> listarTodas() {
        return tipoAtividadeRepository.findAll().stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public TipoAtividadeResponse buscarPorId(UUID id) {
        return paraResponse(buscarEntidade(id));
    }

    public TipoAtividadeResponse criar(TipoAtividadeRequest request) {
        TipoAtividade tipo = new TipoAtividade(request.nome());
        tipo.setDescricao(request.descricao());
        return paraResponse(tipoAtividadeRepository.save(tipo));
    }

    public TipoAtividadeResponse atualizar(UUID id, TipoAtividadeRequest request) {
        TipoAtividade tipo = buscarEntidade(id);
        tipo.setNome(request.nome());
        tipo.setDescricao(request.descricao());
        return paraResponse(tipo);
    }

    public void desativar(UUID id) {
        buscarEntidade(id).setAtivo(false);
    }

    private TipoAtividade buscarEntidade(UUID id) {
        return tipoAtividadeRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de atividade não encontrado: " + id));
    }

    private TipoAtividadeResponse paraResponse(TipoAtividade t) {
        return new TipoAtividadeResponse(t.getId(), t.getNome(), t.getDescricao(), t.isAtivo(), t.getCreatedAt(), t.getUpdatedAt());
    }
}