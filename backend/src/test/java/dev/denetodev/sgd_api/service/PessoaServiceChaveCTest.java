package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.PessoaRequest;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** A chave C é única, mas editar a própria pessoa com a chave que ela já tem não é conflito. */
class PessoaServiceChaveCTest {

    private PessoaRepository pessoaRepository;
    private AreaRepository areaRepository;
    private CurrentPessoaResolver resolver;
    private PessoaService service;
    private final Jwt jwt = mock(Jwt.class);
    private Area area;

    @BeforeEach
    void preparar() {
        pessoaRepository = mock(PessoaRepository.class);
        areaRepository = mock(AreaRepository.class);
        resolver = mock(CurrentPessoaResolver.class);
        service = new PessoaService(pessoaRepository, areaRepository, mock(CargoRepository.class),
                mock(AuditoriaRepository.class), resolver, mock(ConfirmacaoEmail.class), mock(LimitadorTentativas.class));
        area = mock(Area.class);
        UUID areaId = UUID.randomUUID();
        when(area.getId()).thenReturn(areaId);
        when(area.getDiretoria()).thenReturn(mock(Diretoria.class));
        when(areaRepository.findById(areaId)).thenReturn(Optional.of(area));
    }

    private Pessoa pessoa(String nome, PerfilPessoa perfil, String chave) {
        Pessoa p = new Pessoa(nome, area);
        ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
        p.setPerfil(perfil);
        p.setChaveC(chave);
        when(pessoaRepository.findById(p.getId())).thenReturn(Optional.of(p));
        return p;
    }

    private PessoaRequest pedido(String chave, UUID referenciaAreaId) {
        return new PessoaRequest("Fulano", null, chave, area.getId(), null, StatusPessoa.ATIVO, PerfilPessoa.PROFISSIONAL, referenciaAreaId);
    }

    @Test
    void editarAPropriaPessoaComAMesmaChaveNaoEConflito() {
        Pessoa admin = pessoa("Admin", PerfilPessoa.ADMIN, null);
        Pessoa fulano = pessoa("Fulano", PerfilPessoa.PROFISSIONAL, "C1349864");
        when(resolver.resolver(any())).thenReturn(admin);
        when(pessoaRepository.findByChaveC("C1349864")).thenReturn(Optional.of(fulano));

        assertDoesNotThrow(() -> service.atualizar(jwt, fulano.getId(), pedido("c1349864", null)));
    }

    @Test
    void mesmaPessoaVindaComoOutraInstanciaOuProxyTambemNaoEConflito() {
        // o Hibernate pode devolver, na consulta por chave, uma referência diferente da que foi carregada por id
        Pessoa admin = pessoa("Admin", PerfilPessoa.ADMIN, null);
        Pessoa fulano = pessoa("Fulano", PerfilPessoa.PROFISSIONAL, "C1349864");
        Pessoa outraReferencia = new Pessoa("Fulano", area);
        ReflectionTestUtils.setField(outraReferencia, "id", fulano.getId());
        outraReferencia.setChaveC("C1349864");
        when(resolver.resolver(any())).thenReturn(admin);
        when(pessoaRepository.findByChaveC("C1349864")).thenReturn(Optional.of(outraReferencia));

        assertDoesNotThrow(() -> service.atualizar(jwt, fulano.getId(), pedido("C1349864", null)));
    }

    @Test
    void chaveDeOutraPessoaContinuaSendoConflito() {
        Pessoa admin = pessoa("Admin", PerfilPessoa.ADMIN, null);
        Pessoa fulano = pessoa("Fulano", PerfilPessoa.PROFISSIONAL, "C1111111");
        Pessoa ciclano = pessoa("Ciclano", PerfilPessoa.PROFISSIONAL, "C2222222");
        when(resolver.resolver(any())).thenReturn(admin);
        when(pessoaRepository.findByChaveC("C2222222")).thenReturn(Optional.of(ciclano));

        assertThrows(EstadoInvalidoException.class, () -> service.atualizar(jwt, fulano.getId(), pedido("C2222222", null)));
    }

    @Test
    void igualdadeDePessoaUsaOIdentificadorEntaoDuasReferenciasDaMesmaPessoaSaoIguais() {
        Pessoa a = pessoa("A", PerfilPessoa.PROFISSIONAL, null);
        Pessoa mesma = new Pessoa("A", area);
        ReflectionTestUtils.setField(mesma, "id", a.getId());

        assertEquals(a, mesma);
        assertEquals(mesma, a);
        assertNotEquals(a, pessoa("B", PerfilPessoa.PROFISSIONAL, null));
    }
}
