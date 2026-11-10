package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.response.PlannerSincronizacaoResponse;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.planner.PlannerExcelLeitor;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import dev.denetodev.sgd_api.service.support.PermissaoService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

/**
 * "Atualizar pelo Planner": quem tem a chave do plano exporta o plano para o Excel e envia o arquivo aqui. O arquivo é
 * lido na hora e descartado; o que fica são só as sugestões de cada pessoa. Quem envia: Admin, Gestor e Referência de equipe.
 */
@Service
public class PlannerImportacaoService {

    private final CurrentPessoaResolver currentPessoaResolver;
    private final PermissaoService permissaoService;
    private final PlannerExcelLeitor leitor;
    private final PlannerSyncService syncService;

    public PlannerImportacaoService(CurrentPessoaResolver currentPessoaResolver, PermissaoService permissaoService,
                                    PlannerExcelLeitor leitor, PlannerSyncService syncService) {
        this.currentPessoaResolver = currentPessoaResolver;
        this.permissaoService = permissaoService;
        this.leitor = leitor;
        this.syncService = syncService;
    }

    public PlannerSincronizacaoResponse importar(Jwt jwt, MultipartFile arquivo) {
        Pessoa usuario = currentPessoaResolver.resolver(jwt);
        permissaoService.exigirAtualizarPlanner(usuario);
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Escolha o arquivo .xlsx exportado pelo Planner");
        }
        String nome = arquivo.getOriginalFilename();
        if (nome == null || !nome.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new IllegalArgumentException("O arquivo precisa ser o .xlsx exportado pelo Planner");
        }
        try (InputStream entrada = arquivo.getInputStream()) {
            return syncService.importar(leitor.ler(entrada));
        } catch (IOException e) {
            throw new IllegalArgumentException("Não consegui ler o arquivo enviado");
        }
    }
}
