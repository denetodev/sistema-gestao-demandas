package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.PecaRequest;
import dev.denetodev.sgd_api.dto.response.PecaResponse;
import dev.denetodev.sgd_api.entity.Demanda;
import dev.denetodev.sgd_api.entity.Peca;
import dev.denetodev.sgd_api.entity.TipoPeca;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.DemandaRepository;
import dev.denetodev.sgd_api.repository.PecaRepository;
import dev.denetodev.sgd_api.repository.TipoPecaRepository;
import dev.denetodev.sgd_api.service.support.Resolvers;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PecaService {

    private final PecaRepository pecaRepository;
    private final DemandaRepository demandaRepository;
    private final TipoPecaRepository tipoPecaRepository;

    public PecaService(PecaRepository pecaRepository, DemandaRepository demandaRepository, TipoPecaRepository tipoPecaRepository) {
        this.pecaRepository = pecaRepository;
        this.demandaRepository = demandaRepository;
        this.tipoPecaRepository = tipoPecaRepository;
    }

    @Transactional(readOnly = true)
    public List<PecaResponse> listarTodas() {
        return pecaRepository.findAll().stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<PecaResponse> listarPorDemanda(UUID demandaId) {
        return pecaRepository.findByDemandaId(demandaId).stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public PecaResponse buscarPorId(UUID id) {
        return paraResponse(buscarEntidade(id));
    }

    public PecaResponse criar(PecaRequest request) {
        Demanda demanda = Resolvers.resolverObrigatorio(request.demandaId(), demandaRepository, "Demanda");
        TipoPeca tipo = Resolvers.resolverObrigatorio(request.tipoPecaId(), tipoPecaRepository, "Tipo de peça");

        Peca peca = new Peca(demanda, tipo, request.nome());
        peca.setDescricao(request.descricao());

        return paraResponse(pecaRepository.save(peca));
    }

    public PecaResponse atualizar(UUID id, PecaRequest request) {
        Peca peca = buscarEntidade(id);
        peca.setDemanda(Resolvers.resolverObrigatorio(request.demandaId(), demandaRepository, "Demanda"));
        peca.setTipoPeca(Resolvers.resolverObrigatorio(request.tipoPecaId(), tipoPecaRepository, "Tipo de peça"));
        peca.setNome(request.nome());
        peca.setDescricao(request.descricao());

        return paraResponse(peca);
    }

    public void remover(UUID id) {
        pecaRepository.delete(buscarEntidade(id));
    }

    private Peca buscarEntidade(UUID id) {
        return pecaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Peça não encontrada: " + id));
    }

    private PecaResponse paraResponse(Peca p) {
        return new PecaResponse(
                p.getId(),
                p.getDemanda().getId(), p.getDemanda().getTitulo(),
                p.getTipoPeca().getId(), p.getTipoPeca().getNome(),
                p.getNome(), p.getDescricao(),
                p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}