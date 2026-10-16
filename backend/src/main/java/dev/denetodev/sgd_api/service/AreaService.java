package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.AreaRequest;
import dev.denetodev.sgd_api.dto.response.AreaResponse;
import dev.denetodev.sgd_api.entity.Area;
import dev.denetodev.sgd_api.entity.Diretoria;
import dev.denetodev.sgd_api.entity.Nucleo;
import dev.denetodev.sgd_api.repository.NucleoRepository;
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
    private final NucleoRepository nucleoRepository;

    public AreaService(AreaRepository areaRepository, DiretoriaRepository diretoriaRepository,
                       NucleoRepository nucleoRepository) {
        this.nucleoRepository = nucleoRepository;
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
        Nucleo nucleo = buscarNucleo(request);
        Area area = new Area(diretoriaDe(request, nucleo), request.nome());
        area.setNucleo(nucleo);
        return paraResponse(areaRepository.save(area));
    }

    public AreaResponse atualizar(UUID id, AreaRequest request) {
        Area area = buscarEntidade(id);
        Nucleo nucleo = buscarNucleo(request);
        area.setNome(request.nome());
        area.setDiretoria(diretoriaDe(request, nucleo));
        area.setNucleo(nucleo);
        return paraResponse(area);
    }

    /** Liga ou desliga o acesso da equipe inteira ao módulo de equipamentos. */
    public AreaResponse definirAcessoEquipamentos(UUID id, boolean acessa) {
        Area area = buscarEntidade(id);
        area.setAcessaEquipamentos(acessa);
        return paraResponse(area);
    }

    public void desativar(UUID id) {
        buscarEntidade(id).setAtivo(false);
    }

    private Nucleo buscarNucleo(AreaRequest request) {
        if (request.nucleoId() == null) {
            return null;
        }
        return nucleoRepository.findById(request.nucleoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Núcleo não encontrado: " + request.nucleoId()));
    }

    /** A equipe fica na diretoria do núcleo; diretoria diferente da do núcleo é pedido inválido. */
    private Diretoria diretoriaDe(AreaRequest request, Nucleo nucleo) {
        Diretoria diretoria = diretoriaRepository.findById(request.diretoriaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Diretoria não encontrada: " + request.diretoriaId()));
        if (nucleo != null && !nucleo.getDiretoria().getId().equals(diretoria.getId())) {
            throw new IllegalArgumentException("O núcleo escolhido não pertence a essa diretoria");
        }
        return diretoria;
    }

    private Area buscarEntidade(UUID id) {
        return areaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Area não encontrada: " + id));
    }

    private AreaResponse paraResponse(Area a) {
        return new AreaResponse(
                a.getId(), a.getNome(),
                a.getDiretoria().getId(), a.getDiretoria().getNome(),
                a.getNucleo() != null ? a.getNucleo().getId() : null,
                a.getNucleo() != null ? a.getNucleo().getNome() : null,
                a.isAtivo(), a.isAcessaEquipamentos(), a.getCreatedAt(), a.getUpdatedAt()
        );
    }
}