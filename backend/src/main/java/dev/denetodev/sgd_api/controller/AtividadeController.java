package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.request.AtividadeRequest;
import dev.denetodev.sgd_api.dto.response.AtividadeResponse;
import dev.denetodev.sgd_api.entity.EscopoListagem;
import dev.denetodev.sgd_api.service.AtividadeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/atividades")
public class AtividadeController {

    private final AtividadeService atividadeService;

    public AtividadeController(AtividadeService atividadeService) {
        this.atividadeService = atividadeService;
    }

    @GetMapping
    public Page<AtividadeResponse> listar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID demandaId,
            @RequestParam(required = false, defaultValue = "MINHAS") EscopoListagem escopo,
            @PageableDefault(size = 20, sort = "dataRealizacao", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        if (demandaId != null) {
            List<AtividadeResponse> lista = atividadeService.listarPorDemanda(demandaId);
            Pageable paginaUnica = lista.isEmpty()
                    ? Pageable.unpaged()
                    : PageRequest.of(0, lista.size());
            return new PageImpl<>(lista, paginaUnica, lista.size());
        }
        return atividadeService.listarComEscopo(jwt, escopo, pageable);
    }

    @GetMapping("/{id}")
    public AtividadeResponse buscarPorId(@PathVariable UUID id) {
        return atividadeService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<AtividadeResponse> criar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AtividadeRequest request) {
        AtividadeResponse criada = atividadeService.criar(jwt, request);
        return ResponseEntity.status(HttpStatus.CREATED).location(URI.create("/atividades/" + criada.id())).body(criada);
    }

    @PutMapping("/{id}")
    public AtividadeResponse atualizar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody AtividadeRequest request) {
        return atividadeService.atualizar(jwt, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        atividadeService.remover(jwt, id);
        return ResponseEntity.noContent().build();
    }
}