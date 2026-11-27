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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Cada equipe tem uma só Referência: tentar pôr uma segunda dá um aviso claro, com o nome de quem já é. */
class PessoaServiceReferenciaUnicaTest {

    private PessoaRepository pessoaRepository;
    private CurrentPessoaResolver resolver;
    private PessoaService service;
    private final Jwt jwt = mock(Jwt.class);
    private Area area;
    private Pessoa admin;

    @BeforeEach
    void preparar() {
        pessoaRepository = mock(PessoaRepository.class);
        AreaRepository areaRepository = mock(AreaRepository.class);
        resolver = mock(CurrentPessoaResolver.class);
        service = new PessoaService(pessoaRepository, areaRepository, mock(CargoRepository.class),
                mock(AuditoriaRepository.class), resolver, mock(ConfirmacaoEmail.class), mock(LimitadorTentativas.class));
        area = mock(Area.class);
        UUID areaId = UUID.randomUUID();
        when(area.getId()).thenReturn(areaId);
        when(area.getNome()).thenReturn("Design-CRM");
        when(area.getDiretoria()).thenReturn(mock(Diretoria.class));
        when(areaRepository.findById(areaId)).thenReturn(Optional.of(area));
        admin = pessoa("Admin", PerfilPessoa.ADMIN);
        when(resolver.resolver(any())).thenReturn(admin);
    }

    private Pessoa pessoa(String nome, PerfilPessoa perfil) {
        Pessoa p = new Pessoa(nome, area);
        ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
        p.setPerfil(perfil);
        when(pessoaRepository.findById(p.getId())).thenReturn(Optional.of(p));
        return p;
    }

    private PessoaRequest pedido(UUID referenciaAreaId) {
        return new PessoaRequest("Lucas", null, null, area.getId(), null, StatusPessoa.ATIVO, PerfilPessoa.PROFISSIONAL, referenciaAreaId);
    }

    @Test
    void segundaReferenciaNaMesmaEquipeAvisaQuemJaEh() {
        Pessoa gabriel = pessoa("Gabriel Rodrigues Rocha", PerfilPessoa.PROFISSIONAL);
        Pessoa lucas = pessoa("Lucas Corsino", PerfilPessoa.PROFISSIONAL);
        when(pessoaRepository.findByReferenciaAreaId(area.getId())).thenReturn(Optional.of(gabriel));

        EstadoInvalidoException e = assertThrows(EstadoInvalidoException.class,
                () -> service.atualizar(jwt, lucas.getId(), pedido(area.getId())));

        assertTrue(e.getMessage().contains("Design-CRM"), e.getMessage());
        assertTrue(e.getMessage().contains("já tem uma Referência"), e.getMessage());
    }

    @Test
    void quemJaEhAReferenciaPodeSerSalvoSemTrocar() {
        Pessoa gabriel = pessoa("Gabriel Rodrigues Rocha", PerfilPessoa.PROFISSIONAL);
        when(pessoaRepository.findByReferenciaAreaId(area.getId())).thenReturn(Optional.of(gabriel));

        assertDoesNotThrow(() -> service.atualizar(jwt, gabriel.getId(), pedido(area.getId())));
    }

    @Test
    void equipeSemReferenciaAceitaAPrimeira() {
        Pessoa lucas = pessoa("Lucas Corsino", PerfilPessoa.PROFISSIONAL);
        when(pessoaRepository.findByReferenciaAreaId(area.getId())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> service.atualizar(jwt, lucas.getId(), pedido(area.getId())));
        assertEquals(area, lucas.getReferenciaArea());
    }
}
