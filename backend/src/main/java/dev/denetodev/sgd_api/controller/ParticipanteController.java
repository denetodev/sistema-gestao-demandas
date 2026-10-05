package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.request.AtualizarPapelRequest;
import dev.denetodev.sgd_api.dto.request.ParticipanteRequest;
import dev.denetodev.sgd_api.dto.response.ParticipanteResponse;
import dev.denetodev.sgd_api.service.ParticipanteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/demandas/{demandaId}/participantes")
public class ParticipanteController {

    private final ParticipanteService participanteService;

    public ParticipanteController(ParticipanteService participanteService) {
        this.participanteService = participanteService;
    }

    @GetMapping
    public List<ParticipanteResponse> listar(@PathVariable UUID demandaId) {
        return participanteService.listarPorDemanda(demandaId);
    }

    @PostMapping
    public ResponseEntity<ParticipanteResponse> adicionar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID demandaId,
            @Valid @RequestBody ParticipanteRequest request
    ) {
        ParticipanteResponse criado = participanteService.adicionar(jwt, demandaId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @PatchMapping("/{participanteId}")
    public ParticipanteResponse atualizarPapel(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID demandaId,
            @PathVariable UUID participanteId,
            @Valid @RequestBody AtualizarPapelRequest request
    ) {
        return participanteService.atualizarPapel(jwt, demandaId, participanteId, request);
    }

    @DeleteMapping("/{participanteId}")
    public ResponseEntity<Void> remover(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID demandaId,
            @PathVariable UUID participanteId
    ) {
        participanteService.remover(jwt, demandaId, participanteId);
        return ResponseEntity.noContent().build();
    }
}
