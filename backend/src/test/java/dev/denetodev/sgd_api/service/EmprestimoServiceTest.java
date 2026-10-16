package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.DevolucaoRequest;
import dev.denetodev.sgd_api.dto.request.EmprestimoEdicaoRequest;
import dev.denetodev.sgd_api.dto.request.EmprestimoRequest;
import dev.denetodev.sgd_api.dto.request.ProrrogacaoRequest;
import dev.denetodev.sgd_api.dto.response.ResumoEmprestimosResponse;
import dev.denetodev.sgd_api.dto.request.RecusarEmprestimoRequest;
import dev.denetodev.sgd_api.dto.response.EmprestimoResponse;
import dev.denetodev.sgd_api.entity.*;
import dev.denetodev.sgd_api.exception.EstadoInvalidoException;
import dev.denetodev.sgd_api.repository.*;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.*;

class EmprestimoServiceTest {

    private static final Instant AGORA = Instant.parse("2026-10-20T12:00:00Z");

    private EmprestimoRepository emprestimoRepository;
    private EmprestimoItemRepository itemRepository;
    private EquipamentoRepository equipamentoRepository;
    private AuditoriaRepository auditoriaRepository;
    private CurrentPessoaResolver resolver;
    private EmprestimoService service;
    private final Jwt jwt = mock(Jwt.class);

    private Pessoa operador;
    private Pessoa gestorEquip;
    private Pessoa admin;
    private Pessoa consulta;
    private Equipamento camera;   // individual
    private Equipamento cabos;    // lote de 10

    private final Map<UUID, Emprestimo> banco = new HashMap<>();

    @BeforeEach
    void preparar() {
        emprestimoRepository = mock(EmprestimoRepository.class);
        itemRepository = mock(EmprestimoItemRepository.class);
        equipamentoRepository = mock(EquipamentoRepository.class);
        auditoriaRepository = mock(AuditoriaRepository.class);
        resolver = mock(CurrentPessoaResolver.class);
        Clock relogio = Clock.fixed(AGORA, ZoneOffset.UTC);

        TermoEquipamentoRepository termoRepository = mock(TermoEquipamentoRepository.class);
        when(termoRepository.findTopByOrderByVersaoDesc())
                .thenReturn(Optional.of(new TermoEquipamento(3, "Termo curto.", null)));
        EquipamentoService equipamentoService = new EquipamentoService(
                mock(CategoriaEquipamentoRepository.class), equipamentoRepository, itemRepository,
                termoRepository, resolver, relogio);

        service = new EmprestimoService(emprestimoRepository, itemRepository, equipamentoRepository,
                equipamentoService, mock(PessoaRepository.class), mock(DemandaRepository.class),
                mock(AtividadeRepository.class), auditoriaRepository, resolver, relogio);

        operador = pessoa("Operador", PerfilPessoa.PROFISSIONAL, PapelEquipamentos.OPERADOR_DE_ESTUDIO);
        gestorEquip = pessoa("Gestor equip", PerfilPessoa.PROFISSIONAL, PapelEquipamentos.GESTOR_DE_EQUIPAMENTOS);
        admin = pessoa("Admin", PerfilPessoa.ADMIN, null);
        consulta = pessoa("Consulta", PerfilPessoa.PROFISSIONAL, null);
        ((Area) ReflectionTestUtils.getField(consulta, "area")).setAcessaEquipamentos(true);

        camera = equipamento("Câmera A7", ControleEquipamento.INDIVIDUAL, 1);
        cabos = equipamento("Cabo HDMI", ControleEquipamento.QUANTIDADE, 10);

        when(equipamentoRepository.travarPorIds(anyCollection())).thenAnswer(inv -> {
            Collection<UUID> ids = inv.getArgument(0);
            return List.of(camera, cabos).stream().filter(e -> ids.contains(e.getId())).toList();
        });
        when(itemRepository.ocupacao(anyCollection(), anyCollection())).thenReturn(List.of());
        when(emprestimoRepository.save(any(Emprestimo.class))).thenAnswer(inv -> {
            Emprestimo e = inv.getArgument(0);
            if (e.getId() == null) {
                ReflectionTestUtils.setField(e, "id", UUID.randomUUID());
            }
            banco.put(e.getId(), e);
            return e;
        });
        when(emprestimoRepository.findById(any(UUID.class))).thenAnswer(inv -> Optional.ofNullable(banco.get(inv.getArgument(0))));
        agir(operador);
    }

    private Pessoa pessoa(String nome, PerfilPessoa perfil, PapelEquipamentos papel) {
        Pessoa p = new Pessoa(nome, new Area(null, "Equipe " + nome));
        ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
        p.setPerfil(perfil);
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

    private EmprestimoRequest pedido(boolean externo, EmprestimoRequest.Item... itens) {
        return new EmprestimoRequest("João da Silva", "f1234567", "ramal 123", null, "Gravação", externo, "Praça", null,
                OffsetDateTime.now(Clock.fixed(AGORA, ZoneOffset.UTC)).plusDays(2), null, null, List.of(itens), true);
    }

    private EmprestimoRequest.Item item(Equipamento e, Integer qtd) {
        return new EmprestimoRequest.Item(e.getId(), qtd);
    }

    private Emprestimo registrarComoOperador(boolean externo) {
        agir(operador);
        EmprestimoResponse r = service.registrar(jwt, pedido(externo, item(camera, null)));
        return banco.get(r.id());
    }

    // ---------- registrar ----------

    @Test
    void registraAguardandoAutorizacaoComTermoChaveNormalizadaEAuditoria() {
        EmprestimoResponse r = service.registrar(jwt, pedido(true, item(camera, null), item(cabos, 3)));

        assertEquals(StatusEmprestimo.AGUARDANDO_AUTORIZACAO, r.status());
        assertTrue(r.externo());
        assertEquals(3, r.termoVersao());
        assertEquals("F1234567", r.retiranteChaveC());
        assertEquals(operador.getId(), r.registradoPorId());
        assertEquals(2, r.itens().size());
        verify(auditoriaRepository).save(argThat((Auditoria a) -> "status".equals(a.getCampo())
                && "AGUARDANDO_AUTORIZACAO".equals(a.getValorNovo()) && a.getValorAnterior() == null));
    }

    @Test
    void semOOkDoTermoNaoRegistra() {
        EmprestimoRequest r = pedido(false, item(camera, null));
        EmprestimoRequest semTermo = new EmprestimoRequest(r.retiranteNome(), null, null, null, null, false, null, null,
                r.previstaDevolucao(), null, null, r.itens(), false);
        assertThrows(EstadoInvalidoException.class, () -> service.registrar(jwt, semTermo));
        verify(emprestimoRepository, never()).save(any());
    }

    @Test
    void devolucaoPrevistaPrecisaSerFutura() {
        EmprestimoRequest r = pedido(false, item(camera, null));
        EmprestimoRequest ontem = new EmprestimoRequest(r.retiranteNome(), null, null, null, null, false, null, null,
                OffsetDateTime.now(Clock.fixed(AGORA, ZoneOffset.UTC)).minusHours(1), null, null, r.itens(), true);
        assertThrows(IllegalArgumentException.class, () -> service.registrar(jwt, ontem));
    }

    @Test
    void chaveCInvalidaDeQuemRetiraERecusada() {
        EmprestimoRequest r = pedido(false, item(camera, null));
        EmprestimoRequest ruim = new EmprestimoRequest(r.retiranteNome(), "X123", null, null, null, false, null, null,
                r.previstaDevolucao(), null, null, r.itens(), true);
        assertThrows(IllegalArgumentException.class, () -> service.registrar(jwt, ruim));
    }

    /** Outro empréstimo, já gravado, ocupando o equipamento de {@code de} a {@code ate} (em dias a partir de AGORA). */
    private EmprestimoItem outroUso(Equipamento e, int quantidade, long deDias, long ateDias, StatusEmprestimo status) {
        OffsetDateTime ate = agora().plusDays(ateDias);
        Emprestimo outro = new Emprestimo("Outro", ate, operador, 1);
        ReflectionTestUtils.setField(outro, "id", UUID.randomUUID());
        outro.setRetiradaPrevista(agora().plusDays(deDias));
        outro.setStatus(status);
        return new EmprestimoItem(outro, e, quantidade);
    }

    private void ocupar(EmprestimoItem... itens) {
        when(itemRepository.ocupacao(anyCollection(), eq(EquipamentoService.OCUPAM))).thenReturn(List.of(itens));
    }

    private EmprestimoRequest pedidoNoPeriodo(long retiradaDias, long devolucaoDias, EmprestimoRequest.Item... itens) {
        return new EmprestimoRequest("João da Silva", null, null, null, null, false, null,
                agora().plusDays(retiradaDias), agora().plusDays(devolucaoDias), null, null, List.of(itens), true);
    }

    @Test
    void equipamentoIndividualJaReservadoNoPeriodoNaoPodeSerPedidoDeNovo() {
        ocupar(outroUso(camera, 1, 0, 3, StatusEmprestimo.AUTORIZADO));

        EstadoInvalidoException e = assertThrows(EstadoInvalidoException.class,
                () -> service.registrar(jwt, pedidoNoPeriodo(0, 2, item(camera, null))));
        assertTrue(e.getMessage().contains("Câmera A7"));
        assertTrue(e.getMessage().contains("Outro"));
    }

    @Test
    void reservaParaOutroPeriodoPodeSerFeitaMesmoComOEquipamentoOcupadoAgora() {
        ocupar(outroUso(camera, 1, -1, 1, StatusEmprestimo.RETIRADO)); // está com alguém até amanhã

        EmprestimoResponse r = service.registrar(jwt, pedidoNoPeriodo(5, 6, item(camera, null)));

        assertEquals(StatusEmprestimo.AGUARDANDO_AUTORIZACAO, r.status());
        assertEquals(agora().plusDays(5), r.retiradaPrevista());
    }

    @Test
    void periodosQueApenasSeEncostamNaoConflitam() {
        ocupar(outroUso(camera, 1, 2, 3, StatusEmprestimo.AUTORIZADO)); // começa quando o novo termina
        assertDoesNotThrow(() -> service.registrar(jwt, pedidoNoPeriodo(0, 2, item(camera, null))));
    }

    @Test
    void retiradoEVencidoBloqueiaQualquerReservaFutura() {
        // devolução prevista foi há 1 dia e ainda não voltou: não tem fim
        ocupar(outroUso(camera, 1, -3, -1, StatusEmprestimo.RETIRADO));

        assertThrows(EstadoInvalidoException.class, () -> service.registrar(jwt, pedidoNoPeriodo(5, 6, item(camera, null))));
    }

    @Test
    void loteConfereOPicoNoPeriodo() {
        // 6 unidades hoje e outras 6 amanhã: nunca 12 ao mesmo tempo, o pico é 6 e sobram 4
        ocupar(outroUso(cabos, 6, 0, 1, StatusEmprestimo.AUTORIZADO), outroUso(cabos, 6, 1, 2, StatusEmprestimo.AUTORIZADO));

        assertThrows(EstadoInvalidoException.class, () -> service.registrar(jwt, pedidoNoPeriodo(0, 2, item(cabos, 5))));
        assertDoesNotThrow(() -> service.registrar(jwt, pedidoNoPeriodo(0, 2, item(cabos, 4))));
    }

    @Test
    void retiradaNoPassadoOuDevolucaoAntesDaRetiradaSaoRecusadas() {
        assertThrows(IllegalArgumentException.class, () -> service.registrar(jwt, pedidoNoPeriodo(-1, 2, item(camera, null))));
        assertThrows(IllegalArgumentException.class, () -> service.registrar(jwt, pedidoNoPeriodo(3, 2, item(camera, null))));
    }

    @Test
    void corrigirADataParaUmPeriodoOcupadoEhRecusadoMasAPropriaReservaNaoContaContraSi() {
        Emprestimo e = registrarComoOperador(false); // camera de agora a +2d
        // o próprio pedido aparece como ocupação; outra reserva da câmera começa em +5d
        ocupar(new EmprestimoItem(e, camera, 1), outroUso(camera, 1, 5, 6, StatusEmprestimo.AUTORIZADO));

        // estender para +4d continua livre, mesmo "encostando" na própria reserva
        assertDoesNotThrow(() -> service.editar(jwt, e.getId(), edicaoNoPeriodo(0, 4)));
        // estender para +6d invade a reserva de outra pessoa
        EstadoInvalidoException erro = assertThrows(EstadoInvalidoException.class, () -> service.editar(jwt, e.getId(), edicaoNoPeriodo(0, 6)));
        assertTrue(erro.getMessage().contains("Outro"));
    }

    private EmprestimoEdicaoRequest edicaoNoPeriodo(long retiradaDias, long devolucaoDias) {
        return new EmprestimoEdicaoRequest("João da Silva", null, null, null, false, null,
                agora().plusDays(retiradaDias), agora().plusDays(devolucaoDias), null);
    }

    @Test
    void naoProrrogaPorCimaDeUmaReservaDeOutraPessoa() {
        Emprestimo e = retiradoComDoisItens(); // devolução prevista em +2d
        ocupar(new EmprestimoItem(e, camera, 1), outroUso(camera, 1, 3, 4, StatusEmprestimo.AUTORIZADO));

        assertDoesNotThrow(() -> service.prorrogar(jwt, e.getId(), new ProrrogacaoRequest(agora().plusDays(3), null)));
        assertThrows(EstadoInvalidoException.class,
                () -> service.prorrogar(jwt, e.getId(), new ProrrogacaoRequest(agora().plusDays(3).plusHours(12), null)));
    }

    @Test
    void naoEntregaEquipamentoQueAindaEstaComOutraPessoa() {
        Emprestimo e = registrarComoOperador(false);
        agir(gestorEquip);
        service.autorizar(jwt, e.getId());
        agir(operador);
        // o empréstimo anterior da câmera está atrasado e não voltou
        EmprestimoItem comOutro = outroUso(camera, 1, -3, -1, StatusEmprestimo.RETIRADO);
        when(itemRepository.ocupacao(anyCollection(), eq(EnumSet.of(StatusEmprestimo.RETIRADO)))).thenReturn(List.of(comOutro));

        EstadoInvalidoException erro = assertThrows(EstadoInvalidoException.class, () -> service.retirar(jwt, e.getId()));
        assertTrue(erro.getMessage().contains("ainda está com Outro"));
        assertEquals(StatusEmprestimo.AUTORIZADO, e.getStatus());
    }

    @Test
    void entregaAdiantadaNaoPodeInvadirReservaDeOutraPessoa() {
        // reserva para daqui a 2 dias, mas a pessoa chegou hoje
        agir(operador);
        EmprestimoResponse r = service.registrar(jwt, pedidoNoPeriodo(2, 3, item(camera, null)));
        Emprestimo e = banco.get(r.id());
        agir(gestorEquip);
        service.autorizar(jwt, e.getId());
        agir(operador);
        // outra reserva da mesma câmera, de hoje até depois de amanhã
        when(itemRepository.ocupacao(anyCollection(), eq(EquipamentoService.OCUPAM)))
                .thenReturn(List.of(new EmprestimoItem(e, camera, 1), outroUso(camera, 1, 0, 2, StatusEmprestimo.AUTORIZADO)));

        EstadoInvalidoException erro = assertThrows(EstadoInvalidoException.class, () -> service.retirar(jwt, e.getId()));
        assertTrue(erro.getMessage().contains("A retirada está prevista para"));
    }

    @Test
    void entregaAdiantadaSemConflitoFunciona() {
        agir(operador);
        EmprestimoResponse r = service.registrar(jwt, pedidoNoPeriodo(2, 3, item(camera, null)));
        Emprestimo e = banco.get(r.id());
        agir(gestorEquip);
        service.autorizar(jwt, e.getId());
        agir(operador);

        EmprestimoResponse entregue = service.retirar(jwt, e.getId());

        assertEquals(StatusEmprestimo.RETIRADO, entregue.status());
        // adiantou: a ocupação passa a contar da entrega
        assertEquals(e.getRetiradoEm(), e.inicioOcupacao());
    }

    @Test
    void quemSoConsultaOuNaoTemAcessoNaoRegistra() {
        agir(consulta);
        assertThrows(AccessDeniedException.class, () -> service.registrar(jwt, pedido(false, item(camera, null))));
        agir(pessoa("Sem acesso", PerfilPessoa.PROFISSIONAL, null));
        assertThrows(AccessDeniedException.class, () -> service.registrar(jwt, pedido(false, item(camera, null))));
        agir(pessoa("Visualizador", PerfilPessoa.VISUALIZADOR, PapelEquipamentos.OPERADOR_DE_ESTUDIO));
        assertThrows(AccessDeniedException.class, () -> service.registrar(jwt, pedido(false, item(camera, null))));
    }

    // ---------- autorização ----------

    @Test
    void gestorDeEquipamentosAutorizaEFicaRegistradoQuemEQuando() {
        Emprestimo e = registrarComoOperador(true);
        agir(gestorEquip);

        EmprestimoResponse r = service.autorizar(jwt, e.getId());

        assertEquals(StatusEmprestimo.AUTORIZADO, r.status());
        assertEquals("Gestor equip", r.autorizadoPorNome());
        assertNotNull(r.autorizadoEm());
        verify(auditoriaRepository).save(argThat((Auditoria a) -> "status".equals(a.getCampo())
                && "AUTORIZADO".equals(a.getValorNovo()) && gestorEquip.getId().equals(a.getAlteradoPor())));
    }

    @Test
    void operadorNaoAutoriza() {
        Emprestimo e = registrarComoOperador(false);
        agir(operador);
        assertThrows(AccessDeniedException.class, () -> service.autorizar(jwt, e.getId()));
        assertThrows(AccessDeniedException.class, () -> service.recusar(jwt, e.getId(), new RecusarEmprestimoRequest("x")));
    }

    @Test
    void quemRegistrouNaoAutorizaOProprioPedidoExcetoAdmin() {
        agir(gestorEquip);
        EmprestimoResponse r = service.registrar(jwt, pedido(false, item(camera, null)));
        assertThrows(AccessDeniedException.class, () -> service.autorizar(jwt, r.id()));

        agir(admin);
        EmprestimoResponse doAdmin = service.registrar(jwt, pedido(false, item(cabos, 1)));
        assertEquals(StatusEmprestimo.AUTORIZADO, service.autorizar(jwt, doAdmin.id()).status());
    }

    @Test
    void recusarGravaOMotivoEEncerra() {
        Emprestimo e = registrarComoOperador(false);
        agir(gestorEquip);

        EmprestimoResponse r = service.recusar(jwt, e.getId(), new RecusarEmprestimoRequest("  Sem agenda  "));

        assertEquals(StatusEmprestimo.RECUSADO, r.status());
        assertEquals("Sem agenda", r.motivoRecusa());
        assertThrows(EstadoInvalidoException.class, () -> service.autorizar(jwt, e.getId()));
    }

    // ---------- entrega e devolução ----------

    @Test
    void soEntregaDepoisDeAutorizado() {
        Emprestimo e = registrarComoOperador(false);
        assertThrows(EstadoInvalidoException.class, () -> service.retirar(jwt, e.getId()));

        agir(gestorEquip);
        service.autorizar(jwt, e.getId());
        agir(operador);
        EmprestimoResponse r = service.retirar(jwt, e.getId());

        assertEquals(StatusEmprestimo.RETIRADO, r.status());
        assertEquals("Operador", r.retiradoPorNome());
    }

    private Emprestimo retiradoComDoisItens() {
        agir(operador);
        EmprestimoResponse r = service.registrar(jwt, pedido(false, item(camera, null), item(cabos, 2)));
        agir(gestorEquip);
        service.autorizar(jwt, r.id());
        agir(operador);
        service.retirar(jwt, r.id());
        return banco.get(r.id());
    }

    @Test
    void devolucaoTotalEncerraComQuemEQuando() {
        Emprestimo e = retiradoComDoisItens();

        EmprestimoResponse r = service.devolver(jwt, e.getId(), new DevolucaoRequest(null, "Tudo certo"));

        assertEquals(StatusEmprestimo.DEVOLVIDO, r.status());
        assertEquals("Operador", r.devolvidoPorNome());
        assertTrue(r.itens().stream().allMatch(i -> i.devolvidoEm() != null));
        assertEquals("Tudo certo", r.observacoesDevolucao());
    }

    @Test
    void devolucaoParcialDeixaOEmprestimoRetiradoComOQueFalta() {
        Emprestimo e = retiradoComDoisItens();
        ReflectionTestUtils.setField(e.getItens().get(0), "id", UUID.randomUUID());
        ReflectionTestUtils.setField(e.getItens().get(1), "id", UUID.randomUUID());
        UUID cabosId = e.getItens().stream().filter(i -> i.getEquipamento().equals(cabos)).findFirst().orElseThrow().getId();

        EmprestimoResponse r = service.devolver(jwt, e.getId(), new DevolucaoRequest(List.of(cabosId), null));

        assertEquals(StatusEmprestimo.RETIRADO, r.status());
        assertEquals(1, r.itens().stream().filter(i -> i.devolvidoEm() == null).count());
    }

    @Test
    void devolverItemQueNaoEDoEmprestimoEhRecusado() {
        Emprestimo e = retiradoComDoisItens();
        assertThrows(EstadoInvalidoException.class,
                () -> service.devolver(jwt, e.getId(), new DevolucaoRequest(List.of(UUID.randomUUID()), null)));
    }

    @Test
    void naoSeDevolveOQueAindaNaoFoiRetirado() {
        Emprestimo e = registrarComoOperador(false);
        assertThrows(EstadoInvalidoException.class, () -> service.devolver(jwt, e.getId(), null));
    }

    // ---------- cancelar ----------

    @Test
    void cancelaAntesDaEntregaMasNaoDepois() {
        Emprestimo aguardando = registrarComoOperador(false);
        assertEquals(StatusEmprestimo.CANCELADO, service.cancelar(jwt, aguardando.getId()).status());

        Emprestimo retirado = retiradoComDoisItens();
        assertThrows(EstadoInvalidoException.class, () -> service.cancelar(jwt, retirado.getId()));
    }

    @Test
    void quemSoConsultaNaoCancelaPedidoDeOutro() {
        Emprestimo e = registrarComoOperador(false);
        agir(consulta);
        assertThrows(AccessDeniedException.class, () -> service.cancelar(jwt, e.getId()));
    }

    // ---------- atraso e listagem ----------

    @Test
    void atrasadoEQuandoRetiradoEPassouDaDataPrevista() {
        Emprestimo e = retiradoComDoisItens();
        ReflectionTestUtils.setField(e, "previstaDevolucao", OffsetDateTime.now(Clock.fixed(AGORA, ZoneOffset.UTC)).minusHours(2));
        assertTrue(service.buscar(jwt, e.getId()).atrasado());

        Emprestimo aguardando = registrarComoOperador(false);
        ReflectionTestUtils.setField(aguardando, "previstaDevolucao", OffsetDateTime.now(Clock.fixed(AGORA, ZoneOffset.UTC)).minusHours(2));
        assertFalse(service.buscar(jwt, aguardando.getId()).atrasado()); // só conta atraso depois que saiu
    }

    @Test
    void quemSoConsultaVeApenasOQueEstaRetirado() {
        agir(consulta);
        when(emprestimoRepository.findByStatusIn(anyCollection())).thenReturn(List.of());

        service.listar(jwt, EmprestimoService.Grupo.TODOS);

        ArgumentCaptor<Collection<StatusEmprestimo>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(emprestimoRepository).findByStatusIn(captor.capture());
        assertEquals(Set.of(StatusEmprestimo.RETIRADO), new HashSet<>(captor.getValue()));
    }

    @Test
    void quemSoConsultaNaoAbreEmprestimoQueNaoEstaRetirado() {
        Emprestimo e = registrarComoOperador(false);
        agir(consulta);
        assertThrows(AccessDeniedException.class, () -> service.buscar(jwt, e.getId()));
    }

    @Test
    void filaMostraAguardandoExternoPrimeiro() {
        Emprestimo interno = registrarComoOperador(false);
        Emprestimo externo = service_registrarOutro(true);
        Emprestimo autorizado = service_registrarOutro(false);
        agir(gestorEquip);
        service.autorizar(jwt, autorizado.getId());
        when(emprestimoRepository.findByStatusIn(anyCollection())).thenReturn(List.of(autorizado, interno, externo));

        List<EmprestimoResponse> fila = service.listar(jwt, EmprestimoService.Grupo.ATIVOS);

        assertEquals(List.of(externo.getId(), interno.getId(), autorizado.getId()), fila.stream().map(EmprestimoResponse::id).toList());
        assertTrue(fila.get(0).externo());
    }

    // ---------- correção, prorrogação e resumo ----------

    private OffsetDateTime agora() {
        return OffsetDateTime.now(Clock.fixed(AGORA, ZoneOffset.UTC));
    }

    private EmprestimoEdicaoRequest edicao(OffsetDateTime prevista) {
        return new EmprestimoEdicaoRequest("João da Silva Júnior", "c7654321", null, "Nova finalidade", true, "Estúdio B", null, prevista, null);
    }

    @Test
    void corrigeDadosEnquantoAguardaAutorizacaoEAuditaSoOQueMudou() {
        Emprestimo e = registrarComoOperador(false);

        EmprestimoResponse r = service.editar(jwt, e.getId(), edicao(agora().plusDays(5)));

        assertEquals("João da Silva Júnior", r.retiranteNome());
        assertEquals("C7654321", r.retiranteChaveC());
        assertTrue(r.externo());
        assertEquals(agora().plusDays(5), r.previstaDevolucao());
        verify(auditoriaRepository).save(argThat((Auditoria a) -> "externo".equals(a.getCampo())
                && "false".equals(a.getValorAnterior()) && "true".equals(a.getValorNovo())));
        verify(auditoriaRepository).save(argThat((Auditoria a) -> "retirante_nome".equals(a.getCampo())
                && "João da Silva".equals(a.getValorAnterior())));
        // contato continuava "ramal 123"? o pedido novo o apagou: também fica registrado
        verify(auditoriaRepository).save(argThat((Auditoria a) -> "retirante_contato".equals(a.getCampo()) && a.getValorNovo() == null));
    }

    @Test
    void naoCorrigeDepoisDeAutorizadoNemComDataPassadaNemSemPermissao() {
        Emprestimo e = registrarComoOperador(false);
        assertThrows(IllegalArgumentException.class, () -> service.editar(jwt, e.getId(), edicao(agora().minusHours(1))));

        agir(consulta);
        assertThrows(AccessDeniedException.class, () -> service.editar(jwt, e.getId(), edicao(agora().plusDays(1))));

        agir(gestorEquip);
        service.autorizar(jwt, e.getId());
        agir(operador);
        assertThrows(EstadoInvalidoException.class, () -> service.editar(jwt, e.getId(), edicao(agora().plusDays(1))));
    }

    @Test
    void prorrogaSoQuemJaEstaComOEquipamentoEGravaOMotivo() {
        Emprestimo aguardando = registrarComoOperador(false);
        assertThrows(EstadoInvalidoException.class,
                () -> service.prorrogar(jwt, aguardando.getId(), new ProrrogacaoRequest(agora().plusDays(9), "x")));

        Emprestimo e = retiradoComDoisItens();
        OffsetDateTime nova = e.getPrevistaDevolucao().plusDays(3);
        EmprestimoResponse r = service.prorrogar(jwt, e.getId(), new ProrrogacaoRequest(nova, "  Gravação estendida  "));

        assertEquals(nova, r.previstaDevolucao());
        verify(auditoriaRepository).save(argThat((Auditoria a) -> "prevista_devolucao".equals(a.getCampo())
                && "Gravação estendida".equals(a.getMotivo()) && operador.getId().equals(a.getAlteradoPor())));
    }

    @Test
    void prorrogacaoPrecisaSerDepoisDaPrevistaAtual() {
        Emprestimo e = retiradoComDoisItens();
        assertThrows(IllegalArgumentException.class,
                () -> service.prorrogar(jwt, e.getId(), new ProrrogacaoRequest(e.getPrevistaDevolucao().minusHours(1), null)));
        agir(consulta);
        assertThrows(AccessDeniedException.class,
                () -> service.prorrogar(jwt, e.getId(), new ProrrogacaoRequest(e.getPrevistaDevolucao().plusDays(1), null)));
    }

    @Test
    void resumoContaAguardandoExternosEAtrasadosParaQuemOpera() {
        Emprestimo externo = service_registrarOutro(true);
        service_registrarOutro(false);
        Emprestimo retirado = retiradoComDoisItens();
        ReflectionTestUtils.setField(retirado, "previstaDevolucao", agora().minusHours(2));
        when(emprestimoRepository.findByStatusIn(anyCollection())).thenReturn(List.copyOf(banco.values()));
        agir(gestorEquip);

        ResumoEmprestimosResponse r = service.resumo(jwt);

        assertEquals(2, r.aguardandoAutorizacao());
        assertEquals(1, r.aguardandoExterno());
        assertEquals(1, r.atrasados());
        assertTrue(externo.isExterno());
    }

    @Test
    void resumoParaQuemSoConsultaTrazSoOsAtrasados() {
        service_registrarOutro(true);
        Emprestimo retirado = retiradoComDoisItens();
        ReflectionTestUtils.setField(retirado, "previstaDevolucao", agora().minusHours(2));
        when(emprestimoRepository.findByStatusIn(anyCollection())).thenReturn(List.copyOf(banco.values()));
        agir(consulta);

        ResumoEmprestimosResponse r = service.resumo(jwt);

        assertEquals(0, r.aguardandoAutorizacao());
        assertEquals(0, r.aguardandoExterno());
        assertEquals(1, r.atrasados());
    }

    private Emprestimo service_registrarOutro(boolean externo) {
        agir(operador);
        EmprestimoResponse r = service.registrar(jwt, pedido(externo, item(cabos, 1)));
        return banco.get(r.id());
    }
}
