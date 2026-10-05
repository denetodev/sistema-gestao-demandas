package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.response.DashboardResponse;
import dev.denetodev.sgd_api.entity.VisaoDashboard;
import dev.denetodev.sgd_api.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.UUID;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /**
     * @param visao       MINHA, EQUIPE ou DIRETORIA; sem valor, escolhe pelo perfil de quem pede
     * @param pessoaId    só na visão EQUIPE: abre o individual de um colega da área
     * @param diretoriaId só na visão DIRETORIA (Gestor/Admin): sem valor, todas as diretorias
     * @param porPessoa   Gestor/Admin: inclui o detalhamento individual
     * @param mes         yyyy-MM; padrão é o mês atual
     */
    @GetMapping
    public DashboardResponse buscar(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) VisaoDashboard visao,
            @RequestParam(required = false) UUID pessoaId,
            @RequestParam(required = false) UUID diretoriaId,
            @RequestParam(defaultValue = "false") boolean porPessoa,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes
    ) {
        return dashboardService.montar(jwt, visao, pessoaId, diretoriaId, porPessoa, mes);
    }
}
