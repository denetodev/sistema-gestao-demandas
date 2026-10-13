package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.request.NucleoRequest;
import dev.denetodev.sgd_api.dto.response.NucleoResponse;
import dev.denetodev.sgd_api.service.NucleoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/nucleos")
public class NucleoController {

    private final NucleoService nucleoService;

    public NucleoController(NucleoService nucleoService) {
        this.nucleoService = nucleoService;
    }

    @GetMapping
    public List<NucleoResponse> listar() {
        return nucleoService.listarTodos();
    }

    @GetMapping("/{id}")
    public NucleoResponse buscarPorId(@PathVariable UUID id) {
        return nucleoService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<NucleoResponse> criar(@Valid @RequestBody NucleoRequest request) {
        NucleoResponse criado = nucleoService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).location(URI.create("/nucleos/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public NucleoResponse atualizar(@PathVariable UUID id, @Valid @RequestBody NucleoRequest request) {
        return nucleoService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        nucleoService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}
