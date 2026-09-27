package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.AtividadeRequest;
import dev.denetodev.sgd_api.dto.response.AtividadeResponse;
import dev.denetodev.sgd_api.entity.Atividade;
import dev.denetodev.sgd_api.entity.Demanda;
import dev.denetodev.sgd_api.entity.EscopoListagem;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.entity.TipoAtividade;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.AtividadeRepository;
import dev.denetodev.sgd_api.repository.DemandaRepository;
import dev.denetodev.sgd_api.repository.PessoaRepository;
import dev.denetodev.sgd_api.repository.TipoAtividadeRepository;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import dev.denetodev.sgd_api.service.support.PermissaoService;
import dev.denetodev.sgd_api.service.support.Resolvers;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
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
    private final CurrentPessoaResolver currentPessoaResolver;
    private final PermissaoService permissaoService;

    public AtividadeService(
            AtividadeRepository atividadeRepository,
            DemandaRepository demandaRepository,
            TipoAtividadeRepository tipoAtividadeRepository,
            PessoaRepository pessoaRepository,
            CurrentPessoaResolver currentPessoaResolver,
            PermissaoService permissaoService
    ) {
        this.atividadeRepository = atividadeRepository;
        this.demandaRepository = demandaRepository;
        this.tipoAtividadeRepository = tipoAtividadeRepository;
        this.pessoaRepository = pessoaRepository;
        this.currentPessoaResolver = currentPessoaResolver;
        this.permissaoService = permissaoService;
    }

    @Transactional(readOnly = true)
    public Page<AtividadeResponse> listarComEscopo(Jwt jwt, EscopoListagem escopo, Pageable pageable) {
        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        permissaoService.validarEscopo(usuario, escopo);

        Page<Atividade> pagina = switch (escopo) {
            case MINHAS -> atividadeRepository.findByPessoa_Id(usuario.getId(), pageable);
            case EQUIPE -> atividadeRepository.findByPessoa_AreaId(usuario.getReferenciaArea().getId(), pageable);
            case DIRETORIA -> atividadeRepository.findByPessoaAreaDiretoriaId(usuario.getArea().getDiretoria().getId(), pageable);
            case TODAS -> atividadeRepository.findAll(pageable);
        };

        return pagina.map(this::paraResponse);
    }

    @Transactional(readOnly = true)
    public List<AtividadeResponse> listarPorDemanda(UUID demandaId) {
        return atividadeRepository.findByDemandaId(demandaId).stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public AtividadeResponse buscarPorId(UUID id) {
        return paraResponse(buscarEntidade(id));
    }

    public AtividadeResponse criar(Jwt jwt, AtividadeRequest request) {
        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        Pessoa pessoaAlvo = Resolvers.resolverOuNulo(request.pessoaId(), pessoaRepository, "Pessoa");

        if (!permissaoService.podeGerenciar(usuario, pessoaAlvo)) {
            throw new AccessDeniedException("Sem permissão para lançar atividade em nome dessa pessoa");
        }

        TipoAtividade tipo = Resolvers.resolverObrigatorio(request.tipoAtividadeId(), tipoAtividadeRepository, "Tipo de atividade");

        Atividade atividade = new Atividade(tipo);
        atividade.setDemanda(Resolvers.resolverOuNulo(request.demandaId(), demandaRepository, "Demanda"));
        atividade.setPessoa(pessoaAlvo);
        atividade.setDescricao(request.descricao());
        if (request.dataRealizacao() != null) {
            atividade.setDataRealizacao(request.dataRealizacao());
        }
        atividade.setCreatedBy(usuario.getId());

        return paraResponse(atividadeRepository.save(atividade));
    }

    public AtividadeResponse atualizar(Jwt jwt, UUID id, AtividadeRequest request) {
        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        Atividade atividade = buscarEntidade(id);

        if (!permissaoService.podeGerenciar(usuario, atividade.getPessoa())) {
            throw new AccessDeniedException("Sem permissão para editar esta atividade");
        }

        Pessoa novaPessoa = Resolvers.resolverOuNulo(request.pessoaId(), pessoaRepository, "Pessoa");
        if (!permissaoService.podeGerenciar(usuario, novaPessoa)) {
            throw new AccessDeniedException("Sem permissão para reatribuir esta atividade a essa pessoa");
        }

        TipoAtividade tipo = Resolvers.resolverObrigatorio(request.tipoAtividadeId(), tipoAtividadeRepository, "Tipo de atividade");

        atividade.setTipoAtividade(tipo);
        atividade.setDemanda(Resolvers.resolverOuNulo(request.demandaId(), demandaRepository, "Demanda"));
        atividade.setPessoa(novaPessoa);
        atividade.setDescricao(request.descricao());
        if (request.dataRealizacao() != null) {
            atividade.setDataRealizacao(request.dataRealizacao());
        }
        atividade.setUpdatedBy(usuario.getId());

        return paraResponse(atividade);
    }

    public void remover(Jwt jwt, UUID id) {
        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        Atividade atividade = buscarEntidade(id);

        if (!permissaoService.podeGerenciar(usuario, atividade.getPessoa())) {
            throw new AccessDeniedException("Sem permissão para remover esta atividade");
        }

        atividadeRepository.delete(atividade);
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