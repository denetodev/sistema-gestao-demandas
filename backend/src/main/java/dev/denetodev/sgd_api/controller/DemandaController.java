package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.request.DemandaRequest;
import dev.denetodev.sgd_api.dto.response.DemandaResponse;
import dev.denetodev.sgd_api.service.DemandaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}