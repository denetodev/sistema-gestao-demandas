package dev.denetodev.sgd_api.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

/** O que os colegas veem: o nome que a pessoa escolheu, e o nome completo só quando ela não escolheu nenhum. */
class PessoaNomeExibidoTest {

    private Pessoa pessoa(String nome, String exibicao) {
        Pessoa p = new Pessoa(nome, mock(Area.class));
        p.setNomeExibicao(exibicao);
        return p;
    }

    @Test
    void usaOQueAPessoaEscolheu() {
        assertEquals("Carol", pessoa("Ana Caroliny Santos de Sousa", "Carol").getNomeExibido());
    }

    @Test
    void semEscolhaOuComEspacosVoltaAoNomeCompleto() {
        assertEquals("Ana Caroliny Santos de Sousa", pessoa("Ana Caroliny Santos de Sousa", null).getNomeExibido());
        assertEquals("Ana Caroliny Santos de Sousa", pessoa("Ana Caroliny Santos de Sousa", "   ").getNomeExibido());
    }

    @Test
    void tiraOsEspacosDasPontas() {
        assertEquals("Carol", pessoa("Ana Caroliny Santos de Sousa", "  Carol ").getNomeExibido());
    }
}
