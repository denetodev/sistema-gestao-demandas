package dev.denetodev.sgd_api.controller;

import dev.denetodev.sgd_api.config.CorsConfig;
import dev.denetodev.sgd_api.config.SecurityConfig;
import dev.denetodev.sgd_api.entity.Area;
import dev.denetodev.sgd_api.entity.PerfilPessoa;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.repository.PessoaRepository;
import dev.denetodev.sgd_api.security.PessoaAuthenticationConverter;
import dev.denetodev.sgd_api.security.RestAccessDeniedHandler;
import dev.denetodev.sgd_api.security.RestAuthenticationEntryPoint;
import dev.denetodev.sgd_api.service.*;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpMethod;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Matriz de permissões da camada HTTP: para cada endpoint de escrita (e alguns de leitura), diz quais
 * perfis passam e confere os 6 tipos de chamador, com o filtro de segurança real (SecurityConfig,
 * conversor de JWT e @PreAuthorize). Os serviços são mocks: aqui só vale quem entra e quem leva 403.
 *
 * "Passa" significa que a segurança liberou (qualquer status que não seja 401 ou 403); o que o
 * serviço decide depois (por exemplo, a Referência de equipe em TipoPeca) é testado nos testes de serviço.
 */
@WebMvcTest
@Import({SecurityConfig.class, CorsConfig.class, PessoaAuthenticationConverter.class,
        RestAuthenticationEntryPoint.class, RestAccessDeniedHandler.class})
class MatrizDePermissoesTest {

    /** Quem chama. SEM_VINCULO é uma conta com token válido mas sem Pessoa aprovada. */
    enum Chamador { ANONIMO, SEM_VINCULO, VISUALIZADOR, PROFISSIONAL, GESTOR, ADMIN }

    private static final Set<Chamador> TODOS_VINCULADOS =
            EnumSet.of(Chamador.VISUALIZADOR, Chamador.PROFISSIONAL, Chamador.GESTOR, Chamador.ADMIN);
    private static final Set<Chamador> QUEM_ESCREVE =
            EnumSet.of(Chamador.PROFISSIONAL, Chamador.GESTOR, Chamador.ADMIN);
    private static final Set<Chamador> GESTAO = EnumSet.of(Chamador.GESTOR, Chamador.ADMIN);
    private static final Set<Chamador> SO_ADMIN = EnumSet.of(Chamador.ADMIN);
    private static final Set<Chamador> QUALQUER_LOGADO = EnumSet.complementOf(EnumSet.of(Chamador.ANONIMO));

    private static final String ID = "00000000-0000-0000-0000-000000000001";

    record Caso(HttpMethod metodo, String caminho, Set<Chamador> passam) {}

    @Autowired MockMvc mvc;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean PessoaRepository pessoaRepository;
    @MockitoBean AreaService areaService;
    @MockitoBean AtividadeService atividadeService;
    @MockitoBean CampanhaService campanhaService;
    @MockitoBean CampanhaResumoService campanhaResumoService;
    @MockitoBean CargoService cargoService;
    @MockitoBean DashboardService dashboardService;
    @MockitoBean DemandaService demandaService;
    @MockitoBean DemandanteService demandanteService;
    @MockitoBean DiretoriaService diretoriaService;
    @MockitoBean NucleoService nucleoService;
    @MockitoBean PessoaFotoService pessoaFotoService;
    @MockitoBean EvidenciaService evidenciaService;
    @MockitoBean ParticipanteService participanteService;
    @MockitoBean PecaService pecaService;
    @MockitoBean PessoaService pessoaService;
    @MockitoBean VinculoSugeridoService vinculoSugeridoService;
    @MockitoBean ProjetoService projetoService;
    @MockitoBean RelatorioMensalService relatorioMensalService;
    @MockitoBean RelatorioDocxService relatorioDocxService;
    @MockitoBean TipoAtividadeService tipoAtividadeService;
    @MockitoBean TipoPecaService tipoPecaService;
    @MockitoBean EquipamentoService equipamentoService;
    @MockitoBean EmprestimoService emprestimoService;
    @MockitoBean EmprestimoFotoService emprestimoFotoService;

    @BeforeEach
    void prepararChamadores() {
        for (Chamador c : Chamador.values()) {
            if (c == Chamador.ANONIMO) {
                continue;
            }
            UUID authId = UUID.nameUUIDFromBytes(c.name().getBytes());
            Jwt jwt = Jwt.withTokenValue(c.name()).header("alg", "none").subject(authId.toString())
                    .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(300)).build();
            when(jwtDecoder.decode(eq(c.name()))).thenReturn(jwt);

            if (c == Chamador.SEM_VINCULO) {
                when(pessoaRepository.findByAuthUserId(authId)).thenReturn(Optional.empty());
                continue;
            }
            Pessoa pessoa = new Pessoa(c.name(), mock(Area.class));
            pessoa.setPerfil(PerfilPessoa.valueOf(c.name()));
            pessoa.setAprovadoEm(OffsetDateTime.now());
            when(pessoaRepository.findByAuthUserId(authId)).thenReturn(Optional.of(pessoa));
        }
    }

    static List<Caso> casos() {
        List<Caso> c = new ArrayList<>();
        HttpMethod G = HttpMethod.GET, P = HttpMethod.POST, U = HttpMethod.PUT, D = HttpMethod.DELETE,
                A = HttpMethod.PATCH;

        // leitura: todo vinculado (inclusive Visualizador)
        for (String caminho : List.of("/demandas", "/demandas/" + ID, "/atividades", "/pecas", "/evidencias",
                "/projetos", "/campanhas", "/campanhas/resumo", "/campanhas/" + ID + "/resumo", "/demandantes", "/tipos-peca", "/tipos-atividade", "/diretorias", "/nucleos",
                "/pessoas", "/dashboard", "/relatorios/2026-10", "/demandas/" + ID + "/participantes")) {
            c.add(new Caso(G, caminho, TODOS_VINCULADOS));
        }
        // leitura aberta a conta ainda sem vínculo (tela de completar cadastro) e o próprio perfil
        c.add(new Caso(G, "/areas", QUALQUER_LOGADO));
        c.add(new Caso(G, "/cargos", QUALQUER_LOGADO));
        c.add(new Caso(G, "/pessoas/me", QUALQUER_LOGADO));
        c.add(new Caso(P, "/pessoas/auto-cadastro", QUALQUER_LOGADO));
        c.add(new Caso(U, "/pessoas/me", TODOS_VINCULADOS));

        // escrita do trabalho do dia a dia: Visualizador é somente leitura
        for (Caso base : List.of(
                new Caso(P, "/demandas", QUEM_ESCREVE),
                new Caso(U, "/demandas/" + ID, QUEM_ESCREVE),
                new Caso(D, "/demandas/" + ID, QUEM_ESCREVE),
                new Caso(A, "/demandas/" + ID + "/status", QUEM_ESCREVE),
                new Caso(P, "/demandas/" + ID + "/participantes", QUEM_ESCREVE),
                new Caso(A, "/demandas/" + ID + "/participantes/" + ID, QUEM_ESCREVE),
                new Caso(D, "/demandas/" + ID + "/participantes/" + ID, QUEM_ESCREVE),
                new Caso(P, "/atividades", QUEM_ESCREVE),
                new Caso(U, "/atividades/" + ID, QUEM_ESCREVE),
                new Caso(D, "/atividades/" + ID, QUEM_ESCREVE),
                new Caso(P, "/pecas", QUEM_ESCREVE),
                new Caso(U, "/pecas/" + ID, QUEM_ESCREVE),
                new Caso(D, "/pecas/" + ID, QUEM_ESCREVE),
                new Caso(P, "/evidencias", QUEM_ESCREVE),
                new Caso(P, "/evidencias/arquivo", QUEM_ESCREVE),
                new Caso(D, "/evidencias/" + ID, QUEM_ESCREVE),
                new Caso(U, "/relatorios/2026-10", QUEM_ESCREVE),
                new Caso(P, "/relatorios/2026-10/aprovar", QUEM_ESCREVE),
                // Referência de equipe é um Profissional: o serviço decide por área
                new Caso(P, "/tipos-peca", QUEM_ESCREVE),
                new Caso(U, "/tipos-peca/" + ID, QUEM_ESCREVE),
                new Caso(D, "/tipos-peca/" + ID, QUEM_ESCREVE),
                // campanha e projeto: a barreira HTTP libera quem escreve; o serviço só deixa o Profissional que é
                // Referência de equipe (PermissaoServiceProjetosECampanhasTest). Só a gestão exclui ou encerra
                new Caso(P, "/campanhas", QUEM_ESCREVE),
                new Caso(U, "/campanhas/" + ID, QUEM_ESCREVE),
                new Caso(D, "/campanhas/" + ID, GESTAO),
                new Caso(P, "/projetos", QUEM_ESCREVE),
                new Caso(U, "/projetos/" + ID, QUEM_ESCREVE),
                new Caso(D, "/projetos/" + ID, GESTAO),
                new Caso(P, "/demandantes", QUEM_ESCREVE),
                new Caso(U, "/demandantes/" + ID, QUEM_ESCREVE),
                new Caso(D, "/demandantes/" + ID, GESTAO))) {
            c.add(base);
        }

        // cadastros base: só Gestor e Admin escrevem
        for (String base : List.of("/diretorias", "/nucleos", "/areas", "/cargos", "/tipos-atividade")) {
            c.add(new Caso(P, base, GESTAO));
            c.add(new Caso(U, base + "/" + ID, GESTAO));
            c.add(new Caso(D, base + "/" + ID, GESTAO));
        }

        // a própria foto de perfil: todo vinculado, inclusive Visualizador (como o PUT /pessoas/me)
        c.add(new Caso(P, "/pessoas/me/foto", TODOS_VINCULADOS));
        c.add(new Caso(D, "/pessoas/me/foto", TODOS_VINCULADOS));

        // pessoas
        c.add(new Caso(P, "/pessoas", GESTAO));
        c.add(new Caso(U, "/pessoas/" + ID, GESTAO));
        c.add(new Caso(D, "/pessoas/" + ID, GESTAO));
        c.add(new Caso(A, "/pessoas/" + ID + "/aprovar", GESTAO));
        c.add(new Caso(A, "/pessoas/" + ID + "/rejeitar", GESTAO));
        c.add(new Caso(A, "/pessoas/" + ID + "/vincular-auth", SO_ADMIN));
        c.add(new Caso(P, "/pessoas/" + ID + "/vincular-a", GESTAO));
        c.add(new Caso(G, "/pessoas/" + ID + "/vinculo-sugerido", GESTAO));
        c.add(new Caso(P, "/pessoas/" + ID + "/confirmar-vinculo", GESTAO));

        // módulo de equipamentos: a barreira HTTP só trava o Visualizador nas escritas. Quem tem acesso ao módulo
        // (equipe liberada, papel, Gestor, Admin) e quem opera ou gerencia é decidido nos serviços
        // (EquipamentoPermissaoTest, EmprestimoServiceTest).
        for (String caminho : List.of("/equipamentos", "/equipamentos/" + ID, "/equipamentos/termo", "/equipamentos/agenda",
                "/equipamentos/disponibilidade?inicio=2026-10-20T10:00:00Z&fim=2026-10-21T10:00:00Z",
                "/categorias-equipamento", "/emprestimos", "/emprestimos/resumo", "/emprestimos/" + ID, "/emprestimos/" + ID + "/fotos")) {
            c.add(new Caso(G, caminho, TODOS_VINCULADOS));
        }
        for (Caso base : List.of(
                new Caso(P, "/equipamentos", QUEM_ESCREVE),
                new Caso(U, "/equipamentos/" + ID, QUEM_ESCREVE),
                new Caso(D, "/equipamentos/" + ID, QUEM_ESCREVE),
                new Caso(P, "/equipamentos/termo", QUEM_ESCREVE),
                new Caso(P, "/categorias-equipamento", QUEM_ESCREVE),
                new Caso(U, "/categorias-equipamento/" + ID, QUEM_ESCREVE),
                new Caso(D, "/categorias-equipamento/" + ID, QUEM_ESCREVE),
                new Caso(P, "/emprestimos", QUEM_ESCREVE),
                new Caso(U, "/emprestimos/" + ID, QUEM_ESCREVE),
                new Caso(A, "/emprestimos/" + ID + "/prorrogar", QUEM_ESCREVE),
                new Caso(P, "/emprestimos/" + ID + "/fotos", QUEM_ESCREVE),
                new Caso(D, "/emprestimos/" + ID + "/fotos/" + ID, QUEM_ESCREVE),
                new Caso(A, "/emprestimos/" + ID + "/autorizar", QUEM_ESCREVE),
                new Caso(A, "/emprestimos/" + ID + "/recusar", QUEM_ESCREVE),
                new Caso(A, "/emprestimos/" + ID + "/retirar", QUEM_ESCREVE),
                new Caso(A, "/emprestimos/" + ID + "/devolver", QUEM_ESCREVE),
                new Caso(A, "/emprestimos/" + ID + "/cancelar", QUEM_ESCREVE))) {
            c.add(base);
        }
        // quem recebe um papel no módulo é decisão da gestão
        c.add(new Caso(A, "/pessoas/" + ID + "/papel-equipamentos", GESTAO));

        // relatório de outra pessoa e reabertura são decididos no serviço; aqui só a trava de Visualizador
        c.add(new Caso(P, "/relatorios/2026-10/reabrir", QUEM_ESCREVE));
        return c;
    }

    @TestFactory
    List<DynamicTest> cadaChamadorRecebeARespostaEsperada() {
        List<DynamicTest> testes = new ArrayList<>();
        for (Caso caso : casos()) {
            for (Chamador chamador : Chamador.values()) {
                boolean deveLiberar = chamador != Chamador.ANONIMO && caso.passam().contains(chamador);
                String nome = chamador + " " + caso.metodo() + " " + caso.caminho()
                        + (deveLiberar ? " passa" : (chamador == Chamador.ANONIMO ? " 401" : " 403"));
                testes.add(DynamicTest.dynamicTest(nome, () -> {
                    int status = chamar(chamador, caso);
                    if (chamador == Chamador.ANONIMO) {
                        assertEquals(401, status, nome);
                    } else if (deveLiberar) {
                        assertEquals(false, status == 401 || status == 403, nome + " (status " + status + ")");
                    } else {
                        assertEquals(403, status, nome);
                    }
                }));
            }
        }
        return testes;
    }

    private int chamar(Chamador chamador, Caso caso) throws Exception {
        MockHttpServletRequestBuilder req = MockMvcRequestBuilders.request(caso.metodo(), caso.caminho())
                .contentType("application/json").content("{}");
        if (chamador != Chamador.ANONIMO) {
            req.header("Authorization", "Bearer " + chamador.name());
        }
        try {
            return mvc.perform(req).andReturn().getResponse().getStatus();
        } catch (ServletException | RuntimeException e) {
            // a segurança liberou e o controller falhou depois (serviço é mock, devolve nulo): passou
            return 500;
        }
    }
}
