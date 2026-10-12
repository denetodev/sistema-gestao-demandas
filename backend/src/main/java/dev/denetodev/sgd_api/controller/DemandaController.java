package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.request.DemandaRequest;
import dev.denetodev.sgd_api.dto.request.StatusUpdateRequest;
import dev.denetodev.sgd_api.dto.response.DemandaResponse;
import dev.denetodev.sgd_api.dto.response.HistoricoStatusResponse;
import dev.denetodev.sgd_api.dto.request.FiltroDemanda;
import dev.denetodev.sgd_api.entity.EscopoListagem;
import dev.denetodev.sgd_api.entity.StatusDemanda;
import dev.denetodev.sgd_api.service.DemandaService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/demandas")
public class DemandaController {

    private final DemandaService demandaService;

    public DemandaController(DemandaService demandaService) {
        this.demandaService = demandaService;
    }

    @GetMapping
    public Page<DemandaResponse> listar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false, defaultValue = "MINHAS") EscopoListagem escopo,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) StatusDemanda status,
            @RequestParam(required = false) UUID diretoriaId,
            @RequestParam(required = false) UUID areaId,
            @PageableDefault(size = 10) Pageable pageable
    ) {
        return demandaService.listarComEscopo(jwt, new FiltroDemanda(escopo, busca, status, diretoriaId, areaId), pageable);
    }

    @GetMapping("/{id}")
    public DemandaResponse buscarPorId(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return demandaService.buscarPorId(jwt, id);
    }

    @PostMapping
    public ResponseEntity<DemandaResponse> criar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody DemandaRequest request) {
        DemandaResponse criada = demandaService.criar(jwt, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .location(URI.create("/demandas/" + criada.id()))
                .body(criada);
    }

    @PutMapping("/{id}")
    public DemandaResponse atualizar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody DemandaRequest request
    ) {
        return demandaService.atualizar(jwt, id, request);
    }

    @PatchMapping("/{id}/status")
    public DemandaResponse atualizarStatus(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @Valid @RequestBody StatusUpdateRequest request
    ) {
        return demandaService.atualizarStatus(jwt, id, request.status(), request.motivo());
    }

    /** Histórico de mudanças de status, da mais recente para a mais antiga. */
    @GetMapping("/{id}/historico")
    public List<HistoricoStatusResponse> historico(@PathVariable UUID id) {
        return demandaService.historicoDeStatus(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        demandaService.cancelar(jwt, id);
        return ResponseEntity.noContent().build();
    }
}