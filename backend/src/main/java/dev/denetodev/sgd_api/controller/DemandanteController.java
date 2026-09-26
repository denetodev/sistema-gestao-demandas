package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.request.DemandanteRequest;
import dev.denetodev.sgd_api.dto.response.DemandanteResponse;
import dev.denetodev.sgd_api.service.DemandanteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/demandantes")
public class DemandanteController {

    private final DemandanteService demandanteService;

    public DemandanteController(DemandanteService demandanteService) {
        this.demandanteService = demandanteService;
    }

    @GetMapping
    public List<DemandanteResponse> listar() {
        return demandanteService.listarTodas();
    }

    @GetMapping("/{id}")
    public DemandanteResponse buscarPorId(@PathVariable UUID id) {
        return demandanteService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<DemandanteResponse> criar(@Valid @RequestBody DemandanteRequest request) {
        DemandanteResponse criado = demandanteService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).location(URI.create("/demandantes/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public DemandanteResponse atualizar(@PathVariable UUID id, @Valid @RequestBody DemandanteRequest request) {
        return demandanteService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        demandanteService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}