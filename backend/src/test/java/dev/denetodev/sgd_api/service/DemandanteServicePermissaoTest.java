package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.DemandanteRequest;
import dev.denetodev.sgd_api.entity.Area;
import dev.denetodev.sgd_api.entity.PerfilPessoa;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.entity.TipoCliente;
import dev.denetodev.sgd_api.repository.DemandanteRepository;
import dev.denetodev.sgd_api.repository.DiretoriaRepository;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import dev.denetodev.sgd_api.service.support.PermissaoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Demandante: Admin, Gestor e Referência de equipe criam e editam; Profissional comum não. */
class DemandanteServicePermissaoTest {

    private CurrentPessoaResolver resolver;
    private DemandanteRepository repositorio;
    private DemandanteService service;
    private final Jwt jwt = mock(Jwt.class);

    @BeforeEach
    void setUp() {
        resolver = mock(CurrentPessoaResolver.class);
        repositorio = mock(DemandanteRepository.class);
        service = new DemandanteService(repositorio, mock(DiretoriaRepository.class), resolver, new PermissaoService(null));
        when(repositorio.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Pessoa pessoa(PerfilPessoa perfil, boolean referencia) {
        Area area = referencia ? mock(Area.class) : null; // criada antes: mock dentro de when() quebra o stubbing
        Pessoa p = mock(Pessoa.class);
        when(p.getPerfil()).thenReturn(perfil);
        when(p.getReferenciaArea()).thenReturn(area);
        return p;
    }

    private DemandanteRequest pedido() {
        return new DemandanteRequest("Ana Paula", TipoCliente.PESSOA, null, null);
    }

    @Test
    void profissionalComumNaoCriaNemEdita() {
        Pessoa usuario = pessoa(PerfilPessoa.PROFISSIONAL, false);
        when(resolver.resolver(jwt)).thenReturn(usuario);

        assertThrows(AccessDeniedException.class, () -> service.criar(jwt, pedido()));
        assertThrows(AccessDeniedException.class, () -> service.atualizar(jwt, UUID.randomUUID(), pedido()));
        verify(repositorio, never()).save(any());
    }

    @Test
    void referenciaDeEquipeGestorEAdminCriam() {
        for (Pessoa usuario : new Pessoa[]{pessoa(PerfilPessoa.PROFISSIONAL, true), pessoa(PerfilPessoa.GESTOR, false), pessoa(PerfilPessoa.ADMIN, false)}) {
            when(resolver.resolver(jwt)).thenReturn(usuario);
            assertEquals("Ana Paula", service.criar(jwt, pedido()).nome());
        }
    }
}
