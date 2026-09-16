package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.request.ParticipanteRequest;
import dev.denetodev.sgd_api.dto.response.ParticipanteResponse;
import dev.denetodev.sgd_api.service.ParticipanteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            @PathVariable UUID demandaId,
            @Valid @RequestBody ParticipanteRequest request
    ) {
        ParticipanteResponse criado = participanteService.adicionar(demandaId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(criado);
    }

    @DeleteMapping("/{participanteId}")
    public ResponseEntity<Void> remover(@PathVariable UUID demandaId, @PathVariable UUID participanteId) {
        participanteService.remover(demandaId, participanteId);
        return ResponseEntity.noContent().build();
    }
}