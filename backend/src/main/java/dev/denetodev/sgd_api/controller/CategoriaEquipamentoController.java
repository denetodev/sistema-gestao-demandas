package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.request.CategoriaEquipamentoRequest;
import dev.denetodev.sgd_api.dto.response.CategoriaEquipamentoResponse;
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
@RequestMapping("/categorias-equipamento")
public class CategoriaEquipamentoController {

    private final EquipamentoService equipamentoService;

    public CategoriaEquipamentoController(EquipamentoService equipamentoService) {
        this.equipamentoService = equipamentoService;
    }

    @GetMapping
    public List<CategoriaEquipamentoResponse> listar(@AuthenticationPrincipal Jwt jwt) {
        return equipamentoService.listarCategorias(jwt);
    }

    @PostMapping
    public ResponseEntity<CategoriaEquipamentoResponse> criar(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CategoriaEquipamentoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipamentoService.criarCategoria(jwt, request));
    }

    @PutMapping("/{id}")
    public CategoriaEquipamentoResponse atualizar(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody CategoriaEquipamentoRequest request) {
        return equipamentoService.atualizarCategoria(jwt, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        equipamentoService.desativarCategoria(jwt, id);
        return ResponseEntity.noContent().build();
    }
}
