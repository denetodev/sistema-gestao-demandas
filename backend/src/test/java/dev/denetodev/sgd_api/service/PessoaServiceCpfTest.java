package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.AutoCadastroRequest;
import dev.denetodev.sgd_api.dto.request.PessoaRequest;
import dev.denetodev.sgd_api.dto.response.PessoaResponse;
import dev.denetodev.sgd_api.entity.*;
import dev.denetodev.sgd_api.exception.EstadoInvalidoException;
import dev.denetodev.sgd_api.repository.AreaRepository;
import dev.denetodev.sgd_api.repository.AuditoriaRepository;
import dev.denetodev.sgd_api.repository.CargoRepository;
import dev.denetodev.sgd_api.repository.PessoaRepository;
import dev.denetodev.sgd_api.security.CurrentPessoaResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PessoaServiceCpfTest {

    private static final String CPF = "52998224725";

    private PessoaRepository pessoaRepository;
    private AreaRepository areaRepository;
    private CurrentPessoaResolver resolver;
    private PessoaService service;
    private Area area;
    private UUID areaId;

    @BeforeEach
    void setUp() {
        pessoaRepository = mock(PessoaRepository.class);
        areaRepository = mock(AreaRepository.class);
        resolver = mock(CurrentPessoaResolver.class);
        service = new PessoaService(pessoaRepository, areaRepository, mock(CargoRepository.class),
                mock(AuditoriaRepository.class), resolver);

        areaId = UUID.randomUUID();
        Diretoria diretoria = mock(Diretoria.class);
        area = mock(Area.class);
        when(area.getId()).thenReturn(areaId);
        when(area.getDiretoria()).thenReturn(diretoria);
        when(areaRepository.findById(areaId)).thenReturn(Optional.of(area));
        when(pessoaRepository.save(any(Pessoa.class))).thenAnswer(inv -> inv.getArgument(0));
        when(pessoaRepository.findByAuthUserId(any())).thenReturn(Optional.empty());
        when(pessoaRepository.findByCpf(any())).thenReturn(Optional.empty());
    }

    private Jwt jwt() {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(UUID.randomUUID().toString());
        when(jwt.getClaimAsString("email")).thenReturn("nova@exemplo.com");
        return jwt;
    }

    @Test
    void autoCadastroGuardaCpfSoComDigitosEDevolveSoAMascara() {
        PessoaResponse r = service.autoCadastro(jwt(),
                new AutoCadastroRequest("Ana Caroliny Santos de Sousa", "529.982.247-25", "  Carol ", areaId, null));

        assertEquals("***.982.247-**", r.cpfMascarado());
        assertEquals("Carol", r.nomeExibicao());
        assertEquals("Ana Caroliny Santos de Sousa", r.nome());
    }

    @Test
    void autoCadastroRejeitaCpfInvalido() {
        assertThrows(IllegalArgumentException.class, () -> service.autoCadastro(jwt(),
                new AutoCadastroRequest("Ana", "111.111.111-11", null, areaId, null)));
    }

    @Test
    void autoCadastroComCpfJaCadastradoPedeVinculoPeloGestor() {
        Pessoa existente = new Pessoa("Ana importada", area);
        existente.setCpf(CPF);
        when(pessoaRepository.findByCpf(CPF)).thenReturn(Optional.of(existente));

        EstadoInvalidoException e = assertThrows(EstadoInvalidoException.class, () -> service.autoCadastro(jwt(),
                new AutoCadastroRequest("Ana", CPF, null, areaId, null)));
        assertTrue(e.getMessage().contains("Gestor"));
    }

    @Test
    void gestorNaoPodeUsarCpfDeOutraPessoa() {
        Pessoa gestor = new Pessoa("Gestor", area);
        gestor.setPerfil(PerfilPessoa.ADMIN);
        when(resolver.resolver(any())).thenReturn(gestor);
        Pessoa outra = new Pessoa("Outra", area);
        outra.setCpf(CPF);
        when(pessoaRepository.findByCpf(CPF)).thenReturn(Optional.of(outra));

        assertThrows(EstadoInvalidoException.class, () -> service.criar(jwt(),
                new PessoaRequest("Nova", null, CPF, areaId, null, null, null)));
    }

    @Test
    void cpfVazioNaAtualizacaoMantemOAtual() {
        Pessoa gestor = new Pessoa("Gestor", area);
        gestor.setPerfil(PerfilPessoa.ADMIN);
        when(resolver.resolver(any())).thenReturn(gestor);
        Pessoa alvo = new Pessoa("Alvo", area);
        alvo.setCpf(CPF);
        UUID id = UUID.randomUUID();
        when(pessoaRepository.findById(id)).thenReturn(Optional.of(alvo));

        PessoaResponse r = service.atualizar(jwt(), id, new PessoaRequest("Alvo", null, "", areaId, null, null, null));

        assertEquals(CPF, alvo.getCpf());
        assertEquals("***.982.247-**", r.cpfMascarado());
    }
}
