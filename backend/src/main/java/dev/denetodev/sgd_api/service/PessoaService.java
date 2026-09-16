package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.PessoaRequest;
import dev.denetodev.sgd_api.dto.response.PessoaResponse;
import dev.denetodev.sgd_api.entity.Diretoria;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.entity.StatusPessoa;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
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

    public PessoaService(PessoaRepository pessoaRepository, DiretoriaRepository diretoriaRepository) {
        this.pessoaRepository = pessoaRepository;
        this.diretoriaRepository = diretoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<PessoaResponse> listarTodas() {
        return pessoaRepository.findAll().stream()
                .map(this::paraResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PessoaResponse buscarPorId(UUID id) {
        Pessoa pessoa = pessoaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa não encontrada: " + id));
        return paraResponse(pessoa);
    }

    public PessoaResponse criar(PessoaRequest request) {
        Diretoria diretoria = diretoriaRepository.findById(request.diretoriaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Diretoria não encontrada: " + request.diretoriaId()));

        Pessoa pessoa = new Pessoa(request.nome(), diretoria, request.areaId());
        pessoa.setEmail(request.email());
        pessoa.setCargoId(request.cargoId());
        if (request.status() != null) {
            pessoa.setStatus(request.status());
        }

        Pessoa salva = pessoaRepository.save(pessoa);
        return paraResponse(salva);
    }

    public PessoaResponse atualizar(UUID id, PessoaRequest request) {
        Pessoa pessoa = pessoaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa não encontrada: " + id));

        Diretoria diretoria = diretoriaRepository.findById(request.diretoriaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Diretoria não encontrada: " + request.diretoriaId()));

        pessoa.setNome(request.nome());
        pessoa.setEmail(request.email());
        pessoa.setDiretoria(diretoria);
        pessoa.setAreaId(request.areaId());
        pessoa.setCargoId(request.cargoId());
        if (request.status() != null) {
            pessoa.setStatus(request.status());
        }

        return paraResponse(pessoa);
    }

    public void desativar(UUID id) {
        Pessoa pessoa = pessoaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Pessoa não encontrada: " + id));
        pessoa.setStatus(StatusPessoa.INATIVO);
    }

    private PessoaResponse paraResponse(Pessoa pessoa) {
        return new PessoaResponse(
                pessoa.getId(),
                pessoa.getNome(),
                pessoa.getEmail(),
                pessoa.getDiretoria().getId(),
                pessoa.getDiretoria().getNome(),
                pessoa.getAreaId(),
                pessoa.getCargoId(),
                pessoa.getStatus(),
                pessoa.getCreatedAt(),
                pessoa.getUpdatedAt()
        );
    }
}