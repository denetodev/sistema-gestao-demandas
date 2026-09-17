package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.AreaRequest;
import dev.denetodev.sgd_api.dto.response.AreaResponse;
import dev.denetodev.sgd_api.entity.Area;
import dev.denetodev.sgd_api.entity.Diretoria;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.AreaRepository;
import dev.denetodev.sgd_api.repository.DiretoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class AreaService {

    private final AreaRepository areaRepository;
    private final DiretoriaRepository diretoriaRepository;

    public AreaService(AreaRepository areaRepository, DiretoriaRepository diretoriaRepository) {
        this.areaRepository = areaRepository;
        this.diretoriaRepository = diretoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<AreaResponse> listarTodas() {
        return areaRepository.findAll().stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public AreaResponse buscarPorId(UUID id) {
        return paraResponse(buscarEntidade(id));
    }

    public AreaResponse criar(AreaRequest request) {
        Diretoria diretoria = diretoriaRepository.findById(request.diretoriaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Diretoria não encontrada: " + request.diretoriaId()));
        Area area = new Area(diretoria, request.nome());
        return paraResponse(areaRepository.save(area));
    }

    public AreaResponse atualizar(UUID id, AreaRequest request) {
        Area area = buscarEntidade(id);
        Diretoria diretoria = diretoriaRepository.findById(request.diretoriaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Diretoria não encontrada: " + request.diretoriaId()));
        area.setNome(request.nome());
        area.setDiretoria(diretoria);
        return paraResponse(area);
    }

    public void desativar(UUID id) {
        buscarEntidade(id).setAtivo(false);
    }

    private Area buscarEntidade(UUID id) {
        return areaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Area não encontrada: " + id));
    }

    private AreaResponse paraResponse(Area a) {
        return new AreaResponse(
                a.getId(), a.getNome(),
                a.getDiretoria().getId(), a.getDiretoria().getNome(),
                a.isAtivo(), a.getCreatedAt(), a.getUpdatedAt()
        );
    }
}