package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.TipoPecaRequest;
import dev.denetodev.sgd_api.dto.response.TipoPecaResponse;
import dev.denetodev.sgd_api.entity.Area;
import dev.denetodev.sgd_api.entity.TipoPeca;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.AreaRepository;
import dev.denetodev.sgd_api.repository.TipoPecaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TipoPecaService {

    private final TipoPecaRepository tipoPecaRepository;
    private final AreaRepository areaRepository;

    public TipoPecaService(TipoPecaRepository tipoPecaRepository, AreaRepository areaRepository) {
        this.tipoPecaRepository = tipoPecaRepository;
        this.areaRepository = areaRepository;
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
        Area area = areaRepository.findById(request.areaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Area não encontrada: " + request.areaId()));

        TipoPeca tipo = new TipoPeca(request.nome(), area, request.valorReferencia());
        tipo.setDescricao(request.descricao());
        return paraResponse(tipoPecaRepository.save(tipo));
    }

    public TipoPecaResponse atualizar(UUID id, TipoPecaRequest request) {
        TipoPeca tipo = buscarEntidade(id);
        Area area = areaRepository.findById(request.areaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Area não encontrada: " + request.areaId()));

        tipo.setNome(request.nome());
        tipo.setDescricao(request.descricao());
        tipo.setArea(area);
        tipo.setValorReferencia(request.valorReferencia());
        // atualizar aqui muda a referência de mercado DAQUI PRA FRENTE —
        // Peças já lançadas mantêm o valorUnitario congelado no momento
        // em que nasceram, mesmo que esse valor mude depois.
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
        return new TipoPecaResponse(
                t.getId(), t.getNome(), t.getDescricao(),
                t.getArea().getId(), t.getArea().getNome(),
                t.getValorReferencia(),
                t.isAtivo(), t.getCreatedAt(), t.getUpdatedAt()
        );
    }
}