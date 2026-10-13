package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.AutoCadastroRequest;
import dev.denetodev.sgd_api.dto.request.PessoaRequest;
import dev.denetodev.sgd_api.dto.response.PessoaResponse;
import dev.denetodev.sgd_api.entity.*;
import dev.denetodev.sgd_api.exception.EstadoInvalidoException;
import dev.denetodev.sgd_api.exception.RecursoNaoEncontradoException;
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

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PessoaServiceChaveCTest {

    private static final String CHAVE = "C1000001";

    private PessoaRepository pessoaRepository;
    private AreaRepository areaRepository;
    private CurrentPessoaResolver resolver;
    private ConfirmacaoEmail confirmacaoEmail;
    private PessoaService service;
    private Area area;
    private UUID areaId;

    @BeforeEach
    void setUp() {
        pessoaRepository = mock(PessoaRepository.class);
        areaRepository = mock(AreaRepository.class);
        resolver = mock(CurrentPessoaResolver.class);
        confirmacaoEmail = mock(ConfirmacaoEmail.class);
        when(confirmacaoEmail.confirmado(any())).thenReturn(true);
        service = new PessoaService(pessoaRepository, areaRepository, mock(CargoRepository.class),
                mock(AuditoriaRepository.class), resolver, confirmacaoEmail, new LimitadorTentativas(100, java.time.Duration.ofHours(1), java.time.Clock.systemUTC()));

        areaId = UUID.randomUUID();
        Diretoria diretoria = mock(Diretoria.class);
        area = mock(Area.class);
        when(area.getId()).thenReturn(areaId);
        when(area.getDiretoria()).thenReturn(diretoria);
        when(areaRepository.findById(areaId)).thenReturn(Optional.of(area));
        when(pessoaRepository.save(any(Pessoa.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pessoaRepository.findByAuthUserId(any())).thenReturn(Optional.empty());
        when(pessoaRepository.findByChaveC(any())).thenReturn(Optional.empty());
    }

    private Jwt jwt() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(UUID.randomUUID().toString());
        when(jwt.getClaimAsString("email")).thenReturn("nova@exemplo.com");
        return jwt;
    }

    @Test
    void autoCadastroGuardaChaveCEmMaiusculaEDevolveInteira() {
        PessoaResponse r = service.autoCadastro(jwt(),
                new AutoCadastroRequest("Ana Caroliny Santos de Sousa", "  c1000001 ", "  Carol ", areaId, null));

        assertEquals("C1000001", r.chaveC());
        assertEquals("Carol", r.nomeExibicao());
        assertEquals("Ana Caroliny Santos de Sousa", r.nome());
    }

    @Test
    void autoCadastroRejeitaChaveCInvalida() {
        assertThrows(IllegalArgumentException.class, () -> service.autoCadastro(jwt(),
                new AutoCadastroRequest("Ana", "X1000001", null, areaId, null)));
    }

    @Test
    void gestorNaoPodeUsarChaveCDeOutraPessoa() {
        Pessoa gestor = new Pessoa("Gestor", area);
        gestor.setPerfil(PerfilPessoa.ADMIN);
        when(resolver.resolver(any())).thenReturn(gestor);
        Pessoa outra = new Pessoa("Outra", area);
        outra.setChaveC(CHAVE);
        when(pessoaRepository.findByChaveC(CHAVE)).thenReturn(Optional.of(outra));

        assertThrows(EstadoInvalidoException.class, () -> service.criar(jwt(),
                new PessoaRequest("Nova", null, CHAVE, areaId, null, null, null, null)));
    }

    @Test
    void chaveVazioNaAtualizacaoMantemOAtual() {
        Pessoa gestor = new Pessoa("Gestor", area);
        gestor.setPerfil(PerfilPessoa.ADMIN);
        when(resolver.resolver(any())).thenReturn(gestor);
        Pessoa alvo = new Pessoa("Alvo", area);
        alvo.setChaveC(CHAVE);
        UUID id = UUID.randomUUID();
        when(pessoaRepository.findById(id)).thenReturn(Optional.of(alvo));

        PessoaResponse r = service.atualizar(jwt(), id, new PessoaRequest("Alvo", null, "", areaId, null, null, null, null));

        assertEquals(CHAVE, alvo.getChaveC());
        assertEquals("C1000001", r.chaveC());
    }

    @Test
    void gestorDefineEDepoisRemoveAReferenciaDeEquipeNaEdicao() {
        Pessoa gestor = new Pessoa("Gestor", area);
        gestor.setPerfil(PerfilPessoa.ADMIN);
        when(resolver.resolver(any())).thenReturn(gestor);
        Pessoa alvo = new Pessoa("Alvo", area);
        UUID id = UUID.randomUUID();
        when(pessoaRepository.findById(id)).thenReturn(Optional.of(alvo));

        service.atualizar(jwt(), id, new PessoaRequest("Alvo", null, null, areaId, null, null, null, areaId));
        assertEquals(area, alvo.getReferenciaArea());

        service.atualizar(jwt(), id, new PessoaRequest("Alvo", null, null, areaId, null, null, null, null));
        assertEquals(null, alvo.getReferenciaArea());
    }

    @Test
    void referenciaDeEquipeComAreaInexistenteDa404() {
        Pessoa gestor = new Pessoa("Gestor", area);
        gestor.setPerfil(PerfilPessoa.ADMIN);
        when(resolver.resolver(any())).thenReturn(gestor);
        Pessoa alvo = new Pessoa("Alvo", area);
        UUID id = UUID.randomUUID();
        when(pessoaRepository.findById(id)).thenReturn(Optional.of(alvo));

        assertThrows(RecursoNaoEncontradoException.class, () -> service.atualizar(jwt(), id,
                new PessoaRequest("Alvo", null, null, areaId, null, null, null, UUID.randomUUID())));
    }
}
