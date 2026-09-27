package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.TipoPecaRequest;
import dev.denetodev.sgd_api.dto.response.TipoPecaResponse;
import dev.denetodev.sgd_api.entity.Area;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.entity.TipoPeca;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.AreaRepository;
import dev.denetodev.sgd_api.repository.TipoPecaRepository;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import dev.denetodev.sgd_api.service.support.PermissaoService;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TipoPecaService {

    private final TipoPecaRepository tipoPecaRepository;
    private final AreaRepository areaRepository;
    private final CurrentPessoaResolver currentPessoaResolver;
    private final PermissaoService permissaoService;

    public TipoPecaService(
            TipoPecaRepository tipoPecaRepository,
            AreaRepository areaRepository,
            CurrentPessoaResolver currentPessoaResolver,
            PermissaoService permissaoService
    ) {
        this.tipoPecaRepository = tipoPecaRepository;
        this.areaRepository = areaRepository;
        this.currentPessoaResolver = currentPessoaResolver;
        this.permissaoService = permissaoService;
    }

    @Transactional(readOnly = true)
    public List<TipoPecaResponse> listarTodas() {
        return tipoPecaRepository.findAll().stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public TipoPecaResponse buscarPorId(UUID id) {
        return paraResponse(buscarEntidade(id));
    }

    public TipoPecaResponse criar(Jwt jwt, TipoPecaRequest request) {
        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        Area area = areaRepository.findById(request.areaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Area não encontrada: " + request.areaId()));

        if (!permissaoService.podeGerenciarTipoPeca(usuario, area)) {
            throw new AccessDeniedException("Sem permissão para criar tipo de peça nessa área");
        }

        TipoPeca tipo = new TipoPeca(request.nome(), area, request.valorReferencia());
        tipo.setDescricao(request.descricao());
        return paraResponse(tipoPecaRepository.save(tipo));
    }

    public TipoPecaResponse atualizar(Jwt jwt, UUID id, TipoPecaRequest request) {
        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        TipoPeca tipo = buscarEntidade(id);

        if (!permissaoService.podeGerenciarTipoPeca(usuario, tipo.getArea())) {
            throw new AccessDeniedException("Sem permissão para editar este tipo de peça");
        }

        Area novaArea = areaRepository.findById(request.areaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Area não encontrada: " + request.areaId()));

        if (!permissaoService.podeGerenciarTipoPeca(usuario, novaArea)) {
            throw new AccessDeniedException("Sem permissão para mover este tipo de peça para essa área");
        }

        tipo.setNome(request.nome());
        tipo.setDescricao(request.descricao());
        tipo.setArea(novaArea);
        tipo.setValorReferencia(request.valorReferencia());
        return paraResponse(tipo);
    }

    public void desativar(Jwt jwt, UUID id) {
        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        TipoPeca tipo = buscarEntidade(id);

        if (!permissaoService.podeGerenciarTipoPeca(usuario, tipo.getArea())) {
            throw new AccessDeniedException("Sem permissão para desativar este tipo de peça");
        }

        tipo.setAtivo(false);
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