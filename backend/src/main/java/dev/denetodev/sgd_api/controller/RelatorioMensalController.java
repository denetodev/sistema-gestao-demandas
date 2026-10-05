package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.request.SalvarRelatorioRequest;
import dev.denetodev.sgd_api.dto.response.RelatorioResponse;
import dev.denetodev.sgd_api.dto.response.RelatorioResumoResponse;
import dev.denetodev.sgd_api.service.RelatorioDocxService;
import dev.denetodev.sgd_api.service.RelatorioMensalService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.text.Normalizer;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/** Relatórios mensais. O mês vai no caminho, no formato yyyy-MM (ex.: /relatorios/2026-03). */
@RestController
@RequestMapping("/relatorios")
public class RelatorioMensalController {

    private final RelatorioMensalService relatorioService;
    private final RelatorioDocxService docxService;

    public RelatorioMensalController(RelatorioMensalService relatorioService, RelatorioDocxService docxService) {
        this.relatorioService = relatorioService;
        this.docxService = docxService;
    }

    /** Gestor/Admin: situação do relatório do mês de cada profissional. */
    @GetMapping("/resumo")
    public List<RelatorioResumoResponse> resumo(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes
    ) {
        return relatorioService.resumo(jwt, mes);
    }

    /** Sem {@code pessoaId}, é o relatório de quem pede. Com ele, só Gestor/Admin. */
    @GetMapping("/{mes}")
    public RelatorioResponse buscar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
            @RequestParam(required = false) UUID pessoaId
    ) {
        return relatorioService.buscar(jwt, mes, pessoaId);
    }

    /** DOCX para assinar e enviar. Mesmas regras de acesso do {@code GET /{mes}}. */
    @GetMapping("/{mes}/docx")
    public ResponseEntity<byte[]> docx(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
            @RequestParam(required = false) UUID pessoaId
    ) {
        RelatorioResponse relatorio = relatorioService.buscar(jwt, mes, pessoaId);
        String arquivo = "relatorio-" + mes + "-" + slug(relatorio.pessoaNome()) + ".docx";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(arquivo).build().toString())
                .body(docxService.gerar(relatorio));
    }

    private static String slug(String nome) {
        String semAcento = Normalizer.normalize(nome, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
    }

    @PutMapping("/{mes}")
    public RelatorioResponse salvar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
            @RequestBody SalvarRelatorioRequest request
    ) {
        return relatorioService.salvar(jwt, mes, request);
    }

    @PostMapping("/{mes}/aprovar")
    public RelatorioResponse aprovar(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes
    ) {
        return relatorioService.aprovar(jwt, mes);
    }

    /** A própria pessoa reabre o seu; Gestor/Admin reabrem o de qualquer um via {@code pessoaId}. */
    @PostMapping("/{mes}/reabrir")
    public RelatorioResponse reabrir(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable @DateTimeFormat(pattern = "yyyy-MM") YearMonth mes,
            @RequestParam(required = false) UUID pessoaId
    ) {
        return relatorioService.reabrir(jwt, mes, pessoaId);
    }
}
