package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.request.AprovarRequest;
import dev.denetodev.sgd_api.dto.request.AutoCadastroRequest;
import dev.denetodev.sgd_api.dto.request.PessoaRequest;
import dev.denetodev.sgd_api.dto.request.VincularAuthRequest;
import dev.denetodev.sgd_api.dto.response.MeResponse;
import dev.denetodev.sgd_api.dto.response.PessoaResponse;
import dev.denetodev.sgd_api.service.PessoaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/pessoas")
public class PessoaController {

    private final PessoaService pessoaService;

    public PessoaController(PessoaService pessoaService) {
        this.pessoaService = pessoaService;
    }

    @GetMapping
    public List<PessoaResponse> listar() {
        return pessoaService.listarTodas();
    }

    @GetMapping("/{id}")
    public PessoaResponse buscarPorId(@PathVariable UUID id) {
        return pessoaService.buscarPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PessoaResponse> criar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody PessoaRequest request) {
        PessoaResponse criada = pessoaService.criar(jwt, request);
        return ResponseEntity.status(HttpStatus.CREATED).location(URI.create("/pessoas/" + criada.id())).body(criada);
    }

    @PutMapping("/{id}")
    public PessoaResponse atualizar(@PathVariable UUID id, @Valid @RequestBody PessoaRequest request) {
        return pessoaService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        pessoaService.desativar(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/vincular-auth")
    @PreAuthorize("hasRole('ADMIN')")
    public PessoaResponse vincularAuth(@PathVariable UUID id, @Valid @RequestBody VincularAuthRequest request) {
        return pessoaService.vincularAuth(id, request.authUserId());
    }

    @GetMapping("/me")
    public MeResponse meuPerfil(@AuthenticationPrincipal Jwt jwt) {
        UUID authUserId = UUID.fromString(jwt.getSubject());
        return pessoaService.buscarStatusPorAuthUserId(authUserId);
    }

    @PostMapping("/auto-cadastro")
    public ResponseEntity<PessoaResponse> autoCadastro(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AutoCadastroRequest request
    ) {
        PessoaResponse criada = pessoaService.autoCadastro(jwt, request);
        return ResponseEntity.status(HttpStatus.CREATED).location(URI.create("/pessoas/" + criada.id())).body(criada);
    }

    @PatchMapping("/{id}/aprovar")
    @PreAuthorize("hasAnyRole('ADMIN', 'GESTOR')")
    public PessoaResponse aprovar(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AprovarRequest request
    ) {
        return pessoaService.aprovar(id, jwt, request);
    }
}