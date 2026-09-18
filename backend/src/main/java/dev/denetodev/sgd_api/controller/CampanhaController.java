package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.request.CampanhaRequest;
import dev.denetodev.sgd_api.dto.response.CampanhaResponse;
import dev.denetodev.sgd_api.service.CampanhaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/campanhas")
public class CampanhaController {

    private final CampanhaService campanhaService;

    public CampanhaController(CampanhaService campanhaService) {
        this.campanhaService = campanhaService;
    }

    @GetMapping
    public List<CampanhaResponse> listar() {
        return campanhaService.listarTodas();
    }

    @GetMapping("/{id}")
    public CampanhaResponse buscarPorId(@PathVariable UUID id) {
        return campanhaService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<CampanhaResponse> criar(@Valid @RequestBody CampanhaRequest request) {
        CampanhaResponse criada = campanhaService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).location(URI.create("/campanhas/" + criada.id())).body(criada);
    }

    @PutMapping("/{id}")
    public CampanhaResponse atualizar(@PathVariable UUID id, @Valid @RequestBody CampanhaRequest request) {
        return campanhaService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        campanhaService.remover(id);
        return ResponseEntity.noContent().build();
    }
}