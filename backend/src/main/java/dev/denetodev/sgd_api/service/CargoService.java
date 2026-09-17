package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.CargoRequest;
import dev.denetodev.sgd_api.dto.response.CargoResponse;
import dev.denetodev.sgd_api.entity.Cargo;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
import dev.denetodev.sgd_api.repository.CargoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CargoService {

    private final CargoRepository cargoRepository;

    public CargoService(CargoRepository cargoRepository) {
        this.cargoRepository = cargoRepository;
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
        Cargo cargo = new Cargo(request.nome());
        cargo.setDescricao(request.descricao());
        return paraResponse(cargoRepository.save(cargo));
    }

    public CargoResponse atualizar(UUID id, CargoRequest request) {
        Cargo cargo = buscarEntidade(id);
        cargo.setNome(request.nome());
        cargo.setDescricao(request.descricao());
        return paraResponse(cargo);
    }

    public void desativar(UUID id) {
        buscarEntidade(id).setAtivo(false);
    }

    private Cargo buscarEntidade(UUID id) {
        return cargoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cargo não encontrado: " + id));
    }

    private CargoResponse paraResponse(Cargo c) {
        return new CargoResponse(c.getId(), c.getNome(), c.getDescricao(), c.isAtivo(), c.getCreatedAt(), c.getUpdatedAt());
    }
}