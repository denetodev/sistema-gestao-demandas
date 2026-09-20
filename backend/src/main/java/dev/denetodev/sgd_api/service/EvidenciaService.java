package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.EvidenciaRequest;
import dev.denetodev.sgd_api.dto.response.EvidenciaResponse;
import dev.denetodev.sgd_api.entity.Atividade;
import dev.denetodev.sgd_api.entity.Evidencia;
import dev.denetodev.sgd_api.entity.Peca;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.AtividadeRepository;
import dev.denetodev.sgd_api.repository.EvidenciaRepository;
import dev.denetodev.sgd_api.repository.PecaRepository;
import dev.denetodev.sgd_api.service.support.Resolvers;
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

    public EvidenciaService(EvidenciaRepository evidenciaRepository, AtividadeRepository atividadeRepository, PecaRepository pecaRepository) {
        this.evidenciaRepository = evidenciaRepository;
        this.atividadeRepository = atividadeRepository;
        this.pecaRepository = pecaRepository;
    }

    @Transactional(readOnly = true)
    public List<EvidenciaResponse> listarTodas() {
        return evidenciaRepository.findAll().stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public EvidenciaResponse buscarPorId(UUID id) {
        return paraResponse(buscarEntidade(id));
    }

    public EvidenciaResponse criar(EvidenciaRequest request) {
        if (request.atividadeId() == null && request.pecaId() == null) {
            throw new IllegalArgumentException("Evidência precisa estar vinculada a uma atividade ou a uma peça");
        }

        Atividade atividade = Resolvers.resolverOuNulo(request.atividadeId(), atividadeRepository, "Atividade");
        Peca peca = Resolvers.resolverOuNulo(request.pecaId(), pecaRepository, "Peça");

        Evidencia evidencia = new Evidencia(request.tipo());
        evidencia.setAtividade(atividade);
        evidencia.setPeca(peca);
        evidencia.setConteudo(request.conteudo());
        evidencia.setDescricao(request.descricao());

        return paraResponse(evidenciaRepository.save(evidencia));
    }

    public void remover(UUID id) {
        evidenciaRepository.delete(buscarEntidade(id));
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