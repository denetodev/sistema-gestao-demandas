package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.response.PessoaResponse;
import dev.denetodev.sgd_api.entity.*;
import dev.denetodev.sgd_api.exception.EstadoInvalidoException;
import dev.denetodev.sgd_api.repository.AreaRepository;
import dev.denetodev.sgd_api.repository.AuditoriaRepository;
import dev.denetodev.sgd_api.repository.CargoRepository;
import dev.denetodev.sgd_api.repository.PessoaRepository;
import dev.denetodev.sgd_api.security.ConfirmacaoEmail;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import dev.denetodev.sgd_api.security.LimitadorTentativas;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Admin ou Gestor liga a conta pendente (recém-cadastrada) ao cadastro que já existe, por exemplo uma
 * pessoa importada da planilha: a conta passa a ser dessa pessoa e o histórico dela é preservado.
 */
class PessoaServiceVinculoTest {

    private static final String CHAVE = "C1000001";

    private PessoaRepository pessoaRepository;
    private AuditoriaRepository auditoriaRepository;
    private CurrentPessoaResolver resolver;
    private PessoaService service;
    private Area area;
    private Pessoa admin;
    private Pessoa pendente;
    private Pessoa importada;
    private UUID authId;
    private final Jwt jwt = mock(Jwt.class);

    @BeforeEach
    void setUp() {
        pessoaRepository = mock(PessoaRepository.class);
        auditoriaRepository = mock(AuditoriaRepository.class);
        resolver = mock(CurrentPessoaResolver.class);
        service = new PessoaService(pessoaRepository, mock(AreaRepository.class), mock(CargoRepository.class),
                auditoriaRepository, resolver, mock(ConfirmacaoEmail.class), mock(LimitadorTentativas.class));

        area = mock(Area.class);
        when(area.getId()).thenReturn(UUID.randomUUID());
        Diretoria diretoria = mock(Diretoria.class);
        when(area.getDiretoria()).thenReturn(diretoria);

        admin = pessoa("Admin", PerfilPessoa.ADMIN);
        when(resolver.resolver(any())).thenReturn(admin);

        authId = UUID.randomUUID();
        pendente = pessoa("Klebson de Oliveira Santos", PerfilPessoa.PROFISSIONAL);
        pendente.setAuthUserId(authId);
        pendente.setEmail("klebson@bb.com.br");
        pendente.setChaveC(CHAVE);
        pendente.setAprovadoEm(null);

        importada = pessoa("Klebson", PerfilPessoa.PROFISSIONAL);
        importada.setAprovadoEm(OffsetDateTime.now());

        when(pessoaRepository.findById(pendente.getId())).thenReturn(Optional.of(pendente));
        when(pessoaRepository.findById(importada.getId())).thenReturn(Optional.of(importada));
        when(pessoaRepository.save(any(Pessoa.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    private Pessoa pessoa(String nome, PerfilPessoa perfil) {
        Pessoa p = spy(new Pessoa(nome, area));
        UUID id = UUID.randomUUID();
        doReturn(id).when(p).getId();
        p.setPerfil(perfil);
        return p;
    }

    @Test
    void contaPendenteViraAContaDaPessoaImportada_eOCadastroDuplicadoSaiDeCena() {
        PessoaResponse r = service.vincularContaAPessoaExistente(jwt, pendente.getId(), importada.getId());

        assertEquals(authId, importada.getAuthUserId());
        assertEquals("Klebson", r.nome()); // o cadastro importado é o que vale
        // a auditoria do pedido de vínculo foi escrita pela conta pendente: a autoria passa antes do delete,
        // senão a FK auditoria.alterado_por barra a remoção (409 "registro em uso" em produção)
        InOrder ordem = inOrder(auditoriaRepository, pessoaRepository);
        ordem.verify(auditoriaRepository).transferirAutoria(pendente.getId(), importada.getId());
        ordem.verify(pessoaRepository).delete(pendente);
        ordem.verify(pessoaRepository).flush(); // libera auth_user_id e chaveC (únicos) antes de usar na importada
    }

    @Test
    void chaveEEmailInformadosNoCadastroPassamParaAPessoaImportadaQuandoElaNaoTem() {
        service.vincularContaAPessoaExistente(jwt, pendente.getId(), importada.getId());

        assertEquals(CHAVE, importada.getChaveC());
        assertEquals("klebson@bb.com.br", importada.getEmail());
    }

    @Test
    void chaveDaImportadaNaoESobrescritoMasOEmailPassaAserODaContaDeLogin() {
        importada.setChaveC("C1000002");
        importada.setEmail("antigo@bb.com.br");

        service.vincularContaAPessoaExistente(jwt, pendente.getId(), importada.getId());

        assertEquals("C1000002", importada.getChaveC());
        assertEquals("klebson@bb.com.br", importada.getEmail());
        verify(auditoriaRepository).save(argThat((Auditoria a) -> "email".equals(a.getCampo())
                && "antigo@bb.com.br".equals(a.getValorAnterior()) && "klebson@bb.com.br".equals(a.getValorNovo())));
    }

    @Test
    void ficaRegistradoNaAuditoriaQuemLigouQual() {
        service.vincularContaAPessoaExistente(jwt, pendente.getId(), importada.getId());

        verify(auditoriaRepository).save(argThat((Auditoria a) ->
                a.getRegistroId().equals(importada.getId()) && "auth_user_id".equals(a.getCampo())
                        && authId.toString().equals(a.getValorNovo()) && admin.getId().equals(a.getAlteradoPor())));
    }

    @Test
    void naoVinculaSeAPessoaJaTemContaOuSeAContaJaFoiAprovada() {
        importada.setAuthUserId(UUID.randomUUID());
        assertThrows(EstadoInvalidoException.class,
                () -> service.vincularContaAPessoaExistente(jwt, pendente.getId(), importada.getId()));

        importada.setAuthUserId(null);
        pendente.setAprovadoEm(OffsetDateTime.now());
        assertThrows(EstadoInvalidoException.class,
                () -> service.vincularContaAPessoaExistente(jwt, pendente.getId(), importada.getId()));
        verify(pessoaRepository, never()).delete(any(Pessoa.class));
    }

    @Test
    void naoVinculaContaRejeitadaNemPessoaInativaNemAMesmaPessoa() {
        pendente.setStatus(StatusPessoa.REJEITADO);
        assertThrows(EstadoInvalidoException.class,
                () -> service.vincularContaAPessoaExistente(jwt, pendente.getId(), importada.getId()));

        pendente.setStatus(StatusPessoa.ATIVO);
        importada.setStatus(StatusPessoa.INATIVO);
        assertThrows(EstadoInvalidoException.class,
                () -> service.vincularContaAPessoaExistente(jwt, pendente.getId(), importada.getId()));

        importada.setStatus(StatusPessoa.ATIVO);
        assertThrows(EstadoInvalidoException.class,
                () -> service.vincularContaAPessoaExistente(jwt, pendente.getId(), pendente.getId()));
    }

    @Test
    void importadaPrecisaJaTerSidoAprovada() {
        importada.setAprovadoEm(null);
        assertThrows(EstadoInvalidoException.class,
                () -> service.vincularContaAPessoaExistente(jwt, pendente.getId(), importada.getId()));
    }

    @Test
    void gestorNaoLigaContaANinguemQueSejaAdmin() {
        Pessoa gestor = pessoa("Gestor", PerfilPessoa.GESTOR);
        when(resolver.resolver(any())).thenReturn(gestor);
        importada.setPerfil(PerfilPessoa.ADMIN);

        assertThrows(AccessDeniedException.class,
                () -> service.vincularContaAPessoaExistente(jwt, pendente.getId(), importada.getId()));
        assertNull(importada.getAuthUserId());
    }

    @Test
    void gestorLigaContaDeProfissional() {
        Pessoa gestor = pessoa("Gestor", PerfilPessoa.GESTOR);
        when(resolver.resolver(any())).thenReturn(gestor);

        service.vincularContaAPessoaExistente(jwt, pendente.getId(), importada.getId());

        assertEquals(authId, importada.getAuthUserId());
    }
}
