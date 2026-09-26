package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.PecaRequest;
import dev.denetodev.sgd_api.dto.response.PecaResponse;
import dev.denetodev.sgd_api.entity.Demanda;
import dev.denetodev.sgd_api.entity.Peca;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.entity.TipoPeca;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.DemandaRepository;
import dev.denetodev.sgd_api.repository.PecaRepository;
import dev.denetodev.sgd_api.repository.PessoaRepository;
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
    private final PessoaRepository pessoaRepository;

    public PecaService(
            PecaRepository pecaRepository,
            DemandaRepository demandaRepository,
            TipoPecaRepository tipoPecaRepository,
            PessoaRepository pessoaRepository
    ) {
        this.pecaRepository = pecaRepository;
        this.demandaRepository = demandaRepository;
        this.tipoPecaRepository = tipoPecaRepository;
        this.pessoaRepository = pessoaRepository;
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

        // o construtor já copia valorUnitario de tipo.getValorReferencia() —
        // não seta isso aqui de propósito, nunca vem do request
        Peca peca = new Peca(demanda, tipo, request.nome());
        peca.setDescricao(request.descricao());
        if (request.quantidade() != null) {
            peca.setQuantidade(request.quantidade());
        }
        peca.setPessoa(Resolvers.resolverOuNulo(request.pessoaId(), pessoaRepository, "Pessoa"));
        peca.setDataEntrega(request.dataEntrega());

        return paraResponse(pecaRepository.save(peca));
    }

    public PecaResponse atualizar(UUID id, PecaRequest request) {
        Peca peca = buscarEntidade(id);
        Demanda demanda = Resolvers.resolverObrigatorio(request.demandaId(), demandaRepository, "Demanda");
        TipoPeca tipo = Resolvers.resolverObrigatorio(request.tipoPecaId(), tipoPecaRepository, "Tipo de peça");

        peca.setDemanda(demanda);
        peca.setTipoPeca(tipo);
        peca.setNome(request.nome());
        peca.setDescricao(request.descricao());
        if (request.quantidade() != null) {
            peca.setQuantidade(request.quantidade());
        }
        peca.setPessoa(Resolvers.resolverOuNulo(request.pessoaId(), pessoaRepository, "Pessoa"));
        peca.setDataEntrega(request.dataEntrega());
        // valorUnitario NUNCA é tocado aqui, mesmo trocando o tipo — fica
        // congelado no valor do momento da criação, de propósito.

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
        Pessoa pessoa = p.getPessoa();
        return new PecaResponse(
                p.getId(),
                p.getDemanda().getId(), p.getDemanda().getTitulo(),
                p.getTipoPeca().getId(), p.getTipoPeca().getNome(),
                p.getNome(), p.getDescricao(),
                p.getQuantidade(), p.getValorUnitario(), p.getValorTotal(),
                pessoa != null ? pessoa.getId() : null,
                pessoa != null ? pessoa.getNome() : null,
                p.getDataEntrega(),
                p.getCreatedAt(), p.getUpdatedAt()
        );
    }
}