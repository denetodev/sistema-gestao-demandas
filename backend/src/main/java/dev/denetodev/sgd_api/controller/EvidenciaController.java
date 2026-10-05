package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.request.EvidenciaRequest;
import dev.denetodev.sgd_api.dto.response.ArquivoUrlResponse;
import dev.denetodev.sgd_api.dto.response.EvidenciaResponse;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import dev.denetodev.sgd_api.service.EvidenciaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/evidencias")
public class EvidenciaController {

    private final EvidenciaService evidenciaService;

    public EvidenciaController(EvidenciaService evidenciaService) {
        this.evidenciaService = evidenciaService;
    }

    @GetMapping
    public List<EvidenciaResponse> listar() {
        return evidenciaService.listarTodas();
    }

    @GetMapping("/{id}")
    public EvidenciaResponse buscarPorId(@PathVariable UUID id) {
        return evidenciaService.buscarPorId(id);
    }

    @PostMapping
    public ResponseEntity<EvidenciaResponse> criar(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody EvidenciaRequest request) {
        EvidenciaResponse criada = evidenciaService.criar(jwt, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    /** Upload de imagem (JPEG/PNG até 5 MB). Link externo continua em POST /evidencias, tipo LINK. */
    @PostMapping(path = "/arquivo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<EvidenciaResponse> enviarArquivo(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) UUID atividadeId,
            @RequestParam(required = false) UUID pecaId,
            @RequestParam(required = false) String descricao,
            @RequestParam("arquivo") MultipartFile arquivo
    ) {
        EvidenciaResponse criada = evidenciaService.criarComArquivo(jwt, atividadeId, pecaId, descricao, arquivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(criada);
    }

    /** URL assinada, de curta duração, para ver a imagem. */
    @GetMapping("/{id}/arquivo")
    public ArquivoUrlResponse arquivo(@PathVariable UUID id) {
        return evidenciaService.urlDoArquivo(id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remover(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        evidenciaService.remover(jwt, id);
        return ResponseEntity.noContent().build();
    }
}