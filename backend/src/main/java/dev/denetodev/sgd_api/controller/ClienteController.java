package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.request.ClienteRequest;
import dev.denetodev.sgd_api.dto.response.ClienteResponse;
import dev.denetodev.sgd_api.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/clientes")
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    @GetMapping
    public List<ClienteResponse> listar() {
        return clienteService.listarTodas();
    }

    @GetMapping("/{id}")
    public ClienteResponse buscarPorId(@PathVariable UUID id) {
        return clienteService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<ClienteResponse> criar(@Valid @RequestBody ClienteRequest request) {
        ClienteResponse criado = clienteService.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).location(URI.create("/clientes/" + criado.id())).body(criado);
    }

    @PutMapping("/{id}")
    public ClienteResponse atualizar(@PathVariable UUID id, @Valid @RequestBody ClienteRequest request) {
        return clienteService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desativar(@PathVariable UUID id) {
        clienteService.desativar(id);
        return ResponseEntity.noContent().build();
    }
}