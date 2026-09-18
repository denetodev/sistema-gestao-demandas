package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.request.ProjetoRequest;
import dev.denetodev.sgd_api.dto.response.ProjetoResponse;
import dev.denetodev.sgd_api.service.ProjetoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/projetos")
public class ProjetoController {

    private final ProjetoService projetoService;

    public ProjetoController(ProjetoService projetoService) {
        this.projetoService = projetoService;
    }

    @GetMapping
    public List<ProjetoResponse> listar() {
        return projetoService.listarTodas();
    }

    @GetMapping("/{id}")
    public ProjetoResponse buscarPorId(@PathVariable UUID id) {
        return projetoService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<ProjetoResponse> criar(@Valid @RequestBody ProjetoRequest request) {
        ProjetoResponse criado = projetoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).location(URI.create("/projetos/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public ProjetoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody ProjetoRequest request) {
        return projetoService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> encerrar(@PathVariable UUID id) {
        projetoService.encerrar(id);
        return ResponseEntity.noContent().build();
    }
}