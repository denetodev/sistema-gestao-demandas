package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.EvidenciaRequest;
import dev.denetodev.sgd_api.dto.response.EvidenciaResponse;
import dev.denetodev.sgd_api.entity.Atividade;
import dev.denetodev.sgd_api.entity.Evidencia;
import dev.denetodev.sgd_api.entity.Peca;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.AtividadeRepository;
import dev.denetodev.sgd_api.repository.EvidenciaRepository;
import dev.denetodev.sgd_api.repository.PecaRepository;
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
public class EvidenciaService {

    private final EvidenciaRepository evidenciaRepository;
    private final AtividadeRepository atividadeRepository;
    private final PecaRepository pecaRepository;
    private final CurrentPessoaResolver currentPessoaResolver;
    private final PermissaoService permissaoService;

    public EvidenciaService(
            EvidenciaRepository evidenciaRepository,
            AtividadeRepository atividadeRepository,
            PecaRepository pecaRepository,
            CurrentPessoaResolver currentPessoaResolver,
            PermissaoService permissaoService
    ) {
        this.evidenciaRepository = evidenciaRepository;
        this.atividadeRepository = atividadeRepository;
        this.pecaRepository = pecaRepository;
        this.currentPessoaResolver = currentPessoaResolver;
        this.permissaoService = permissaoService;
    }

    @Transactional(readOnly = true)
    public List<EvidenciaResponse> listarTodas() {
        return evidenciaRepository.findAll().stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public EvidenciaResponse buscarPorId(UUID id) {
        return paraResponse(buscarEntidade(id));
    }

    public EvidenciaResponse criar(Jwt jwt, EvidenciaRequest request) {
        if (request.atividadeId() == null && request.pecaId() == null) {
            throw new IllegalArgumentException("Evidência precisa estar vinculada a uma atividade ou a uma peça");
        }

        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        Atividade atividade = Resolvers.resolverOuNulo(request.atividadeId(), atividadeRepository, "Atividade");
        Peca peca = Resolvers.resolverOuNulo(request.pecaId(), pecaRepository, "Peça");

        boolean podePelaAtividade = atividade == null || permissaoService.podeGerenciar(usuario, atividade.getPessoa());
        boolean podePelaPeca = peca == null || permissaoService.podeGerenciar(usuario, peca.getPessoa());
        if (!podePelaAtividade || !podePelaPeca) {
            throw new AccessDeniedException("Sem permissão para anexar evidência a este item");
        }

        Evidencia evidencia = new Evidencia(request.tipo());
        evidencia.setAtividade(atividade);
        evidencia.setPeca(peca);
        evidencia.setConteudo(request.conteudo());
        evidencia.setDescricao(request.descricao());
        evidencia.setCreatedBy(usuario.getId());

        return paraResponse(evidenciaRepository.save(evidencia));
    }

    public void remover(Jwt jwt, UUID id) {
        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        Evidencia evidencia = buscarEntidade(id);

        if (!permissaoService.podeGerenciarEvidencia(usuario, evidencia)) {
            throw new AccessDeniedException("Sem permissão para remover esta evidência");
        }

        evidenciaRepository.delete(evidencia);
    }

    private Evidencia buscarEntidade(UUID id) {
        return evidenciaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evidência não encontrada: " + id));
    }

    private EvidenciaResponse paraResponse(Evidencia e) {
        Atividade atividade = e.getAtividade();
        Peca peca = e.getPeca();
        return new EvidenciaResponse(
                e.getId(),
                atividade != null ? atividade.getId() : null,
                peca != null ? peca.getId() : null,
                e.getTipo(), e.getConteudo(), e.getDescricao(),
                e.getCreatedAt()
        );
    }
}