package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.DiretoriaRequest;
import dev.denetodev.sgd_api.dto.response.DiretoriaResponse;
import dev.denetodev.sgd_api.entity.Diretoria;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.DiretoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DiretoriaService {

    private final DiretoriaRepository diretoriaRepository;

    public DiretoriaService(DiretoriaRepository diretoriaRepository) {
        this.diretoriaRepository = diretoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<DiretoriaResponse> listarTodas() {
        return diretoriaRepository.findAll().stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public DiretoriaResponse buscarPorId(UUID id) {
        return paraResponse(buscarEntidade(id));
    }

    public DiretoriaResponse criar(DiretoriaRequest request) {
        Diretoria diretoria = new Diretoria(request.nome());
        diretoria.setSigla(request.sigla());
        return paraResponse(diretoriaRepository.save(diretoria));
    }

    public DiretoriaResponse atualizar(UUID id, DiretoriaRequest request) {
        Diretoria diretoria = buscarEntidade(id);
        diretoria.setNome(request.nome());
        diretoria.setSigla(request.sigla());
        return paraResponse(diretoria);
    }

    public void desativar(UUID id) {
        buscarEntidade(id).setAtivo(false);
    }

    private Diretoria buscarEntidade(UUID id) {
        return diretoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Diretoria não encontrada: " + id));
    }

    private DiretoriaResponse paraResponse(Diretoria d) {
        return new DiretoriaResponse(d.getId(), d.getNome(), d.getSigla(), d.isAtivo(), d.getCreatedAt(), d.getUpdatedAt());
    }
}