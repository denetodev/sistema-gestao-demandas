package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.response.RelatorioResponse;
import dev.denetodev.sgd_api.dto.response.RelatorioResponse.ItemAtividade;
import dev.denetodev.sgd_api.dto.response.RelatorioResponse.ItemPeca;
import dev.denetodev.sgd_api.entity.StatusRelatorio;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class RelatorioDocxServiceTest {

    private final RelatorioDocxService service = new RelatorioDocxService();

    private ItemAtividade atividade(String data, String demanda, String tipo, String descricao, boolean incluida) {
        return new ItemAtividade(UUID.randomUUID(), LocalDate.parse(data), demanda == null ? null : UUID.randomUUID(),
                demanda, tipo, descricao, incluida);
    }

    private RelatorioResponse relatorio(StatusRelatorio status, OffsetDateTime aprovadoEm) {
        return new RelatorioResponse(
                UUID.randomUUID(), "Ana Souza", "COE/CRM", "HTML", "Especialista HTML", "2026-03",
                status, aprovadoEm, "Mês com férias parciais.",
                List.of(
                        atividade("2026-03-04", "Campanha Plano Safra", "Briefing", "Alinhamento inicial", true),
                        atividade("2026-03-18", "Campanha Plano Safra", "Produção", null, true),
                        atividade("2026-03-20", null, "Reunião", "Alinhamento interno", true),
                        atividade("2026-03-25", "Campanha Plano Safra", "Revisão", "fora do relatório", false)),
                List.of(new ItemPeca(UUID.randomUUID(), "Campanha Plano Safra", "E-mail v1", "Ajustes HTML",
                        2, new BigDecimal("6000.00"), new BigDecimal("12000.00"))),
                new BigDecimal("12000.00"), false, false);
    }

    private String texto(byte[] docx) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docx))) {
            String paragrafos = doc.getParagraphs().stream().map(p -> p.getText()).collect(Collectors.joining("\n"));
            String tabelas = doc.getTables().stream()
                    .flatMap(t -> t.getRows().stream())
                    .flatMap(r -> r.getTableCells().stream())
                    .map(c -> c.getText())
                    .collect(Collectors.joining("\n"));
            return paragrafos + "\n" + tabelas;
        }
    }

    @Test
    void geraDocxValidoComOConteudoDoRelatorioAprovado() throws Exception {
        byte[] bytes = service.gerar(relatorio(StatusRelatorio.APROVADO, OffsetDateTime.parse("2026-04-02T10:00:00Z")));

        // DOCX é um zip: começa com "PK"
        assertEquals('P', bytes[0]);
        assertEquals('K', bytes[1]);

        String t = texto(bytes);
        assertTrue(t.contains("RELATÓRIO MENSAL DE ATIVIDADES"));
        assertTrue(t.contains("Março de 2026"));
        assertTrue(t.contains("Ana Souza"));
        assertTrue(t.contains("Campanha Plano Safra"));
        assertTrue(t.contains("Atividades avulsas (sem demanda)"));
        assertTrue(t.contains("Alinhamento inicial"));
        assertTrue(t.contains("Total: 3 atividade(s)"));
        assertTrue(t.contains("Mês com férias parciais."));
        assertTrue(t.contains("aprovado em 02/04/2026"));
        assertFalse(t.contains("Rascunho"));
    }

    @Test
    void atividadeForaDaSelecaoNaoEntraEMarcaRascunhoQuandoAberto() throws Exception {
        String t = texto(service.gerar(relatorio(StatusRelatorio.ABERTO, null)));

        assertFalse(t.contains("fora do relatório"));
        assertTrue(t.contains("Rascunho"));
        assertFalse(t.contains("aprovado em"));
    }

    @Test
    void semAtividadesNemPecasGeraAvisos() throws Exception {
        RelatorioResponse vazio = new RelatorioResponse(
                UUID.randomUUID(), "Bia", null, null, null, "2026-10", StatusRelatorio.ABERTO, null, null,
                List.of(), List.of(), BigDecimal.ZERO, false, false);

        String t = texto(service.gerar(vazio));

        assertTrue(t.contains("Nenhuma atividade selecionada."));
        assertTrue(t.contains("Nenhuma peça registrada no mês."));
        assertTrue(t.contains("Sem observações."));
    }

    @Test
    void tabelaDePecasTemOValorEmReais() throws Exception {
        byte[] bytes = service.gerar(relatorio(StatusRelatorio.APROVADO, OffsetDateTime.now()));
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(bytes))) {
            XWPFTable pecas = doc.getTables().get(doc.getTables().size() - 1);
            String valor = pecas.getRow(1).getCell(4).getText();
            assertTrue(valor.contains("12.000,00"), valor);
        }
    }
}
