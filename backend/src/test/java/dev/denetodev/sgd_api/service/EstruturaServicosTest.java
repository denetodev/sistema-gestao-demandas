package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.request.AreaRequest;
import dev.denetodev.sgd_api.dto.request.NucleoRequest;
import dev.denetodev.sgd_api.entity.Area;
import dev.denetodev.sgd_api.entity.Diretoria;
import dev.denetodev.sgd_api.entity.Nucleo;
import dev.denetodev.sgd_api.entity.StatusPessoa;
import dev.denetodev.sgd_api.exception.EstadoInvalidoException;
import dev.denetodev.sgd_api.repository.AreaRepository;
import dev.denetodev.sgd_api.repository.DiretoriaRepository;
import dev.denetodev.sgd_api.repository.NucleoRepository;
import dev.denetodev.sgd_api.repository.PessoaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Regras de núcleos e equipes: nome único na diretoria e só desativa o que está vazio. */
class EstruturaServicosTest {

    private AreaRepository areaRepository;
    private NucleoRepository nucleoRepository;
    private DiretoriaRepository diretoriaRepository;
    private PessoaRepository pessoaRepository;
    private NucleoService nucleoService;
    private AreaService areaService;

    private final UUID diretoriaId = UUID.randomUUID();
    private Diretoria diretoria;

    @BeforeEach
    void setUp() {
        areaRepository = mock(AreaRepository.class);
        nucleoRepository = mock(NucleoRepository.class);
        diretoriaRepository = mock(DiretoriaRepository.class);
        pessoaRepository = mock(PessoaRepository.class);
        nucleoService = new NucleoService(nucleoRepository, diretoriaRepository, areaRepository);
        areaService = new AreaService(areaRepository, diretoriaRepository, nucleoRepository, pessoaRepository);

        diretoria = mock(Diretoria.class);
        when(diretoria.getId()).thenReturn(diretoriaId);
        when(diretoria.getNome()).thenReturn("UGR");
        when(diretoriaRepository.findById(diretoriaId)).thenReturn(Optional.of(diretoria));
    }

    @Test
    void naoCriaNucleoComNomeRepetidoNaMesmaDiretoria() {
        when(nucleoRepository.existsByDiretoriaIdAndNomeIgnoreCaseAndAtivoTrue(diretoriaId, "Multimídia")).thenReturn(true);

        assertThrows(EstadoInvalidoException.class,
                () -> nucleoService.criar(new NucleoRequest("  Multimídia ", diretoriaId)));
        verify(nucleoRepository, never()).save(any());
    }

    @Test
    void criaNucleoGuardandoONomeAparado() {
        when(nucleoRepository.save(any(Nucleo.class))).thenAnswer(inv -> inv.getArgument(0));

        var resposta = nucleoService.criar(new NucleoRequest("  Redes  ", diretoriaId));

        assertEquals("Redes", resposta.nome());
    }

    @Test
    void naoDesativaNucleoQueAindaTemEquipes() {
        UUID id = UUID.randomUUID();
        when(areaRepository.existsByNucleoIdAndAtivoTrue(id)).thenReturn(true);

        assertThrows(EstadoInvalidoException.class, () -> nucleoService.desativar(id));
        verify(nucleoRepository, never()).findById(any());
    }

    @Test
    void desativaNucleoVazio() {
        UUID id = UUID.randomUUID();
        Nucleo nucleo = new Nucleo(diretoria, "Redes");
        when(nucleoRepository.findById(id)).thenReturn(Optional.of(nucleo));

        nucleoService.desativar(id);

        assertFalse(nucleo.isAtivo());
    }

    @Test
    void naoMoveNucleoComEquipesParaOutraDiretoria() {
        UUID id = UUID.randomUUID();
        UUID outra = UUID.randomUUID();
        Nucleo nucleo = new Nucleo(diretoria, "Multimídia");
        when(nucleoRepository.findById(id)).thenReturn(Optional.of(nucleo));
        when(areaRepository.existsByNucleoIdAndAtivoTrue(id)).thenReturn(true);

        assertThrows(EstadoInvalidoException.class, () -> nucleoService.atualizar(id, new NucleoRequest("Multimídia", outra)));
    }

    @Test
    void naoCriaEquipeComNomeRepetidoNaMesmaDiretoria() {
        when(areaRepository.existsByDiretoriaIdAndNomeIgnoreCaseAndAtivoTrue(diretoriaId, "design")).thenReturn(true);

        assertThrows(EstadoInvalidoException.class,
                () -> areaService.criar(new AreaRequest("design", diretoriaId, null)));
        verify(areaRepository, never()).save(any());
    }

    @Test
    void naoDesativaEquipeComPessoasAtivas() {
        UUID id = UUID.randomUUID();
        when(pessoaRepository.existsByAreaIdAndStatus(id, StatusPessoa.ATIVO)).thenReturn(true);

        assertThrows(EstadoInvalidoException.class, () -> areaService.desativar(id));
        verify(areaRepository, never()).findById(any());
    }

    @Test
    void desativaEquipeSemPessoasAtivas() {
        UUID id = UUID.randomUUID();
        Area area = new Area(diretoria, "Motion");
        when(areaRepository.findById(id)).thenReturn(Optional.of(area));

        areaService.desativar(id);

        assertFalse(area.isAtivo());
    }
}
