package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.response.ParticipanteResumo;
import dev.denetodev.sgd_api.entity.Area;
import dev.denetodev.sgd_api.entity.Diretoria;
import dev.denetodev.sgd_api.entity.Nucleo;
import dev.denetodev.sgd_api.entity.PapelPessoaDemanda;
import dev.denetodev.sgd_api.entity.Pessoa;
import dev.denetodev.sgd_api.entity.PessoaDemanda;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** O grupo de fotos da lista de demandas: só quem está no time agora, com o Responsável principal primeiro. */
class DemandaResumoDoTimeTest {

    private PessoaDemanda vinculo(String nome, String foto, PapelPessoaDemanda papel, LocalDate saida) {
        Pessoa pessoa = mock(Pessoa.class);
        when(pessoa.getId()).thenReturn(UUID.randomUUID());
        when(pessoa.getNome()).thenReturn(nome);
        when(pessoa.getFotoUrl()).thenReturn(foto);
        PessoaDemanda v = mock(PessoaDemanda.class);
        when(v.getPessoa()).thenReturn(pessoa);
        when(v.getPapel()).thenReturn(papel);
        when(v.getDataSaida()).thenReturn(saida);
        return v;
    }

    @Test
    void ignoraQuemJaSaiuEPoeOResponsavelPrincipalPrimeiroDepoisPorNome() {
        List<PessoaDemanda> time = List.of(
                vinculo("Zeca", null, PapelPessoaDemanda.PARTICIPANTE, null),
                vinculo("Ana", "https://x/ana.jpg", PapelPessoaDemanda.APOIO, null),
                vinculo("Bia", null, PapelPessoaDemanda.RESPONSAVEL_PRINCIPAL, null),
                vinculo("Caio", null, PapelPessoaDemanda.PARTICIPANTE, LocalDate.of(2026, 9, 1)));

        List<ParticipanteResumo> resumo = DemandaService.resumirTime(time);

        assertEquals(List.of("Bia", "Ana", "Zeca"), resumo.stream().map(ParticipanteResumo::nome).toList());
        assertEquals("https://x/ana.jpg", resumo.get(1).fotoUrl());
        assertNull(resumo.get(0).fotoUrl());
    }

    @Test
    void trazAEquipeEONucleoDeCadaPessoaParaAgruparOQuadro() {
        Nucleo nucleo = mock(Nucleo.class);
        UUID nucleoId = UUID.randomUUID();
        when(nucleo.getId()).thenReturn(nucleoId);
        when(nucleo.getNome()).thenReturn("House-CRM");
        Area area = mock(Area.class);
        UUID areaId = UUID.randomUUID();
        when(area.getId()).thenReturn(areaId);
        when(area.getNome()).thenReturn("Design");
        when(area.getNucleo()).thenReturn(nucleo);
        Diretoria diretoria = mock(Diretoria.class);
        when(diretoria.getNome()).thenReturn("COE/CRM");
        when(area.getDiretoria()).thenReturn(diretoria);
        PessoaDemanda comEquipe = vinculo("Ana", null, PapelPessoaDemanda.PARTICIPANTE, null);
        when(comEquipe.getPessoa().getArea()).thenReturn(area);
        PessoaDemanda semEquipeLigada = vinculo("Bia", null, PapelPessoaDemanda.PARTICIPANTE, null);
        Area soEquipe = mock(Area.class);
        when(soEquipe.getNome()).thenReturn("HTML");
        when(semEquipeLigada.getPessoa().getArea()).thenReturn(soEquipe);

        List<ParticipanteResumo> resumo = DemandaService.resumirTime(List.of(comEquipe, semEquipeLigada));

        assertEquals(areaId, resumo.get(0).areaId());
        assertEquals("Design", resumo.get(0).areaNome());
        assertEquals(nucleoId, resumo.get(0).nucleoId());
        assertEquals("House-CRM", resumo.get(0).nucleoNome());
        assertEquals("COE/CRM", resumo.get(0).diretoriaNome());
        assertEquals("HTML", resumo.get(1).areaNome());
        assertNull(resumo.get(1).nucleoId());
    }

    @Test
    void semTimeDevolveListaVazia() {
        assertEquals(List.of(), DemandaService.resumirTime(List.of()));
    }
}
