package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.request.EvidenciaRequest;
import dev.denetodev.sgd_api.dto.response.EvidenciaResponse;
import dev.denetodev.sgd_api.service.EvidenciaService;
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
    public ResponseEntity<EvidenciaResponse> criar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody EvidenciaRequest request) {
        EvidenciaResponse criada = evidenciaService.criar(jwt, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        evidenciaService.remover(jwt, id);
        return ResponseEntity.noContent().build();
    }
}