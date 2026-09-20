package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.TipoPecaRequest;
import dev.denetodev.sgd_api.dto.response.TipoPecaResponse;
import dev.denetodev.sgd_api.entity.TipoPeca;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.TipoPecaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TipoPecaService {

    private final TipoPecaRepository tipoPecaRepository;

    public TipoPecaService(TipoPecaRepository tipoPecaRepository) {
        this.tipoPecaRepository = tipoPecaRepository;
    }

    @Transactional(readOnly = true)
    public List<TipoPecaResponse> listarTodas() {
        return tipoPecaRepository.findAll().stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public TipoPecaResponse buscarPorId(UUID id) {
        return paraResponse(buscarEntidade(id));
    }

    public TipoPecaResponse criar(TipoPecaRequest request) {
        TipoPeca tipo = new TipoPeca(request.nome());
        tipo.setDescricao(request.descricao());
        return paraResponse(tipoPecaRepository.save(tipo));
    }

    public TipoPecaResponse atualizar(UUID id, TipoPecaRequest request) {
        TipoPeca tipo = buscarEntidade(id);
        tipo.setNome(request.nome());
        tipo.setDescricao(request.descricao());
        return paraResponse(tipo);
    }

    public void desativar(UUID id) {
        buscarEntidade(id).setAtivo(false);
    }

    private TipoPeca buscarEntidade(UUID id) {
        return tipoPecaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Tipo de peça não encontrado: " + id));
    }

    private TipoPecaResponse paraResponse(TipoPeca t) {
        return new TipoPecaResponse(t.getId(), t.getNome(), t.getDescricao(), t.isAtivo(), t.getCreatedAt(), t.getUpdatedAt());
    }
}