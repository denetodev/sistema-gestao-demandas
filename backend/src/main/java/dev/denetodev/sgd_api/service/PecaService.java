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
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import dev.denetodev.sgd_api.service.support.PermissaoService;
import dev.denetodev.sgd_api.service.support.Resolvers;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
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
    private final CurrentPessoaResolver currentPessoaResolver;
    private final PermissaoService permissaoService;

    public PecaService(
            PecaRepository pecaRepository,
            DemandaRepository demandaRepository,
            TipoPecaRepository tipoPecaRepository,
            PessoaRepository pessoaRepository,
            CurrentPessoaResolver currentPessoaResolver,
            PermissaoService permissaoService
    ) {
        this.pecaRepository = pecaRepository;
        this.demandaRepository = demandaRepository;
        this.tipoPecaRepository = tipoPecaRepository;
        this.pessoaRepository = pessoaRepository;
        this.currentPessoaResolver = currentPessoaResolver;
        this.permissaoService = permissaoService;
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

    public PecaResponse criar(Jwt jwt, PecaRequest request) {
        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        Pessoa pessoaAlvo = Resolvers.resolverOuNulo(request.pessoaId(), pessoaRepository, "Pessoa");

        if (!permissaoService.podeGerenciar(usuario, pessoaAlvo)) {
            throw new AccessDeniedException("Sem permissão para lançar peça em nome dessa pessoa");
        }

        Demanda demanda = Resolvers.resolverObrigatorio(request.demandaId(), demandaRepository, "Demanda");
        TipoPeca tipo = Resolvers.resolverObrigatorio(request.tipoPecaId(), tipoPecaRepository, "Tipo de peça");

        Peca peca = new Peca(demanda, tipo, request.nome());
        peca.setDescricao(request.descricao());
        if (request.quantidade() != null) {
            peca.setQuantidade(request.quantidade());
        }
        peca.setPessoa(pessoaAlvo);
        peca.setDataEntrega(request.dataEntrega());
        peca.setCreatedBy(usuario.getId());

        return paraResponse(pecaRepository.save(peca));
    }

    public PecaResponse atualizar(Jwt jwt, UUID id, PecaRequest request) {
        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        Peca peca = buscarEntidade(id);

        if (!permissaoService.podeGerenciar(usuario, peca.getPessoa())) {
            throw new AccessDeniedException("Sem permissão para editar esta peça");
        }

        Pessoa novaPessoa = Resolvers.resolverOuNulo(request.pessoaId(), pessoaRepository, "Pessoa");
        if (!permissaoService.podeGerenciar(usuario, novaPessoa)) {
            throw new AccessDeniedException("Sem permissão para reatribuir esta peça a essa pessoa");
        }

        Demanda demanda = Resolvers.resolverObrigatorio(request.demandaId(), demandaRepository, "Demanda");
        TipoPeca tipo = Resolvers.resolverObrigatorio(request.tipoPecaId(), tipoPecaRepository, "Tipo de peça");

        peca.setDemanda(demanda);
        peca.setTipoPeca(tipo);
        peca.setNome(request.nome());
        peca.setDescricao(request.descricao());
        if (request.quantidade() != null) {
            peca.setQuantidade(request.quantidade());
        }
        peca.setPessoa(novaPessoa);
        peca.setDataEntrega(request.dataEntrega());
        peca.setUpdatedBy(usuario.getId());
        // valorUnitario continua intocado — decisão do step 3

        return paraResponse(peca);
    }

    public void remover(Jwt jwt, UUID id) {
        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        Peca peca = buscarEntidade(id);

        if (!permissaoService.podeGerenciar(usuario, peca.getPessoa())) {
            throw new AccessDeniedException("Sem permissão para remover esta peça");
        }

        pecaRepository.delete(peca);
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