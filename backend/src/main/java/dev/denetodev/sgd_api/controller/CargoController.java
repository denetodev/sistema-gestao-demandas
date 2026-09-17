package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.request.CargoRequest;
import dev.denetodev.sgd_api.dto.response.CargoResponse;
import dev.denetodev.sgd_api.service.CargoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/cargos")
public class CargoController {

    private final CargoService cargoService;

    public CargoController(CargoService cargoService) {
        this.cargoService = cargoService;
    }

    @GetMapping
    public List<CargoResponse> listar() {
        return cargoService.listarTodas();
    }

    @GetMapping("/{id}")
    public CargoResponse buscarPorId(@PathVariable UUID id) {
        return cargoService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<CargoResponse> criar(@Valid @RequestBody CargoRequest request) {
        CargoResponse criado = cargoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).location(URI.create("/cargos/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public CargoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody CargoRequest request) {
        return cargoService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        cargoService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}