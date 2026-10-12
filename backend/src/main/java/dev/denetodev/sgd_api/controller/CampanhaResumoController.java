package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.response.CampanhaDetalheResponse;
import dev.denetodev.sgd_api.dto.response.CampanhaResumoResponse;
import dev.denetodev.sgd_api.service.CampanhaResumoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** Leitura: quantas demandas, pessoas e quanto valor cada campanha reuniu. */
@RestController
@RequestMapping("/campanhas")
public class CampanhaResumoController {

    private final CampanhaResumoService campanhaResumoService;

    public CampanhaResumoController(CampanhaResumoService campanhaResumoService) {
        this.campanhaResumoService = campanhaResumoService;
    }

    @GetMapping("/resumo")
    public List<CampanhaResumoResponse> resumirTodas() {
        return campanhaResumoService.resumirTodas();
    }

    @GetMapping("/{id}/resumo")
    public CampanhaDetalheResponse detalhar(@PathVariable UUID id) {
        return campanhaResumoService.detalhar(id);
    }
}
