package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.request.TipoAtividadeRequest;
import dev.denetodev.sgd_api.dto.response.TipoAtividadeResponse;
import dev.denetodev.sgd_api.service.TipoAtividadeService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/tipos-atividade")
public class TipoAtividadeController {

    private final TipoAtividadeService tipoAtividadeService;

    public TipoAtividadeController(TipoAtividadeService tipoAtividadeService) {
        this.tipoAtividadeService = tipoAtividadeService;
    }

    @GetMapping
    public List<TipoAtividadeResponse> listar() {
        return tipoAtividadeService.listarTodas();
    }

    @GetMapping("/{id}")
    public TipoAtividadeResponse buscarPorId(@PathVariable UUID id) {
        return tipoAtividadeService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<TipoAtividadeResponse> criar(@Valid @RequestBody TipoAtividadeRequest request) {
        TipoAtividadeResponse criado = tipoAtividadeService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).location(URI.create("/tipos-atividade/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public TipoAtividadeResponse atualizar(@PathVariable UUID id, @Valid @RequestBody TipoAtividadeRequest request) {
        return tipoAtividadeService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        tipoAtividadeService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}