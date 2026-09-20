package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.request.EvidenciaRequest;
import dev.denetodev.sgd_api.dto.response.EvidenciaResponse;
import dev.denetodev.sgd_api.service.EvidenciaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/evidencias")
public class EvidenciaController {

    private final EvidenciaService evidenciaService;

    public EvidenciaController(EvidenciaService evidenciaService) {
        this.evidenciaService = evidenciaService;
    }

    @GetMapping
    public List<EvidenciaResponse> listar() {
        return evidenciaService.listarTodas();
    }

    @GetMapping("/{id}")
    public EvidenciaResponse buscarPorId(@PathVariable UUID id) {
        return evidenciaService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<EvidenciaResponse> criar(@Valid @RequestBody EvidenciaRequest request) {
        EvidenciaResponse criada = evidenciaService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).location(URI.create("/evidencias/" + criada.id())).body(criada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        evidenciaService.remover(id);
        return ResponseEntity.noContent().build();
    }
}