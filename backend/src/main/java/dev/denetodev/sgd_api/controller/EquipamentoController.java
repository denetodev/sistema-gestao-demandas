package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.request.EquipamentoRequest;
import dev.denetodev.sgd_api.dto.request.TermoEquipamentoRequest;
import dev.denetodev.sgd_api.dto.response.EquipamentoResponse;
import dev.denetodev.sgd_api.dto.response.TermoEquipamentoResponse;
import dev.denetodev.sgd_api.service.EquipamentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/equipamentos")
public class EquipamentoController {

    private final EquipamentoService equipamentoService;

    public EquipamentoController(EquipamentoService equipamentoService) {
        this.equipamentoService = equipamentoService;
    }

    @GetMapping
    public List<EquipamentoResponse> listar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID categoriaId,
            @RequestParam(required = false) String busca
    ) {
        return equipamentoService.listar(jwt, categoriaId, busca);
    }

    /** Termo de responsabilidade vigente (texto mostrado na tela antes do OK). */
    @GetMapping("/termo")
    public TermoEquipamentoResponse termo(@AuthenticationPrincipal Jwt jwt) {
        return equipamentoService.termoAtual(jwt);
    }

    /** Publica uma versão nova do termo. As versões antigas continuam valendo para os empréstimos que as aceitaram. */
    @PostMapping("/termo")
    public ResponseEntity<TermoEquipamentoResponse> publicarTermo(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody TermoEquipamentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipamentoService.publicarTermo(jwt, request));
    }

    @GetMapping("/{id}")
    public EquipamentoResponse buscar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return equipamentoService.buscar(jwt, id);
    }

    @PostMapping
    public ResponseEntity<EquipamentoResponse> criar(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody EquipamentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipamentoService.criar(jwt, request));
    }

    @PutMapping("/{id}")
    public EquipamentoResponse atualizar(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody EquipamentoRequest request) {
        return equipamentoService.atualizar(jwt, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        equipamentoService.desativar(jwt, id);
        return ResponseEntity.noContent().build();
    }
}
