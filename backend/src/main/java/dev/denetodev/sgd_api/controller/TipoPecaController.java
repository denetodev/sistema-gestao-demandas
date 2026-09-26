package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.request.TipoPecaRequest;
import dev.denetodev.sgd_api.dto.response.TipoPecaResponse;
import dev.denetodev.sgd_api.service.TipoPecaService;
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
@RequestMapping("/tipos-peca")
public class TipoPecaController {

    private final TipoPecaService tipoPecaService;

    public TipoPecaController(TipoPecaService tipoPecaService) {
        this.tipoPecaService = tipoPecaService;
    }

    @GetMapping
    public List<TipoPecaResponse> listar() {
        return tipoPecaService.listarTodas();
    }

    @GetMapping("/{id}")
    public TipoPecaResponse buscarPorId(@PathVariable UUID id) {
        return tipoPecaService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<TipoPecaResponse> criar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TipoPecaRequest request) {
        TipoPecaResponse criado = tipoPecaService.criar(jwt, request);
        return ResponseEntity.status(HttpStatus.CREATED).location(URI.create("/tipos-peca/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public TipoPecaResponse atualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody TipoPecaRequest request) {
        return tipoPecaService.atualizar(jwt, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        tipoPecaService.desativar(jwt, id);
        return ResponseEntity.noContent().build();
    }
}