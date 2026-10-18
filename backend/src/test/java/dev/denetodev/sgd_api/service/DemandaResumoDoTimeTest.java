package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.response.ParticipanteResumo;
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
    void semTimeDevolveListaVazia() {
        assertEquals(List.of(), DemandaService.resumirTime(List.of()));
    }
}
