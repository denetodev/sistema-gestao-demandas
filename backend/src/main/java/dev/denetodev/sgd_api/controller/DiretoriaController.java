package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.request.DiretoriaRequest;
import dev.denetodev.sgd_api.dto.response.DiretoriaResponse;
import dev.denetodev.sgd_api.service.DiretoriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/diretorias")
public class DiretoriaController {

    private final DiretoriaService diretoriaService;

    public DiretoriaController(DiretoriaService diretoriaService) {
        this.diretoriaService = diretoriaService;
    }

    @GetMapping
    public List<DiretoriaResponse> listar() {
        return diretoriaService.listarTodas();
    }

    @GetMapping("/{id}")
    public DiretoriaResponse buscarPorId(@PathVariable UUID id) {
        return diretoriaService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<DiretoriaResponse> criar(@Valid @RequestBody DiretoriaRequest request) {
        DiretoriaResponse criada = diretoriaService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).location(URI.create("/diretorias/" + criada.id())).body(criada);
    }

    @PutMapping("/{id}")
    public DiretoriaResponse atualizar(@PathVariable UUID id, @Valid @RequestBody DiretoriaRequest request) {
        return diretoriaService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        diretoriaService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}