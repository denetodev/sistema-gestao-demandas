package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.AprovarRequest;
import dev.denetodev.sgd_api.dto.request.AutoCadastroRequest;
import dev.denetodev.sgd_api.dto.request.PessoaRequest;
import dev.denetodev.sgd_api.dto.response.MeResponse;
import dev.denetodev.sgd_api.dto.response.PessoaResponse;
import dev.denetodev.sgd_api.entity.*;
import dev.denetodev.sgd_api.exception.EstadoInvalidoException;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.AreaRepository;
import dev.denetodev.sgd_api.repository.AuditoriaRepository;
import dev.denetodev.sgd_api.repository.CargoRepository;
import dev.denetodev.sgd_api.repository.PessoaRepository;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PessoaService {

    private final PessoaRepository pessoaRepository;
    private final AreaRepository areaRepository;
    private final CargoRepository cargoRepository;
    private final AuditoriaRepository auditoriaRepository;
    private final CurrentPessoaResolver currentPessoaResolver;

    public PessoaService(
            PessoaRepository pessoaRepository,
            AreaRepository areaRepository,
            CargoRepository cargoRepository,
            AuditoriaRepository auditoriaRepository,
            CurrentPessoaResolver currentPessoaResolver
    ) {
        this.pessoaRepository = pessoaRepository;
        this.areaRepository = areaRepository;
        this.cargoRepository = cargoRepository;
        this.auditoriaRepository = auditoriaRepository;
        this.currentPessoaResolver = currentPessoaResolver;
    }

    @Transactional(readOnly = true)
    public List<PessoaResponse> listarTodas() {
        return pessoaRepository.findAll().stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public PessoaResponse buscarPorId(UUID id) {
        return paraResponse(buscarEntidade(id));
    }

    public PessoaResponse criar(Jwt jwt, PessoaRequest request) {
        Pessoa admin = currentPessoaResolver.resolver(jwt);

        Area area = areaRepository.findById(request.areaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Area não encontrada: " + request.areaId()));

        Pessoa pessoa = new Pessoa(request.nome(), area);
        pessoa.setEmail(request.email());
        pessoa.setCargo(resolverCargo(request.cargoId()));
        if (request.status() != null) {
            pessoa.setStatus(request.status());
        }
        if (request.perfil() != null) {
            pessoa.setPerfil(request.perfil());
        }
        // criação direta por ADMIN já conta como aprovação
        pessoa.setAprovadoEm(OffsetDateTime.now());
        pessoa.setAprovadoPor(admin);

        return paraResponse(pessoaRepository.save(pessoa));
    }

    public PessoaResponse atualizar(UUID id, PessoaRequest request) {
        Pessoa pessoa = buscarEntidade(id);

        Area area = areaRepository.findById(request.areaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Area não encontrada: " + request.areaId()));

        pessoa.setNome(request.nome());
        pessoa.setEmail(request.email());
        pessoa.setArea(area);
        pessoa.setCargo(resolverCargo(request.cargoId()));
        if (request.status() != null) {
            pessoa.setStatus(request.status());
        }
        if (request.perfil() != null) {
            pessoa.setPerfil(request.perfil());
        }

        return paraResponse(pessoa);
    }

    public void desativar(UUID id) {
        buscarEntidade(id).setStatus(StatusPessoa.INATIVO);
    }

    public PessoaResponse vincularAuth(UUID id, UUID authUserId) {
        Pessoa pessoa = buscarEntidade(id);
        pessoa.setAuthUserId(authUserId);
        return paraResponse(pessoa);
    }

    @Transactional(readOnly = true)
    public MeResponse buscarStatusPorAuthUserId(UUID authUserId) {
        return pessoaRepository.findByAuthUserId(authUserId)
                .map(pessoa -> new MeResponse(pessoa.getAprovadoEm() != null, paraResponse(pessoa)))
                .orElseGet(() -> new MeResponse(false, null));
    }

    public PessoaResponse autoCadastro(Jwt jwt, AutoCadastroRequest request) {
        UUID authUserId = UUID.fromString(jwt.getSubject());

        if (pessoaRepository.findByAuthUserId(authUserId).isPresent()) {
            throw new EstadoInvalidoException("Já existe um cadastro vinculado a este usuário");
        }

        Area area = areaRepository.findById(request.areaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Area não encontrada: " + request.areaId()));

        Pessoa pessoa = new Pessoa(request.nome(), area);
        pessoa.setEmail(jwt.getClaimAsString("email"));
        pessoa.setCargo(resolverCargo(request.cargoId()));
        pessoa.setAuthUserId(authUserId);
        // aprovadoEm/aprovadoPor ficam null de propósito — pendente de aprovação

        return paraResponse(pessoaRepository.save(pessoa));
    }

    public PessoaResponse aprovar(UUID id, Jwt jwt, AprovarRequest request) {
        Pessoa aprovador = currentPessoaResolver.resolver(jwt);
        Pessoa alvo = buscarEntidade(id);

        boolean gestorTentandoElevarOuMexerEmAdmin =
                aprovador.getPerfil() == PerfilPessoa.GESTOR
                        && (request.perfil() == PerfilPessoa.ADMIN || alvo.getPerfil() == PerfilPessoa.ADMIN);

        if (gestorTentandoElevarOuMexerEmAdmin) {
            throw new AccessDeniedException("Gestor não pode conceder ADMIN nem alterar uma Pessoa ADMIN");
        }

        if (request.areaId() != null && !request.areaId().equals(alvo.getArea().getId())) {
            Area novaArea = areaRepository.findById(request.areaId())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Area não encontrada: " + request.areaId()));
            registrarAuditoria(alvo.getId(), "area_id", alvo.getArea().getId().toString(), novaArea.getId().toString(), aprovador.getId());
            alvo.setArea(novaArea);
        }

        UUID cargoAtualId = alvo.getCargo() != null ? alvo.getCargo().getId() : null;
        if (request.cargoId() != null && !request.cargoId().equals(cargoAtualId)) {
            Cargo novoCargo = resolverCargo(request.cargoId());
            registrarAuditoria(
                    alvo.getId(), "cargo_id",
                    cargoAtualId != null ? cargoAtualId.toString() : null,
                    request.cargoId().toString(),
                    aprovador.getId()
            );
            alvo.setCargo(novoCargo);
        }

        Area referenciaArea = request.referenciaAreaId() != null
                ? areaRepository.findById(request.referenciaAreaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Area não encontrada: " + request.referenciaAreaId()))
                : null;

        alvo.setPerfil(request.perfil());
        alvo.setReferenciaArea(referenciaArea);
        alvo.setAprovadoEm(OffsetDateTime.now());
        alvo.setAprovadoPor(aprovador);

        return paraResponse(alvo);
    }

    private void registrarAuditoria(UUID registroId, String campo, String valorAnterior, String valorNovo, UUID alteradoPor) {
        auditoriaRepository.save(new Auditoria("pessoa", registroId, campo, valorAnterior, valorNovo, alteradoPor));
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
        Pessoa aprovadoPor = pessoa.getAprovadoPor();
        return new PessoaResponse(
                pessoa.getId(), pessoa.getNome(), pessoa.getEmail(),
                pessoa.getArea().getDiretoria().getId(), pessoa.getArea().getDiretoria().getNome(),
                pessoa.getArea().getId(), pessoa.getArea().getNome(),
                cargo != null ? cargo.getId() : null,
                cargo != null ? cargo.getNome() : null,
                pessoa.getStatus(), pessoa.getPerfil(),
                pessoa.getAuthUserId(),
                aprovadoPor != null ? aprovadoPor.getId() : null,
                aprovadoPor != null ? aprovadoPor.getNome() : null,
                pessoa.getAprovadoEm(),
                pessoa.getCreatedAt(), pessoa.getUpdatedAt()
        );
    }
}