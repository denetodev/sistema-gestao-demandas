package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.response.NotificacaoResponse;
import dev.denetodev.sgd_api.service.NotificacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** O sininho do topo: só dá para ver e marcar os avisos da própria pessoa. */
@RestController
@RequestMapping("/notificacoes")
public class NotificacaoController {

    private final NotificacaoService notificacaoService;

    public NotificacaoController(NotificacaoService notificacaoService) {
        this.notificacaoService = notificacaoService;
    }

    @GetMapping
    public NotificacaoResponse listar(@AuthenticationPrincipal Jwt jwt) {
        return notificacaoService.listar(jwt);
    }

    @PostMapping("/vistas")
    public ResponseEntity<Void> marcarVistas(@AuthenticationPrincipal Jwt jwt) {
        notificacaoService.marcarVistas(jwt);
        return ResponseEntity.noContent().build();
    }
}
