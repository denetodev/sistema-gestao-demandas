package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.request.PecaRequest;
import dev.denetodev.sgd_api.dto.response.PecaResponse;
import dev.denetodev.sgd_api.service.PecaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pecas")
public class PecaController {

    private final PecaService pecaService;

    public PecaController(PecaService pecaService) {
        this.pecaService = pecaService;
    }

    @GetMapping
    public List<PecaResponse> listar(@RequestParam(required = false) UUID demandaId) {
        if (demandaId != null) {
            return pecaService.listarPorDemanda(demandaId);
        }
        return pecaService.listarTodas();
    }

    @GetMapping("/{id}")
    public PecaResponse buscarPorId(@PathVariable UUID id) {
        return pecaService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<PecaResponse> criar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PecaRequest request) {
        PecaResponse criada = pecaService.criar(jwt, request);
        return ResponseEntity.status(HttpStatus.CREATED).location(URI.create("/pecas/" + criada.id())).body(criada);
    }

    @PutMapping("/{id}")
    public PecaResponse atualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody PecaRequest request) {
        return pecaService.atualizar(jwt, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        pecaService.remover(jwt, id);
        return ResponseEntity.noContent().build();
    }
}