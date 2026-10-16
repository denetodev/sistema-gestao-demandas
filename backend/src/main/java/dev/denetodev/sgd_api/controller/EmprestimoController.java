package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.dto.request.DevolucaoRequest;
import dev.denetodev.sgd_api.dto.request.EmprestimoEdicaoRequest;
import dev.denetodev.sgd_api.dto.request.EmprestimoRequest;
import dev.denetodev.sgd_api.dto.request.ProrrogacaoRequest;
import dev.denetodev.sgd_api.dto.response.ResumoEmprestimosResponse;
import dev.denetodev.sgd_api.dto.request.RecusarEmprestimoRequest;
import dev.denetodev.sgd_api.dto.response.EmprestimoResponse;
import dev.denetodev.sgd_api.dto.response.FotoEmprestimoResponse;
import dev.denetodev.sgd_api.entity.MomentoFoto;
import dev.denetodev.sgd_api.service.EmprestimoFotoService;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import dev.denetodev.sgd_api.service.EmprestimoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/emprestimos")
public class EmprestimoController {

    private final EmprestimoService emprestimoService;
    private final EmprestimoFotoService fotoService;

    public EmprestimoController(EmprestimoService emprestimoService, EmprestimoFotoService fotoService) {
        this.emprestimoService = emprestimoService;
        this.fotoService = fotoService;
    }

    /** grupo: ATIVOS (padrão), HISTORICO ou TODOS. Quem só consulta vê apenas o que está retirado. */
    @GetMapping
    public List<EmprestimoResponse> listar(
            @AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) EmprestimoService.Grupo grupo) {
        return emprestimoService.listar(jwt, grupo);
    }

    /** Contadores do aviso no menu (aguardando autorização, externos esperando, atrasados). */
    @GetMapping("/resumo")
    public ResumoEmprestimosResponse resumo(@AuthenticationPrincipal Jwt jwt) {
        return emprestimoService.resumo(jwt);
    }

    @GetMapping("/{id}")
    public EmprestimoResponse buscar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return emprestimoService.buscar(jwt, id);
    }

    @PostMapping
    public ResponseEntity<EmprestimoResponse> registrar(
            @AuthenticationPrincipal Jwt jwt, @Valid @RequestBody EmprestimoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(emprestimoService.registrar(jwt, request));
    }

    /** Corrige dados de um empréstimo que ainda espera autorização. */
    @PutMapping("/{id}")
    public EmprestimoResponse editar(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody EmprestimoEdicaoRequest request) {
        return emprestimoService.editar(jwt, id, request);
    }

    /** Estende a devolução prevista de quem já está com o equipamento. */
    @PatchMapping("/{id}/prorrogar")
    public EmprestimoResponse prorrogar(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody ProrrogacaoRequest request) {
        return emprestimoService.prorrogar(jwt, id, request);
    }

    @PatchMapping("/{id}/autorizar")
    public EmprestimoResponse autorizar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return emprestimoService.autorizar(jwt, id);
    }

    @PatchMapping("/{id}/recusar")
    public EmprestimoResponse recusar(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @Valid @RequestBody RecusarEmprestimoRequest request) {
        return emprestimoService.recusar(jwt, id, request);
    }

    /** Confirma a entrega do equipamento (saída). */
    @PatchMapping("/{id}/retirar")
    public EmprestimoResponse retirar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return emprestimoService.retirar(jwt, id);
    }

    @PatchMapping("/{id}/devolver")
    public EmprestimoResponse devolver(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @RequestBody(required = false) DevolucaoRequest request) {
        return emprestimoService.devolver(jwt, id, request);
    }

    /** Fotos opcionais do equipamento. A lista traz URLs assinadas de curta duração (bucket privado). */
    @GetMapping("/{id}/fotos")
    public List<FotoEmprestimoResponse> fotos(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return fotoService.listar(jwt, id);
    }

    @PostMapping(path = "/{id}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FotoEmprestimoResponse> enviarFoto(
            @AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
            @RequestParam MomentoFoto momento, @RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fotoService.enviar(jwt, id, momento, arquivo));
    }

    @DeleteMapping("/{id}/fotos/{fotoId}")
    public ResponseEntity<Void> removerFoto(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id, @PathVariable UUID fotoId) {
        fotoService.remover(jwt, id, fotoId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/cancelar")
    public EmprestimoResponse cancelar(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return emprestimoService.cancelar(jwt, id);
    }
}
