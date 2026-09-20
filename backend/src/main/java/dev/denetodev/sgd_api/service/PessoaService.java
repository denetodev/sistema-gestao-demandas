package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.PessoaRequest;
import dev.denetodev.sgd_api.dto.response.PessoaResponse;
import dev.denetodev.sgd_api.entity.Area;
import dev.denetodev.sgd_api.entity.Cargo;
import dev.denetodev.sgd_api.entity.Diretoria;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.entity.StatusPessoa;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.AreaRepository;
import dev.denetodev.sgd_api.repository.CargoRepository;
import dev.denetodev.sgd_api.repository.DiretoriaRepository;
import dev.denetodev.sgd_api.repository.PessoaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PessoaService {

    private final PessoaRepository pessoaRepository;
    private final DiretoriaRepository diretoriaRepository;
    private final AreaRepository areaRepository;
    private final CargoRepository cargoRepository;

    public PessoaService(
            PessoaRepository pessoaRepository,
            DiretoriaRepository diretoriaRepository,
            AreaRepository areaRepository,
            CargoRepository cargoRepository
    ) {
        this.pessoaRepository = pessoaRepository;
        this.diretoriaRepository = diretoriaRepository;
        this.areaRepository = areaRepository;
        this.cargoRepository = cargoRepository;
    }

    @Transactional(readOnly = true)
    public List<PessoaResponse> listarTodas() {
        return pessoaRepository.findAll().stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public PessoaResponse buscarPorId(UUID id) {
        return paraResponse(buscarEntidade(id));
    }

    public PessoaResponse criar(PessoaRequest request) {
        Diretoria diretoria = diretoriaRepository.findById(request.diretoriaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Diretoria não encontrada: " + request.diretoriaId()));
        Area area = areaRepository.findById(request.areaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Area não encontrada: " + request.areaId()));

        Pessoa pessoa = new Pessoa(request.nome(), diretoria, area);
        pessoa.setEmail(request.email());
        pessoa.setCargo(resolverCargo(request.cargoId()));
        if (request.status() != null) {
            pessoa.setStatus(request.status());
        } if (request.perfil() != null) {
            pessoa.setPerfil(request.perfil());
        }

        return paraResponse(pessoaRepository.save(pessoa));
    }

    public PessoaResponse atualizar(UUID id, PessoaRequest request) {
        Pessoa pessoa = buscarEntidade(id);

        Diretoria diretoria = diretoriaRepository.findById(request.diretoriaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Diretoria não encontrada: " + request.diretoriaId()));
        Area area = areaRepository.findById(request.areaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Area não encontrada: " + request.areaId()));

        pessoa.setNome(request.nome());
        pessoa.setEmail(request.email());
        pessoa.setDiretoria(diretoria);
        pessoa.setArea(area);
        pessoa.setCargo(resolverCargo(request.cargoId()));
        if (request.status() != null) {
            pessoa.setStatus(request.status());
        } if (request.perfil() != null) {
            pessoa.setPerfil(request.perfil());
        }

        return paraResponse(pessoa);
    }

    public void desativar(UUID id) {
        buscarEntidade(id).setStatus(StatusPessoa.INATIVO);
    }

    private Cargo resolverCargo(UUID cargoId) {
        if (cargoId == null) {
            return null;
        }
        return cargoRepository.findById(cargoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cargo não encontrado: " + cargoId));
    }

    private Pessoa buscarEntidade(UUID id) {
        return pessoaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa não encontrada: " + id));
    }

    private PessoaResponse paraResponse(Pessoa pessoa) {
        Cargo cargo = pessoa.getCargo();
        return new PessoaResponse(
                pessoa.getId(), pessoa.getNome(), pessoa.getEmail(),
                pessoa.getDiretoria().getId(), pessoa.getDiretoria().getNome(),
                pessoa.getArea().getId(), pessoa.getArea().getNome(),
                cargo != null ? cargo.getId() : null,
                cargo != null ? cargo.getNome() : null,
                pessoa.getStatus(), pessoa.getPerfil(),
                pessoa.getAuthUserId(),
                pessoa.getCreatedAt(), pessoa.getUpdatedAt()
        );
    }

    public PessoaResponse vincularAuth(UUID id, UUID authUserId) {
        Pessoa pessoa = buscarEntidade(id);
        pessoa.setAuthUserId(authUserId);
        return paraResponse(pessoa);
    }
}