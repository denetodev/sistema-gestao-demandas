package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.request.EtiquetaPlannerRequest;
import dev.denetodev.sgd_api.dto.response.PessoaResponse;
import dev.denetodev.sgd_api.dto.response.PlannerSincronizacaoResponse;
import dev.denetodev.sgd_api.dto.response.PlannerSugestaoResponse;
import dev.denetodev.sgd_api.service.PessoaService;
import dev.denetodev.sgd_api.service.PlannerSugestaoService;
import dev.denetodev.sgd_api.service.PlannerSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.UUID;

/** Tarefas do Planner que pedem a decisão da pessoa. Cada um vê e decide só as próprias sugestões. */
@RestController
@RequestMapping("/planner")
public class PlannerController {

    private final PlannerSugestaoService sugestaoService;
    private final PlannerSyncService syncService;
    private final PessoaService pessoaService;

    public PlannerController(PlannerSugestaoService sugestaoService, PlannerSyncService syncService, PessoaService pessoaService) {
        this.sugestaoService = sugestaoService;
        this.syncService = syncService;
        this.pessoaService = pessoaService;
    }

    @GetMapping("/sugestoes")
    public java.util.List<PlannerSugestaoResponse> minhas(@AuthenticationPrincipal Jwt jwt) {
        return sugestaoService.minhas(jwt);
    }

    @PostMapping("/sugestoes/{id}/aceitar")
    public Map<String, UUID> aceitar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return Map.of("demandaId", sugestaoService.aceitar(jwt, id));
    }

    @PostMapping("/sugestoes/{id}/recusar")
    public ResponseEntity<Void> recusar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        sugestaoService.recusar(jwt, id);
        return ResponseEntity.noContent().build();
    }

    /** Gestor e Admin: busca no Planner agora, sem esperar a próxima rodada. */
    @PostMapping("/sincronizar")
    public PlannerSincronizacaoResponse sincronizar() {
        return syncService.sincronizar();
    }

    /** Gestor e Admin: a etiqueta que a pessoa usa no plano. */
    @PutMapping("/pessoas/{id}/etiqueta")
    public PessoaResponse definirEtiqueta(@PathVariable UUID id, @RequestBody EtiquetaPlannerRequest request) {
        return pessoaService.definirEtiquetaPlanner(id, request.etiqueta());
    }
}
