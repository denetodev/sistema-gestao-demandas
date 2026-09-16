package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.request.DemandaRequest;
import dev.denetodev.sgd_api.dto.request.StatusUpdateRequest;
import dev.denetodev.sgd_api.dto.response.DemandaResponse;
import dev.denetodev.sgd_api.service.DemandaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/demandas")
public class DemandaController {

    private final DemandaService demandaService;

    public DemandaController(DemandaService demandaService) {
        this.demandaService = demandaService;
    }

    @GetMapping
    public List<DemandaResponse> listar() {
        return demandaService.listarTodas();
    }

    @GetMapping("/{id}")
    public DemandaResponse buscarPorId(@PathVariable UUID id) {
        return demandaService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<DemandaResponse> criar(@Valid @RequestBody DemandaRequest request) {
        DemandaResponse criada = demandaService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/demandas/" + criada.id()))
                .body(criada);
    }

    @PutMapping("/{id}")
    public DemandaResponse atualizar(@PathVariable UUID id, @Valid @RequestBody DemandaRequest request) {
        return demandaService.atualizar(id, request);
    }

    @PatchMapping("/{id}/status")
    public DemandaResponse atualizarStatus(@PathVariable UUID id, @Valid @RequestBody StatusUpdateRequest request) {
        return demandaService.atualizarStatus(id, request.status());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@PathVariable UUID id) {
        demandaService.cancelar(id);
        return ResponseEntity.noContent().build();
    }
}