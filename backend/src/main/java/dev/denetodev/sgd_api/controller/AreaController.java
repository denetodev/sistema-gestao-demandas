package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.request.AreaRequest;
import dev.denetodev.sgd_api.dto.response.AreaResponse;
import dev.denetodev.sgd_api.service.AreaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/areas")
public class AreaController {

    private final AreaService areaService;

    public AreaController(AreaService areaService) {
        this.areaService = areaService;
    }

    @GetMapping
    public List<AreaResponse> listar() {
        return areaService.listarTodas();
    }

    @GetMapping("/{id}")
    public AreaResponse buscarPorId(@PathVariable UUID id) {
        return areaService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<AreaResponse> criar(@Valid @RequestBody AreaRequest request) {
        AreaResponse criada = areaService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).location(URI.create("/areas/" + criada.id())).body(criada);
    }

    @PutMapping("/{id}")
    public AreaResponse atualizar(@PathVariable UUID id, @Valid @RequestBody AreaRequest request) {
        return areaService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        areaService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}