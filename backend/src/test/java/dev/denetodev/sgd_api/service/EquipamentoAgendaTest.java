package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.response.AgendaResponse;
import dev.denetodev.sgd_api.dto.response.DisponibilidadeResponse;
import dev.denetodev.sgd_api.dto.response.EquipamentoResponse;
import dev.denetodev.sgd_api.entity.*;
import dev.denetodev.sgd_api.repository.*;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
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
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Agenda (Gantt) e disponibilidade por período do inventário. */
class EquipamentoAgendaTest {

    private static final Instant INSTANTE = Instant.parse("2026-10-20T12:00:00Z");
    private static final OffsetDateTime AGORA = OffsetDateTime.ofInstant(INSTANTE, ZoneOffset.UTC);

    private EquipamentoRepository equipamentoRepository;
    private EmprestimoItemRepository itemRepository;
    private CurrentPessoaResolver resolver;
    private EquipamentoService service;
    private final Jwt jwt = mock(Jwt.class);

    private Pessoa operador;
    private Pessoa consulta;
    private Equipamento camera;
    private Equipamento cabos;

    @BeforeEach
    void preparar() {
        equipamentoRepository = mock(EquipamentoRepository.class);
        itemRepository = mock(EmprestimoItemRepository.class);
        resolver = mock(CurrentPessoaResolver.class);
        service = new EquipamentoService(mock(CategoriaEquipamentoRepository.class), equipamentoRepository, itemRepository,
                mock(TermoEquipamentoRepository.class), resolver, Clock.fixed(INSTANTE, ZoneOffset.UTC));

        operador = pessoa("Operador", PapelEquipamentos.OPERADOR_DE_ESTUDIO, false);
        consulta = pessoa("Consulta", null, true);
        camera = equipamento("Câmera A7", ControleEquipamento.INDIVIDUAL, 1);
        cabos = equipamento("Cabo HDMI", ControleEquipamento.QUANTIDADE, 10);
        when(equipamentoRepository.findAllByOrderByNomeAsc()).thenReturn(List.of(cabos, camera));
        when(itemRepository.ocupacao(anyCollection(), anyCollection())).thenReturn(List.of());
        when(itemRepository.devolvidosDesde(any())).thenReturn(List.of());
        agir(operador);
    }

    private Pessoa pessoa(String nome, PapelEquipamentos papel, boolean equipeLiberada) {
        Area area = new Area(null, "Equipe " + nome);
        Pessoa p = new Pessoa(nome, area);
        ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
        p.setPerfil(PerfilPessoa.PROFISSIONAL);
        p.setPapelEquipamentos(papel);
        return p;
    }

    private Equipamento equipamento(String nome, ControleEquipamento controle, int total) {
        Equipamento e = new Equipamento(new CategoriaEquipamento("Cat " + nome, controle), nome);
        ReflectionTestUtils.setField(e, "id", UUID.randomUUID());
        e.setQuantidadeTotal(total);
        return e;
    }

    private void agir(Pessoa quem) {
        when(resolver.resolver(any())).thenReturn(quem);
    }

    private EmprestimoItem uso(Equipamento e, String quem, long deDias, long ateDias, StatusEmprestimo status, int quantidade) {
        Emprestimo emp = new Emprestimo(quem, AGORA.plusDays(ateDias), operador, 1);
        ReflectionTestUtils.setField(emp, "id", UUID.randomUUID());
        emp.setRetiradaPrevista(AGORA.plusDays(deDias));
        emp.setStatus(status);
        if (status == StatusEmprestimo.RETIRADO || status == StatusEmprestimo.DEVOLVIDO) {
            emp.setRetiradoEm(AGORA.plusDays(deDias));
        }
        return new EmprestimoItem(emp, e, quantidade);
    }

    private void abertos(EmprestimoItem... itens) {
        when(itemRepository.ocupacao(anyCollection(), eq(EquipamentoService.OCUPAM))).thenReturn(List.of(itens));
    }

    private AgendaResponse.Linha linha(AgendaResponse r, Equipamento e) {
        return r.equipamentos().stream().filter(l -> l.equipamentoId().equals(e.getId())).findFirst().orElseThrow();
    }

    // ---------- agenda ----------

    @Test
    void quemOperaVeReservasEmprestimosEDevolucoesRecentes() {
        EmprestimoItem reserva = uso(camera, "Maria", 3, 4, StatusEmprestimo.AUTORIZADO, 1);
        EmprestimoItem comAlguem = uso(cabos, "João", -1, 1, StatusEmprestimo.RETIRADO, 2);
        EmprestimoItem devolvido = uso(camera, "Pedro", -4, -2, StatusEmprestimo.DEVOLVIDO, 1);
        devolvido.setDevolvidoEm(AGORA.minusDays(2));
        abertos(reserva, comAlguem);
        when(itemRepository.devolvidosDesde(any())).thenReturn(List.of(devolvido));

        AgendaResponse r = service.agenda(jwt, AGORA.minusDays(7), AGORA.plusDays(14));

        assertEquals(2, r.equipamentos().size());
        assertEquals(List.of("Pedro", "Maria"), linha(r, camera).blocos().stream().map(AgendaResponse.Bloco::retiranteNome).toList());
        assertTrue(linha(r, camera).blocos().get(0).devolvido());
        assertEquals(2, linha(r, cabos).blocos().get(0).quantidade());
    }


    @Test
    void profissionalSemPapelNaoVeAAgendaNemOInventario() {
        agir(consulta);
        assertThrows(AccessDeniedException.class, () -> service.agenda(jwt, null, null));
        assertThrows(AccessDeniedException.class, () -> service.listar(jwt, null, null));
    }

    @Test
    void semAcessoNaoVeAAgenda() {
        agir(pessoa("Sem acesso", null, false));
        assertThrows(AccessDeniedException.class, () -> service.agenda(jwt, null, null));
    }

    @Test
    void atrasadoContinuaOcupandoAteAgoraEFicaMarcado() {
        EmprestimoItem atrasado = uso(camera, "João", -5, -1, StatusEmprestimo.RETIRADO, 1);
        abertos(atrasado);

        AgendaResponse.Bloco bloco = linha(service.agenda(jwt, AGORA.minusDays(2), AGORA.plusDays(3)), camera).blocos().get(0);

        assertTrue(bloco.atrasado());
        assertEquals(AGORA.minusDays(1), bloco.fim()); // o fim continua sendo a devolução prevista
    }

    @Test
    void blocosForaDaJanelaNaoAparecem() {
        abertos(uso(camera, "Futuro", 30, 31, StatusEmprestimo.AUTORIZADO, 1), uso(camera, "Dentro", 1, 2, StatusEmprestimo.AUTORIZADO, 1));

        AgendaResponse r = service.agenda(jwt, AGORA, AGORA.plusDays(10));

        assertEquals(List.of("Dentro"), linha(r, camera).blocos().stream().map(AgendaResponse.Bloco::retiranteNome).toList());
    }

    @Test
    void janelaInvalidaOuGrandeDemaisERecusada() {
        assertThrows(IllegalArgumentException.class, () -> service.agenda(jwt, AGORA, AGORA.minusDays(1)));
        assertThrows(IllegalArgumentException.class, () -> service.agenda(jwt, AGORA, AGORA.plusDays(121)));
    }

    // ---------- disponibilidade por período ----------

    private int livre(List<DisponibilidadeResponse> lista, Equipamento e) {
        return lista.stream().filter(d -> d.equipamentoId().equals(e.getId())).findFirst().orElseThrow().quantidadeDisponivel();
    }

    @Test
    void disponibilidadeValeParaOPeriodoTodo() {
        abertos(uso(camera, "Maria", 3, 4, StatusEmprestimo.AUTORIZADO, 1), uso(cabos, "João", 0, 1, StatusEmprestimo.AUTORIZADO, 7));

        List<DisponibilidadeResponse> semConflito = service.disponibilidade(jwt, AGORA.plusDays(5), AGORA.plusDays(6), null);
        assertEquals(1, livre(semConflito, camera));
        assertEquals(10, livre(semConflito, cabos));

        List<DisponibilidadeResponse> comConflito = service.disponibilidade(jwt, AGORA, AGORA.plusDays(4), null);
        assertEquals(0, livre(comConflito, camera));
        assertEquals(3, livre(comConflito, cabos));
    }

    @Test
    void disponibilidadeIgnoraOEmprestimoEmCorrecao() {
        EmprestimoItem meu = uso(camera, "Eu", 0, 2, StatusEmprestimo.AGUARDANDO_AUTORIZACAO, 1);
        abertos(meu);

        assertEquals(0, livre(service.disponibilidade(jwt, AGORA, AGORA.plusDays(1), null), camera));
        assertEquals(1, livre(service.disponibilidade(jwt, AGORA, AGORA.plusDays(1), meu.getEmprestimo().getId()), camera));
    }

    @Test
    void soQuemOperaConsultaDisponibilidadeEOPeriodoPrecisaSerValido() {
        agir(consulta);
        assertThrows(AccessDeniedException.class, () -> service.disponibilidade(jwt, AGORA, AGORA.plusDays(1), null));

        agir(operador);
        assertThrows(IllegalArgumentException.class, () -> service.disponibilidade(jwt, AGORA, AGORA, null));
        assertThrows(IllegalArgumentException.class, () -> service.disponibilidade(jwt, null, AGORA, null));
    }

    // ---------- "disponível agora" no inventário ----------

    @Test
    void reservaFuturaNaoTiraOEquipamentoDoEstudioHoje() {
        abertos(uso(camera, "Maria", 3, 4, StatusEmprestimo.AUTORIZADO, 1));

        EquipamentoResponse r = service.listar(jwt, null, null).stream().filter(e -> e.id().equals(camera.getId())).findFirst().orElseThrow();

        assertEquals(1, r.quantidadeDisponivel());
        assertEquals(1, r.emUso().size()); // mas a reserva aparece em "com quem está"
    }

    @Test
    void emprestimoEmCursoTiraOEquipamentoDoEstudio() {
        abertos(uso(camera, "João", -1, 1, StatusEmprestimo.RETIRADO, 1), uso(cabos, "João", -1, 1, StatusEmprestimo.RETIRADO, 4));

        List<EquipamentoResponse> lista = service.listar(jwt, null, null);

        assertEquals(0, lista.stream().filter(e -> e.id().equals(camera.getId())).findFirst().orElseThrow().quantidadeDisponivel());
        assertEquals(6, lista.stream().filter(e -> e.id().equals(cabos.getId())).findFirst().orElseThrow().quantidadeDisponivel());
    }
}
