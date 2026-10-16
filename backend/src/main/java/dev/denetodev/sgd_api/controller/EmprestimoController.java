package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.request.DevolucaoRequest;
import dev.denetodev.sgd_api.dto.request.EmprestimoRequest;
import dev.denetodev.sgd_api.dto.request.RecusarEmprestimoRequest;
import dev.denetodev.sgd_api.dto.response.EmprestimoResponse;
import dev.denetodev.sgd_api.service.EmprestimoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/emprestimos")
public class EmprestimoController {

    private final EmprestimoService emprestimoService;

    public EmprestimoController(EmprestimoService emprestimoService) {
        this.emprestimoService = emprestimoService;
    }

    /** grupo: ATIVOS (padrão), HISTORICO ou TODOS. Quem só consulta vê apenas o que está retirado. */
    @GetMapping
    public List<EmprestimoResponse> listar(
            @AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) EmprestimoService.Grupo grupo) {
        return emprestimoService.listar(jwt, grupo);
    }

    @GetMapping("/{id}")
    public EmprestimoResponse buscar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return emprestimoService.buscar(jwt, id);
    }

    @PostMapping
    public ResponseEntity<EmprestimoResponse> registrar(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody EmprestimoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(emprestimoService.registrar(jwt, request));
    }

    @PatchMapping("/{id}/autorizar")
    public EmprestimoResponse autorizar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return emprestimoService.autorizar(jwt, id);
    }

    @PatchMapping("/{id}/recusar")
    public EmprestimoResponse recusar(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody RecusarEmprestimoRequest request) {
        return emprestimoService.recusar(jwt, id, request);
    }

    /** Confirma a entrega do equipamento (saída). */
    @PatchMapping("/{id}/retirar")
    public EmprestimoResponse retirar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return emprestimoService.retirar(jwt, id);
    }

    @PatchMapping("/{id}/devolver")
    public EmprestimoResponse devolver(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @RequestBody(required = false) DevolucaoRequest request) {
        return emprestimoService.devolver(jwt, id, request);
    }

    @PatchMapping("/{id}/cancelar")
    public EmprestimoResponse cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return emprestimoService.cancelar(jwt, id);
    }
}
