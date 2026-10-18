package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.NucleoRequest;
import dev.denetodev.sgd_api.dto.response.NucleoResponse;
import dev.denetodev.sgd_api.entity.Diretoria;
import dev.denetodev.sgd_api.entity.Nucleo;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.exception.EstadoInvalidoException;
import dev.denetodev.sgd_api.repository.AreaRepository;
import dev.denetodev.sgd_api.repository.DiretoriaRepository;
import dev.denetodev.sgd_api.repository.NucleoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class NucleoService {

    private final NucleoRepository nucleoRepository;
    private final DiretoriaRepository diretoriaRepository;
    private final AreaRepository areaRepository;

    public NucleoService(NucleoRepository nucleoRepository, DiretoriaRepository diretoriaRepository,
                         AreaRepository areaRepository) {
        this.nucleoRepository = nucleoRepository;
        this.areaRepository = areaRepository;
        this.diretoriaRepository = diretoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<NucleoResponse> listarTodos() {
        return nucleoRepository.findAll().stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public NucleoResponse buscarPorId(UUID id) {
        return paraResponse(buscarEntidade(id));
    }

    public NucleoResponse criar(NucleoRequest request) {
        String nome = request.nome().trim();
        if (nucleoRepository.existsByDiretoriaIdAndNomeIgnoreCaseAndAtivoTrue(request.diretoriaId(), nome)) {
            throw new EstadoInvalidoException("Já existe um núcleo com esse nome nessa diretoria");
        }
        return paraResponse(nucleoRepository.save(new Nucleo(buscarDiretoria(request.diretoriaId()), nome)));
    }

    public NucleoResponse atualizar(UUID id, NucleoRequest request) {
        Nucleo nucleo = buscarEntidade(id);
        String nome = request.nome().trim();
        if (nucleoRepository.existsByDiretoriaIdAndNomeIgnoreCaseAndAtivoTrueAndIdNot(request.diretoriaId(), nome, id)) {
            throw new EstadoInvalidoException("Já existe um núcleo com esse nome nessa diretoria");
        }
        boolean mudouDeDiretoria = !nucleo.getDiretoria().getId().equals(request.diretoriaId());
        if (mudouDeDiretoria && areaRepository.existsByNucleoIdAndAtivoTrue(id)) {
            throw new EstadoInvalidoException("Mude as equipes para outro núcleo antes de mover este núcleo de diretoria");
        }
        nucleo.setNome(nome);
        nucleo.setDiretoria(buscarDiretoria(request.diretoriaId()));
        return paraResponse(nucleo);
    }

    public void desativar(UUID id) {
        if (areaRepository.existsByNucleoIdAndAtivoTrue(id)) {
            throw new EstadoInvalidoException("Este núcleo ainda tem equipes. Mude ou desative as equipes antes");
        }
        buscarEntidade(id).setAtivo(false);
    }

    private Diretoria buscarDiretoria(UUID id) {
        return diretoriaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Diretoria não encontrada: " + id));
    }

    private Nucleo buscarEntidade(UUID id) {
        return nucleoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Núcleo não encontrado: " + id));
    }

    private NucleoResponse paraResponse(Nucleo n) {
        return new NucleoResponse(n.getId(), n.getNome(), n.getDiretoria().getId(), n.getDiretoria().getNome(),
                n.isAtivo(), n.getCreatedAt(), n.getUpdatedAt());
    }
}
