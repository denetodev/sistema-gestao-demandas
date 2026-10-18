package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.response.NotificacaoResponse;
import dev.denetodev.sgd_api.dto.response.NotificacaoResponse.Item;
import dev.denetodev.sgd_api.dto.response.NotificacaoResponse.Tipo;
import dev.denetodev.sgd_api.dto.response.ResumoEmprestimosResponse;
import dev.denetodev.sgd_api.entity.*;
import dev.denetodev.sgd_api.repository.PessoaDemandaRepository;
import dev.denetodev.sgd_api.repository.PessoaRepository;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** O sininho: o que cada pessoa vê conforme o que ela faz no sistema. */
class NotificacaoServiceTest {

    private static final OffsetDateTime AGORA = OffsetDateTime.of(2026, 10, 18, 12, 0, 0, 0, ZoneOffset.UTC);

    private PessoaRepository pessoaRepository;
    private PessoaDemandaRepository pessoaDemandaRepository;
    private EmprestimoService emprestimoService;
    private CurrentPessoaResolver resolver;
    private NotificacaoService service;
    private final Jwt jwt = mock(Jwt.class);
    private Area area;

    @BeforeEach
    void preparar() {
        pessoaRepository = mock(PessoaRepository.class);
        pessoaDemandaRepository = mock(PessoaDemandaRepository.class);
        emprestimoService = mock(EmprestimoService.class);
        resolver = mock(CurrentPessoaResolver.class);
        service = new NotificacaoService(resolver, pessoaRepository, pessoaDemandaRepository, emprestimoService,
                Clock.fixed(Instant.parse("2026-10-18T12:00:00Z"), ZoneOffset.UTC));
        area = mock(Area.class);
        when(pessoaRepository.findByAprovadoEmIsNullAndStatus(StatusPessoa.ATIVO)).thenReturn(List.of());
        when(emprestimoService.contar()).thenReturn(new ResumoEmprestimosResponse(0, 0, 0));
        when(pessoaDemandaRepository.findTop20ByPessoaIdAndAdicionadoEmAfterAndDataSaidaIsNullOrderByAdicionadoEmDesc(any(), any()))
                .thenReturn(List.of());
    }

    private Pessoa pessoa(String nome, PerfilPessoa perfil, PapelEquipamentos papel) {
        Pessoa p = new Pessoa(nome, area);
        ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
        p.setPerfil(perfil);
        p.setPapelEquipamentos(papel);
        return p;
    }

    private PessoaDemanda adicao(Pessoa alvo, Pessoa por, String titulo, OffsetDateTime quando) {
        Demanda demanda = mock(Demanda.class);
        when(demanda.getId()).thenReturn(UUID.randomUUID());
        when(demanda.getTitulo()).thenReturn(titulo);
        PessoaDemanda v = new PessoaDemanda(alvo, demanda);
        ReflectionTestUtils.setField(v, "id", UUID.randomUUID());
        v.registrarAdicao(por, quando);
        return v;
    }

    private void adicoesDe(Pessoa alvo, PessoaDemanda... vinculos) {
        when(pessoaDemandaRepository.findTop20ByPessoaIdAndAdicionadoEmAfterAndDataSaidaIsNullOrderByAdicionadoEmDesc(eq(alvo.getId()), any()))
                .thenReturn(List.of(vinculos));
    }

    private static <T> T eq(T valor) {
        return org.mockito.ArgumentMatchers.eq(valor);
    }

    @Test
    void profissionalSemNadaPendenteTemSininhoVazio() {
        Pessoa fulano = pessoa("Fulano", PerfilPessoa.PROFISSIONAL, null);

        NotificacaoResponse r = service.listar(fulano);

        assertTrue(r.itens().isEmpty());
        assertEquals(0, r.naoLidas());
    }

    @Test
    void gestorEAdminVeemQuantosCadastrosEsperamAprovacao() {
        when(pessoaRepository.findByAprovadoEmIsNullAndStatus(StatusPessoa.ATIVO))
                .thenReturn(List.of(mock(Pessoa.class), mock(Pessoa.class), mock(Pessoa.class)));

        for (PerfilPessoa perfil : List.of(PerfilPessoa.GESTOR, PerfilPessoa.ADMIN)) {
            NotificacaoResponse r = service.listar(pessoa("Chefe", perfil, null));
            assertEquals(1, r.itens().size());
            assertEquals(Tipo.CADASTROS_PENDENTES, r.itens().get(0).tipo());
            assertEquals(3, r.naoLidas());
            assertEquals("/pessoas", r.itens().get(0).link());
        }
        // Profissional comum nunca vê isso
        assertTrue(service.listar(pessoa("Fulano", PerfilPessoa.PROFISSIONAL, null)).itens().isEmpty());
    }

    @Test
    void quemAutorizaEmprestimoVeOsPedidosEquemOperaVeOsAtrasados() {
        when(emprestimoService.contar()).thenReturn(new ResumoEmprestimosResponse(2, 1, 3));

        NotificacaoResponse gestorDeEquipamentos = service.listar(pessoa("G", PerfilPessoa.PROFISSIONAL, PapelEquipamentos.GESTOR_DE_EQUIPAMENTOS));
        assertEquals(List.of(Tipo.EMPRESTIMOS_AGUARDANDO, Tipo.EMPRESTIMOS_ATRASADOS),
                gestorDeEquipamentos.itens().stream().map(Item::tipo).toList());
        assertEquals(5, gestorDeEquipamentos.naoLidas());
        assertTrue(gestorDeEquipamentos.itens().get(0).texto().contains("1 para fora do prédio"));

        // o Operador só opera: vê os atrasados, não os pedidos para autorizar
        NotificacaoResponse operador = service.listar(pessoa("O", PerfilPessoa.PROFISSIONAL, PapelEquipamentos.OPERADOR_DE_ESTUDIO));
        assertEquals(List.of(Tipo.EMPRESTIMOS_ATRASADOS), operador.itens().stream().map(Item::tipo).toList());
        assertEquals(3, operador.naoLidas());

        // sem papel no módulo, nada
        assertTrue(service.listar(pessoa("P", PerfilPessoa.PROFISSIONAL, null)).itens().isEmpty());
        assertTrue(service.listar(pessoa("V", PerfilPessoa.VISUALIZADOR, null)).itens().isEmpty());
    }

    @Test
    void aviso_deQuemTeAdicionouNumaDemandaAparecesoQuandoFoiOutraPessoa() {
        Pessoa fulano = pessoa("Fulano", PerfilPessoa.PROFISSIONAL, null);
        Pessoa ana = pessoa("Ana", PerfilPessoa.GESTOR, null);
        adicoesDe(fulano,
                adicao(fulano, ana, "Campanha Plano Safra", AGORA.minusHours(1)),
                adicao(fulano, fulano, "Que eu mesmo criei", AGORA.minusHours(2)),
                adicao(fulano, null, "Importada", AGORA.minusHours(3)));

        NotificacaoResponse r = service.listar(fulano);

        assertEquals(1, r.itens().size());
        Item item = r.itens().get(0);
        assertEquals(Tipo.ADICIONADO_EM_DEMANDA, item.tipo());
        assertEquals("Ana te adicionou em Campanha Plano Safra.", item.texto());
        assertTrue(item.link().startsWith("/demandas/"));
        assertFalse(item.lida());
        assertEquals(1, r.naoLidas());
    }

    @Test
    void depoisDeVerOsAvisosDeAdicaoViramLidosMasPendenciasContinuam() {
        Pessoa gestor = pessoa("Gestora", PerfilPessoa.GESTOR, null);
        Pessoa ana = pessoa("Ana", PerfilPessoa.PROFISSIONAL, null);
        when(pessoaRepository.findByAprovadoEmIsNullAndStatus(StatusPessoa.ATIVO)).thenReturn(List.of(mock(Pessoa.class)));
        adicoesDe(gestor,
                adicao(gestor, ana, "Antiga", AGORA.minusHours(5)),
                adicao(gestor, ana, "Nova", AGORA.minusMinutes(5)));

        gestor.setNotificacoesVistasEm(AGORA.minusHours(1));
        NotificacaoResponse r = service.listar(gestor);

        assertEquals(List.of(false, true, false), r.itens().stream().map(Item::lida).toList()); // cadastros, antiga (lida), nova
        assertEquals(2, r.naoLidas()); // 1 cadastro + a adição nova
    }

    @Test
    void marcarVistasGravaOMomentoAtual() {
        Pessoa fulano = pessoa("Fulano", PerfilPessoa.PROFISSIONAL, null);
        when(resolver.resolver(jwt)).thenReturn(fulano);

        service.marcarVistas(jwt);

        assertEquals(AGORA, fulano.getNotificacoesVistasEm());
    }
}
