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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** O papel no módulo de equipamentos é o que dá acesso ao Profissional; só Gestor e Admin atribuem. */
class PessoaServicePapelEquipamentosTest {

    private PessoaRepository pessoaRepository;
    private AuditoriaRepository auditoriaRepository;
    private CurrentPessoaResolver resolver;
    private PessoaService service;
    private final Jwt jwt = mock(Jwt.class);
    private Area area;

    @BeforeEach
    void preparar() {
        pessoaRepository = mock(PessoaRepository.class);
        auditoriaRepository = mock(AuditoriaRepository.class);
        resolver = mock(CurrentPessoaResolver.class);
        service = new PessoaService(pessoaRepository, mock(AreaRepository.class), mock(CargoRepository.class),
                auditoriaRepository, resolver, mock(ConfirmacaoEmail.class), mock(LimitadorTentativas.class));
        area = mock(Area.class);
        when(area.getId()).thenReturn(UUID.randomUUID());
        when(area.getDiretoria()).thenReturn(mock(Diretoria.class));
    }

    private Pessoa pessoa(String nome, PerfilPessoa perfil) {
        Pessoa p = new Pessoa(nome, area);
        ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
        p.setPerfil(perfil);
        when(pessoaRepository.findById(p.getId())).thenReturn(Optional.of(p));
        return p;
    }

    private void agir(Pessoa quem) {
        when(resolver.resolver(any())).thenReturn(quem);
    }

    @Test
    void adminDaPapelAoProfissionalEIssoLiberaOAcessoNaResposta() {
        Pessoa admin = pessoa("Admin", PerfilPessoa.ADMIN);
        Pessoa fulano = pessoa("Fulano", PerfilPessoa.PROFISSIONAL);
        agir(admin);

        PessoaResponse antes = service.definirPapelEquipamentos(jwt, fulano.getId(), null);
        assertFalse(antes.acessaEquipamentos()); // sem papel, nem consulta

        PessoaResponse depois = service.definirPapelEquipamentos(jwt, fulano.getId(), PapelEquipamentos.OPERADOR_DE_ESTUDIO);
        assertTrue(depois.acessaEquipamentos());
        assertEquals(PapelEquipamentos.OPERADOR_DE_ESTUDIO, depois.papelEquipamentos());
        verify(auditoriaRepository).save(argThat((Auditoria a) -> "papel_equipamentos".equals(a.getCampo())
                && "OPERADOR_DE_ESTUDIO".equals(a.getValorNovo()) && admin.getId().equals(a.getAlteradoPor())));
    }

    @Test
    void removerOPapelTiraOAcesso() {
        Pessoa admin = pessoa("Admin", PerfilPessoa.ADMIN);
        Pessoa fulano = pessoa("Fulano", PerfilPessoa.PROFISSIONAL);
        fulano.setPapelEquipamentos(PapelEquipamentos.GESTOR_DE_EQUIPAMENTOS);
        agir(admin);

        PessoaResponse r = service.definirPapelEquipamentos(jwt, fulano.getId(), null);

        assertNull(r.papelEquipamentos());
        assertFalse(r.acessaEquipamentos());
    }

    @Test
    void adminEGestorTemAcessoMesmoSemPapel() {
        Pessoa admin = pessoa("Admin", PerfilPessoa.ADMIN);
        Pessoa gestor = pessoa("Gestor", PerfilPessoa.GESTOR);
        agir(admin);

        assertTrue(service.definirPapelEquipamentos(jwt, gestor.getId(), null).acessaEquipamentos());
    }

    @Test
    void visualizadorNaoRecebePapel() {
        Pessoa admin = pessoa("Admin", PerfilPessoa.ADMIN);
        Pessoa leitor = pessoa("Leitor", PerfilPessoa.VISUALIZADOR);
        agir(admin);

        assertThrows(EstadoInvalidoException.class,
                () -> service.definirPapelEquipamentos(jwt, leitor.getId(), PapelEquipamentos.OPERADOR_DE_ESTUDIO));
        assertNull(leitor.getPapelEquipamentos());
    }

    @Test
    void profissionalNaoDaPapelParaNinguem() {
        Pessoa profissional = pessoa("Profissional", PerfilPessoa.PROFISSIONAL);
        Pessoa fulano = pessoa("Fulano", PerfilPessoa.PROFISSIONAL);
        agir(profissional);

        assertThrows(AccessDeniedException.class,
                () -> service.definirPapelEquipamentos(jwt, fulano.getId(), PapelEquipamentos.OPERADOR_DE_ESTUDIO));
    }

    @Test
    void gestorNaoMexeNoPapelDeUmAdmin() {
        Pessoa gestor = pessoa("Gestor", PerfilPessoa.GESTOR);
        Pessoa admin = pessoa("Admin", PerfilPessoa.ADMIN);
        agir(gestor);

        assertThrows(AccessDeniedException.class,
                () -> service.definirPapelEquipamentos(jwt, admin.getId(), PapelEquipamentos.GESTOR_DE_EQUIPAMENTOS));
    }
}
