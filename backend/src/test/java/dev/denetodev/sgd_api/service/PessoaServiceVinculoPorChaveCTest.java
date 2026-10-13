package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.AprovarRequest;
import dev.denetodev.sgd_api.dto.request.AutoCadastroRequest;
import dev.denetodev.sgd_api.dto.response.PessoaResponse;
import dev.denetodev.sgd_api.entity.*;
import dev.denetodev.sgd_api.exception.EstadoInvalidoException;
import dev.denetodev.sgd_api.exception.TentativasExcedidasException;
import dev.denetodev.sgd_api.repository.AreaRepository;
import dev.denetodev.sgd_api.repository.AuditoriaRepository;
import dev.denetodev.sgd_api.repository.CargoRepository;
import dev.denetodev.sgd_api.repository.PessoaRepository;
import dev.denetodev.sgd_api.security.ConfirmacaoEmail;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import dev.denetodev.sgd_api.security.LimitadorTentativas;
import dev.denetodev.sgd_api.security.PessoaAuthenticationConverter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Clock;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Quem cria conta e informa o CHAVE de uma pessoa importada (sem login) não assume o cadastro sozinho:
 * nasce como conta pendente, sem histórico e sem permissão, com a sugestão de vínculo. Só a confirmação
 * de um Gestor/Admin passa o login para o cadastro importado. O "banco" aqui é uma lista em memória.
 */
class PessoaServiceVinculoPorChaveCTest {

    private static final String CHAVE = "C1000001";

    private final List<Pessoa> banco = new ArrayList<>();
    private AuditoriaRepository auditoriaRepository;
    private CurrentPessoaResolver resolver;
    private ConfirmacaoEmail confirmacaoEmail;
    private PessoaRepository pessoaRepository;
    private AreaRepository areaRepository;
    private PessoaService service;
    private PessoaAuthenticationConverter converter;
    private Area area;
    private UUID areaId;
    private Pessoa admin;

    @BeforeEach
    void setUp() {
        pessoaRepository = mock(PessoaRepository.class);
        areaRepository = mock(AreaRepository.class);
        auditoriaRepository = mock(AuditoriaRepository.class);
        resolver = mock(CurrentPessoaResolver.class);
        confirmacaoEmail = mock(ConfirmacaoEmail.class);
        when(confirmacaoEmail.confirmado(any())).thenReturn(true);
        service = novoServico(new LimitadorTentativas(100, Duration.ofHours(1), Clock.systemUTC()));
        converter = new PessoaAuthenticationConverter(pessoaRepository);

        areaId = UUID.randomUUID();
        area = mock(Area.class);
        when(area.getId()).thenReturn(areaId);
        when(area.getDiretoria()).thenReturn(mock(Diretoria.class));
        when(areaRepository.findById(areaId)).thenReturn(Optional.of(area));

        // repositório em memória
        when(pessoaRepository.findByAuthUserId(any())).thenAnswer(inv ->
                banco.stream().filter(p -> inv.getArgument(0).equals(p.getAuthUserId())).findFirst());
        when(pessoaRepository.findByChaveC(any())).thenAnswer(inv ->
                banco.stream().filter(p -> inv.getArgument(0).equals(p.getChaveC())).findFirst());
        when(pessoaRepository.findById(any(UUID.class))).thenAnswer(inv ->
                banco.stream().filter(p -> inv.getArgument(0).equals(p.getId())).findFirst());
        when(pessoaRepository.save(any(Pessoa.class))).thenAnswer(inv -> {
            Pessoa nova = spy((Pessoa) inv.getArgument(0));
            doReturn(UUID.randomUUID()).when(nova).getId();
            banco.add(nova);
            return nova;
        });
        doAnswer(inv -> {
            banco.remove(inv.<Pessoa>getArgument(0));
            return null;
        }).when(pessoaRepository).delete(any(Pessoa.class));

        admin = pessoa("Admin", PerfilPessoa.ADMIN);
        admin.setAprovadoEm(OffsetDateTime.now());
        banco.add(admin);
        when(resolver.resolver(any())).thenReturn(admin);
    }

    private PessoaService novoServico(LimitadorTentativas limitador) {
        return new PessoaService(pessoaRepository, areaRepository, mock(CargoRepository.class),
                auditoriaRepository, resolver, confirmacaoEmail, limitador);
    }

    private Pessoa pessoa(String nome, PerfilPessoa perfil) {
        Pessoa p = spy(new Pessoa(nome, area == null ? mock(Area.class) : area));
        doReturn(UUID.randomUUID()).when(p).getId();
        p.setPerfil(perfil);
        return p;
    }

    /** Pessoa que veio da planilha: aprovada, sem login, com CHAVE preenchido por um Admin. */
    private Pessoa importada(String chaveC) {
        Pessoa p = pessoa("Klebson de Oliveira", PerfilPessoa.PROFISSIONAL);
        p.setChaveC(chaveC);
        p.setAprovadoEm(OffsetDateTime.now());
        banco.add(p);
        return p;
    }

    private Jwt jwtDaConta(UUID authId) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(authId.toString());
        when(jwt.getClaimAsString("email")).thenReturn("klebson@bb.com.br");
        return jwt;
    }

    private PessoaResponse cadastrar(UUID authId, String chaveC) {
        return service.autoCadastro(jwtDaConta(authId), new AutoCadastroRequest("Klebson de Oliveira Santos", chaveC, "Kle", areaId, null));
    }

    private Pessoa daConta(UUID authId) {
        return banco.stream().filter(p -> authId.equals(p.getAuthUserId())).findFirst().orElseThrow();
    }

    private Set<String> permissoes(UUID authId) {
        Jwt jwt = jwtDaConta(authId);
        return converter.convert(jwt).getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toSet());
    }

    // ------------------------------------------------------------------ cadastro

    @Test
    void chaveNovoCriaRegistroNovoComOChaveC() {
        UUID authId = UUID.randomUUID();
        cadastrar(authId, CHAVE);

        Pessoa nova = daConta(authId);
        assertEquals(CHAVE, nova.getChaveC());
        assertNull(nova.getVinculoSugerido());
        assertNull(nova.getAprovadoEm());
        assertEquals(2, banco.size()); // admin + a nova
    }

    @Test
    void chaveDeImportadoSemLoginCriaContaPendenteComSugestaoSemDuplicarNemTocarNoImportado() {
        Pessoa importada = importada(CHAVE);
        UUID idDoImportado = importada.getId();
        UUID authId = UUID.randomUUID();

        PessoaResponse r = cadastrar(authId, CHAVE);

        Pessoa pendente = daConta(authId);
        assertNotEquals(idDoImportado, pendente.getId());
        assertEquals(importada, pendente.getVinculoSugerido());
        assertNull(pendente.getChaveC());          // o CHAVE continua só no cadastro importado (é único)
        assertNull(pendente.getAprovadoEm());   // pendente de verdade
        assertTrue(r.temVinculoSugerido());
        assertNull(importada.getAuthUserId());  // o login NÃO foi gravado no importado
        assertEquals(idDoImportado, importada.getId());
        verify(auditoriaRepository).save(argThat((Auditoria a) -> "vinculo_sugerido_id".equals(a.getCampo())
                && idDoImportado.toString().equals(a.getValorNovo()) && pendente.getId().equals(a.getRegistroId())));
    }

    @Test
    void chaveDePessoaQueJaTemLoginERecusadoComMensagemNeutraSemVazarDados() {
        Pessoa dona = importada(CHAVE);
        dona.setAuthUserId(UUID.randomUUID());
        int antes = banco.size();

        EstadoInvalidoException e = assertThrows(EstadoInvalidoException.class, () -> cadastrar(UUID.randomUUID(), CHAVE));

        assertEquals(PessoaService.MSG_CADASTRO_NAO_CONCLUIDO, e.getMessage());
        assertFalse(e.getMessage().contains("Klebson"));
        assertFalse(e.getMessage().contains(CHAVE));
        assertEquals(antes, banco.size());
    }

    @Test
    void contaJaLigadaAOutraPessoaERecusada() {
        UUID authId = UUID.randomUUID();
        cadastrar(authId, CHAVE);

        assertThrows(EstadoInvalidoException.class, () -> cadastrar(authId, "C1000002"));
        assertEquals(2, banco.size());
    }

    @Test
    void emailNaoConfirmadoNaoCadastra() {
        UUID authId = UUID.randomUUID();
        when(confirmacaoEmail.confirmado(authId)).thenReturn(false);

        assertThrows(EstadoInvalidoException.class, () -> cadastrar(authId, CHAVE));
        assertEquals(1, banco.size());
    }

    @Test
    void importadoJaComOMesmoEmailDaContaNaoEstouraAUnicidadeDoEmail() {
        Pessoa importada = importada(CHAVE);
        importada.setEmail("klebson@bb.com.br"); // o Gestor preencheu o e-mail antes de a pessoa se cadastrar
        when(pessoaRepository.existsByEmailIgnoreCase("klebson@bb.com.br")).thenReturn(true);
        UUID authId = UUID.randomUUID();

        cadastrar(authId, CHAVE);

        Pessoa pendente = daConta(authId);
        assertNull(pendente.getEmail()); // e-mail é único: a pendente nasce sem ele
        assertEquals(importada, pendente.getVinculoSugerido());
    }

    @Test
    void chaveEmMinusculaComEspacosEAceita_eFormatoInvalidoERecusado() {
        UUID authId = UUID.randomUUID();
        cadastrar(authId, "  c1000001 ");
        assertEquals(CHAVE, daConta(authId).getChaveC());

        assertThrows(IllegalArgumentException.class, () -> cadastrar(UUID.randomUUID(), "C100000"));
        assertThrows(IllegalArgumentException.class, () -> cadastrar(UUID.randomUUID(), "X1000001"));
    }

    @Test
    void depoisDoLimiteDeTentativasACadastroEBarrado() {
        service = novoServico(new LimitadorTentativas(3, Duration.ofHours(1), Clock.systemUTC()));
        UUID authId = UUID.randomUUID();
        when(confirmacaoEmail.confirmado(authId)).thenReturn(false); // cada envio falha, mas conta como tentativa

        for (int i = 0; i < 3; i++) {
            assertThrows(EstadoInvalidoException.class, () -> cadastrar(authId, CHAVE));
        }
        assertThrows(TentativasExcedidasException.class, () -> cadastrar(authId, CHAVE));
        // outra conta não é afetada
        UUID outra = UUID.randomUUID();
        assertDoesNotThrow(() -> cadastrar(outra, "C1000002"));
    }

    // ------------------------------------------------------------------ acesso e aprovação

    @Test
    void contaVinculadaPorSugestaoMasNaoAprovadaNaoTemNenhumaPermissao() {
        importada(CHAVE);
        UUID authId = UUID.randomUUID();
        cadastrar(authId, CHAVE);

        assertTrue(permissoes(authId).isEmpty(), "pendente não pode ter VINCULADO nem ROLE_*");
    }

    @Test
    void gestorEAdminConfirmamOVinculo_eALoginPassaParaOCadastroImportadoComOMesmoId() {
        for (PerfilPessoa perfil : List.of(PerfilPessoa.GESTOR, PerfilPessoa.ADMIN)) {
            banco.removeIf(p -> p != admin);
            Pessoa importada = importada(CHAVE);
            UUID idDoImportado = importada.getId();
            UUID authId = UUID.randomUUID();
            cadastrar(authId, CHAVE);
            Pessoa pendente = daConta(authId);

            Pessoa aprovador = pessoa("Aprovador", perfil);
            aprovador.setAprovadoEm(OffsetDateTime.now());
            when(resolver.resolver(any())).thenReturn(aprovador);

            PessoaResponse r = service.confirmarVinculo(mock(Jwt.class), pendente.getId());

            assertEquals(idDoImportado, r.id());                 // histórico preservado: o mesmo registro
            assertEquals(authId, importada.getAuthUserId());
            assertFalse(banco.contains(pendente));               // a conta pendente (duplicata) saiu
            assertTrue(permissoes(authId).containsAll(Set.of("VINCULADO", "ROLE_PROFISSIONAL")));
            assertEquals(idDoImportado, daConta(authId).getId());
            verify(auditoriaRepository, atLeastOnce()).save(argThat((Auditoria a) -> "vinculo_confirmado".equals(a.getCampo())
                    && idDoImportado.equals(a.getRegistroId()) && aprovador.getId().equals(a.getAlteradoPor())));
            verify(auditoriaRepository, atLeastOnce()).save(argThat((Auditoria a) -> "auth_user_id".equals(a.getCampo())
                    && authId.toString().equals(a.getValorNovo()) && idDoImportado.equals(a.getRegistroId())));
        }
    }

    @Test
    void profissionalEVisualizadorNaoConfirmamVinculo() {
        Pessoa importada = importada(CHAVE);
        UUID authId = UUID.randomUUID();
        cadastrar(authId, CHAVE);
        UUID pendenteId = daConta(authId).getId();

        for (PerfilPessoa perfil : List.of(PerfilPessoa.PROFISSIONAL, PerfilPessoa.VISUALIZADOR)) {
            Pessoa semPoder = pessoa("Sem poder", perfil);
            when(resolver.resolver(any())).thenReturn(semPoder);
            assertThrows(AccessDeniedException.class, () -> service.confirmarVinculo(mock(Jwt.class), pendenteId));
            assertThrows(AccessDeniedException.class,
                    () -> service.vincularContaAPessoaExistente(mock(Jwt.class), pendenteId, importada.getId()));
        }
        assertNull(importada.getAuthUserId());
    }

    @Test
    void naoConfirmaContaSemSugestaoNemComEmailNaoConfirmado() {
        UUID semSugestao = UUID.randomUUID();
        cadastrar(semSugestao, "C1000002");
        assertThrows(EstadoInvalidoException.class,
                () -> service.confirmarVinculo(mock(Jwt.class), daConta(semSugestao).getId()));

        importada(CHAVE);
        UUID comSugestao = UUID.randomUUID();
        cadastrar(comSugestao, CHAVE);
        when(confirmacaoEmail.confirmado(comSugestao)).thenReturn(false);
        assertThrows(EstadoInvalidoException.class,
                () -> service.confirmarVinculo(mock(Jwt.class), daConta(comSugestao).getId()));
    }

    @Test
    void aprovarComoPessoaNovaLimpaASugestaoERegistraNaAuditoria() {
        Pessoa importada = importada(CHAVE);
        UUID authId = UUID.randomUUID();
        cadastrar(authId, CHAVE);
        Pessoa pendente = daConta(authId);

        service.aprovar(pendente.getId(), mock(Jwt.class), new AprovarRequest(PerfilPessoa.PROFISSIONAL, null, null, null));

        assertNull(pendente.getVinculoSugerido());
        assertNotNull(pendente.getAprovadoEm());
        verify(auditoriaRepository).save(argThat((Auditoria a) -> "vinculo_sugerido_id".equals(a.getCampo())
                && importada.getId().toString().equals(a.getValorAnterior()) && a.getValorNovo() == null));
    }

    // ------------------------------------------------------------------ vínculo direto (Admin)

    @Test
    void vincularAuthDiretoConfereContaLivrePessoaSemContaEEmailConfirmado() {
        Pessoa importada = importada(CHAVE);
        UUID authId = UUID.randomUUID();

        service.vincularAuth(mock(Jwt.class), importada.getId(), authId);
        assertEquals(authId, importada.getAuthUserId());
        verify(auditoriaRepository).save(argThat((Auditoria a) -> "auth_user_id".equals(a.getCampo())
                && authId.toString().equals(a.getValorNovo()) && admin.getId().equals(a.getAlteradoPor())));

        // pessoa que já tem conta
        assertThrows(EstadoInvalidoException.class,
                () -> service.vincularAuth(mock(Jwt.class), importada.getId(), UUID.randomUUID()));
        // conta que já é de outra pessoa
        Pessoa outra = importada("C1000002");
        assertThrows(EstadoInvalidoException.class, () -> service.vincularAuth(mock(Jwt.class), outra.getId(), authId));
        // e-mail não confirmado
        UUID naoConfirmada = UUID.randomUUID();
        when(confirmacaoEmail.confirmado(naoConfirmada)).thenReturn(false);
        assertThrows(EstadoInvalidoException.class, () -> service.vincularAuth(mock(Jwt.class), outra.getId(), naoConfirmada));
    }

    // ------------------------------------------------------------------ caminho completo

    @Test
    void caminhoCompleto_importadoSemLogin_contaCriada_chaveInformado_aprovacao_pessoaVeOMesmoCadastro() {
        Pessoa importada = importada(CHAVE);
        UUID idDoImportado = importada.getId();
        UUID authId = UUID.randomUUID();

        // 1) cria conta e informa o chave C: continua sem acesso
        PessoaResponse pedido = cadastrar(authId, CHAVE);
        assertTrue(pedido.temVinculoSugerido());
        assertTrue(permissoes(authId).isEmpty());
        assertFalse(service.buscarStatusPorAuthUserId(authId).vinculado());

        // 2) o Gestor confirma
        service.confirmarVinculo(mock(Jwt.class), pedido.id());

        // 3) a mesma conta agora é a pessoa importada, aprovada, com o mesmo id (logo, as mesmas demandas)
        var me = service.buscarStatusPorAuthUserId(authId);
        assertTrue(me.vinculado());
        assertEquals(idDoImportado, me.pessoa().id());
        assertTrue(permissoes(authId).contains("VINCULADO"));
        assertEquals("klebson@bb.com.br", importada.getEmail());
    }
}
