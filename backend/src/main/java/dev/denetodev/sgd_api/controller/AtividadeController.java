package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.request.AtividadeRequest;
import dev.denetodev.sgd_api.dto.response.AtividadeResponse;
import dev.denetodev.sgd_api.service.AtividadeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/atividades")
public class AtividadeController {

    private final AtividadeService atividadeService;

    public AtividadeController(AtividadeService atividadeService) {
        this.atividadeService = atividadeService;
    }

    @GetMapping
    public List<AtividadeResponse> listar(@RequestParam(required = false) UUID demandaId) {
        if (demandaId != null) {
            return atividadeService.listarPorDemanda(demandaId);
        }
        return atividadeService.listarTodas();
    }

    @GetMapping("/{id}")
    public AtividadeResponse buscarPorId(@PathVariable UUID id) {
        return atividadeService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<AtividadeResponse> criar(@Valid @RequestBody AtividadeRequest request) {
        AtividadeResponse criada = atividadeService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).location(URI.create("/atividades/" + criada.id())).body(criada);
    }

    @PutMapping("/{id}")
    public AtividadeResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AtividadeRequest request) {
        return atividadeService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@PathVariable UUID id) {
        atividadeService.remover(id);
        return ResponseEntity.noContent().build();
    }
}