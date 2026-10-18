package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.CargoRequest;
import dev.denetodev.sgd_api.entity.Cargo;
import dev.denetodev.sgd_api.entity.StatusPessoa;
import dev.denetodev.sgd_api.exception.EstadoInvalidoException;
import dev.denetodev.sgd_api.repository.CargoRepository;
import dev.denetodev.sgd_api.repository.PessoaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CargoServiceTest {

    private CargoRepository cargoRepository;
    private PessoaRepository pessoaRepository;
    private CargoService service;

    @BeforeEach
    void setUp() {
        cargoRepository = mock(CargoRepository.class);
        pessoaRepository = mock(PessoaRepository.class);
        service = new CargoService(cargoRepository, pessoaRepository);
        when(cargoRepository.findByNomeIgnoreCase(any())).thenReturn(Optional.empty());
        when(cargoRepository.save(any(Cargo.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void criaCargoComNomeAparadoEDescricaoVaziaVirandoNula() {
        var resposta = service.criar(new CargoRequest("  Analista de Mídia  ", "   "));

        assertEquals("Analista de Mídia", resposta.nome());
        assertNull(resposta.descricao());
        assertTrue(resposta.ativo());
    }

    @Test
    void recusaNomeDeCargoAtivoQueJaExiste() {
        when(cargoRepository.findByNomeIgnoreCase("designer")).thenReturn(Optional.of(new Cargo("Designer")));

        assertThrows(EstadoInvalidoException.class, () -> service.criar(new CargoRequest("designer", null)));
        verify(cargoRepository, never()).save(any());
    }

    @Test
    void criarComNomeDeCargoDesativadoOReativa() {
        Cargo antigo = new Cargo("Editor");
        antigo.setAtivo(false);
        when(cargoRepository.findByNomeIgnoreCase("Editor")).thenReturn(Optional.of(antigo));

        var resposta = service.criar(new CargoRequest("Editor", "Edição de vídeo"));

        assertTrue(antigo.isAtivo());
        assertEquals("Edição de vídeo", resposta.descricao());
        verify(cargoRepository, never()).save(any());
    }

    @Test
    void naoRenomeiaParaNomeDeOutroCargo() {
        UUID id = UUID.randomUUID();
        when(cargoRepository.findById(id)).thenReturn(Optional.of(new Cargo("Redator")));
        when(cargoRepository.existsByNomeIgnoreCaseAndIdNot("Designer", id)).thenReturn(true);

        assertThrows(EstadoInvalidoException.class, () -> service.atualizar(id, new CargoRequest("Designer", null)));
    }

    @Test
    void naoDesativaCargoComPessoasAtivas() {
        UUID id = UUID.randomUUID();
        Cargo cargo = new Cargo("Operador");
        when(cargoRepository.findById(id)).thenReturn(Optional.of(cargo));
        when(pessoaRepository.existsByCargoIdAndStatus(id, StatusPessoa.ATIVO)).thenReturn(true);

        assertThrows(EstadoInvalidoException.class, () -> service.desativar(id));
        assertTrue(cargo.isAtivo());
    }

    @Test
    void desativaCargoSemPessoasAtivas() {
        UUID id = UUID.randomUUID();
        Cargo cargo = new Cargo("Operador");
        when(cargoRepository.findById(id)).thenReturn(Optional.of(cargo));

        service.desativar(id);

        assertFalse(cargo.isAtivo());
    }
}
