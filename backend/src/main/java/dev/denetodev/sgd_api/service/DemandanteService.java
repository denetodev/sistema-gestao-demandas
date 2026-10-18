package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.DemandanteRequest;
import dev.denetodev.sgd_api.dto.response.DemandanteResponse;
import dev.denetodev.sgd_api.entity.Demandante;
import dev.denetodev.sgd_api.entity.Diretoria;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.DemandanteRepository;
import dev.denetodev.sgd_api.repository.DiretoriaRepository;
import dev.denetodev.sgd_api.service.support.Resolvers;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import dev.denetodev.sgd_api.service.support.PermissaoService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DemandanteService {

    private final DemandanteRepository demandanteRepository;
    private final DiretoriaRepository diretoriaRepository;
    private final CurrentPessoaResolver currentPessoaResolver;
    private final PermissaoService permissaoService;

    public DemandanteService(DemandanteRepository demandanteRepository, DiretoriaRepository diretoriaRepository,
                             CurrentPessoaResolver currentPessoaResolver, PermissaoService permissaoService) {
        this.currentPessoaResolver = currentPessoaResolver;
        this.permissaoService = permissaoService;
        this.demandanteRepository = demandanteRepository;
        this.diretoriaRepository = diretoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<DemandanteResponse> listarTodas() {
        return demandanteRepository.findAll().stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public DemandanteResponse buscarPorId(UUID id) {
        return paraResponse(buscarEntidade(id));
    }

    public DemandanteResponse criar(Jwt jwt, DemandanteRequest request) {
        permissaoService.exigirGerenciarDemandantes(currentPessoaResolver.resolver(jwt));
        Demandante demandante = new Demandante(request.nome(), request.tipo());
        demandante.setObservacao(request.observacao());
        demandante.setDiretoria(Resolvers.resolverOuNulo(request.diretoriaId(), diretoriaRepository, "Diretoria"));
        return paraResponse(demandanteRepository.save(demandante));
    }

    public DemandanteResponse atualizar(Jwt jwt, UUID id, DemandanteRequest request) {
        permissaoService.exigirGerenciarDemandantes(currentPessoaResolver.resolver(jwt));
        Demandante demandante = buscarEntidade(id);
        demandante.setNome(request.nome());
        demandante.setTipo(request.tipo());
        demandante.setObservacao(request.observacao());
        demandante.setDiretoria(Resolvers.resolverOuNulo(request.diretoriaId(), diretoriaRepository, "Diretoria"));
        return paraResponse(demandante);
    }

    public void desativar(UUID id) {
        buscarEntidade(id).setAtivo(false);
    }

    private Demandante buscarEntidade(UUID id) {
        return demandanteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Demandante não encontrado: " + id));
    }

    private DemandanteResponse paraResponse(Demandante d) {
        Diretoria diretoria = d.getDiretoria();
        return new DemandanteResponse(
                d.getId(), d.getNome(), d.getTipo(), d.getObservacao(),
                diretoria != null ? diretoria.getId() : null,
                diretoria != null ? diretoria.getNome() : null,
                d.isAtivo(), d.getCreatedAt(), d.getUpdatedAt()
        );
    }
}