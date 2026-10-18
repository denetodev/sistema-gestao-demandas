package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.CargoRequest;
import dev.denetodev.sgd_api.dto.response.CargoResponse;
import dev.denetodev.sgd_api.entity.Cargo;
import dev.denetodev.sgd_api.entity.StatusPessoa;
import dev.denetodev.sgd_api.exception.EstadoInvalidoException;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.PessoaRepository;
import dev.denetodev.sgd_api.repository.CargoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class CargoService {

    private final CargoRepository cargoRepository;
    private final PessoaRepository pessoaRepository;

    public CargoService(CargoRepository cargoRepository, PessoaRepository pessoaRepository) {
        this.cargoRepository = cargoRepository;
        this.pessoaRepository = pessoaRepository;
    }

    @Transactional(readOnly = true)
    public List<CargoResponse> listarTodas() {
        return cargoRepository.findAll().stream().map(this::paraResponse).toList();
    }

    @Transactional(readOnly = true)
    public CargoResponse buscarPorId(UUID id) {
        return paraResponse(buscarEntidade(id));
    }

    public CargoResponse criar(CargoRequest request) {
        String nome = request.nome().trim();
        String descricao = limpar(request.descricao());
        // o nome é único no banco, inclusive entre os desativados: recriar um cargo desativado o reativa
        Optional<Cargo> existente = cargoRepository.findByNomeIgnoreCase(nome);
        if (existente.isPresent()) {
            Cargo cargo = existente.get();
            if (cargo.isAtivo()) {
                throw new EstadoInvalidoException("Já existe um cargo com esse nome");
            }
            cargo.setAtivo(true);
            cargo.setDescricao(descricao);
            return paraResponse(cargo);
        }
        Cargo cargo = new Cargo(nome);
        cargo.setDescricao(descricao);
        return paraResponse(cargoRepository.save(cargo));
    }

    public CargoResponse atualizar(UUID id, CargoRequest request) {
        Cargo cargo = buscarEntidade(id);
        String nome = request.nome().trim();
        if (cargoRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new EstadoInvalidoException("Já existe um cargo com esse nome (talvez desativado: crie com o mesmo nome para reativar)");
        }
        cargo.setNome(nome);
        cargo.setDescricao(limpar(request.descricao()));
        return paraResponse(cargo);
    }

    public void desativar(UUID id) {
        Cargo cargo = buscarEntidade(id);
        if (pessoaRepository.existsByCargoIdAndStatus(id, StatusPessoa.ATIVO)) {
            throw new EstadoInvalidoException("Este cargo ainda tem pessoas ativas. Mude o cargo delas antes");
        }
        cargo.setAtivo(false);
    }

    private static String limpar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private Cargo buscarEntidade(UUID id) {
        return cargoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cargo não encontrado: " + id));
    }

    private CargoResponse paraResponse(Cargo c) {
        return new CargoResponse(c.getId(), c.getNome(), c.getDescricao(), c.isAtivo(), c.getCreatedAt(), c.getUpdatedAt());
    }
}