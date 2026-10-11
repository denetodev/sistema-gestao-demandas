package dev.denetodev.sgd_api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Healthcheck público para o Railway. Só diz que o processo subiu: se o banco ou o schema
 * estivessem errados, o Spring nem chegaria aqui (ddl-auto: validate derruba a inicialização).
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
