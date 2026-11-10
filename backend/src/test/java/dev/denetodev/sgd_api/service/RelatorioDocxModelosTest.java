package dev.denetodev.sgd_api.service;

import dev.denetodev.sgd_api.dto.response.RelatorioResponse;
import dev.denetodev.sgd_api.dto.response.RelatorioResponse.ItemAtividade;
import dev.denetodev.sgd_api.dto.response.RelatorioResponse.ItemDemanda;
import dev.denetodev.sgd_api.dto.response.RelatorioResponse.ItemEvidencia;
import dev.denetodev.sgd_api.dto.response.RelatorioResponse.ResumoPeca;
import dev.denetodev.sgd_api.entity.ModeloRelatorio;
import dev.denetodev.sgd_api.entity.StatusRelatorio;
import dev.denetodev.sgd_api.entity.TipoEvidencia;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** O DOCX segue o modelo da função: Designer lista demanda a demanda, HTML traz o resumo e a tabela por semana. */
class RelatorioDocxModelosTest {

    private final RelatorioDocxService service = new RelatorioDocxService();

    private RelatorioResponse relatorio(ModeloRelatorio modelo) {
        UUID atividadeId = UUID.randomUUID();
        ItemAtividade atividade = new ItemAtividade(atividadeId, LocalDate.of(2026, 10, 2), UUID.randomUUID(),
                "Boletim Exclusivo", "Criação", null, true);
        ItemEvidencia link = new ItemEvidencia(UUID.randomUUID(), atividadeId, null, TipoEvidencia.LINK, null,
                "https://sharepoint/arte.png", false, null);
        ItemDemanda demanda = new ItemDemanda(UUID.randomUUID(), "BC2610AC", "Boletim Exclusivo", "Criar o e-mail marketing",
                "https://planner/x", "Flavia dos Santos", "COE/CRM", null, LocalDate.of(2026, 10, 2), List.of(atividadeId),
                4, new BigDecimal("40"), List.of(new ResumoPeca("E-mail", 2), new ResumoPeca("Banner", 2)),
                List.of("Criação"), List.of(link));
        return new RelatorioResponse(UUID.randomUUID(), "Thaís Caldas", "COE/CRM", "Design", "Designer III - Sênior",
                "2026-10", StatusRelatorio.APROVADO, OffsetDateTime.parse("2026-11-03T10:00:00Z"), null,
                List.of(atividade), List.of(), BigDecimal.ZERO, modelo, List.of(demanda), false, false);
    }

    private String texto(byte[] docx) throws Exception {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(docx))) {
            String paragrafos = doc.getParagraphs().stream().map(p -> p.getText()).collect(Collectors.joining("\n"));
            String tabelas = doc.getTables().stream().flatMap(t -> t.getRows().stream())
                    .flatMap(r -> r.getTableCells().stream()).map(c -> c.getText()).collect(Collectors.joining("\n"));
            return paragrafos + "\n" + tabelas;
        }
    }

    @Test
    void designerListaCadaDemandaComLinkPecasGestorEEvidencia() throws Exception {
        String t = texto(service.gerar(relatorio(ModeloRelatorio.DESIGNER)));

        assertTrue(t.contains("Nome do Colaborador: Thaís Caldas"));
        assertTrue(t.contains("Cargo (conforme CTPS): Designer III - Sênior"));
        assertTrue(t.contains("SEMANA 1 – de 01/10 a 04/10"));
        assertTrue(t.contains("Demanda 01 - BC2610AC - Boletim Exclusivo"));
        assertTrue(t.contains("Link: https://planner/x"));
        assertTrue(t.contains("Quantidade de peças feitas: 04 (2 E-mail e 2 Banner)"));
        assertTrue(t.contains("Gestor demandante: Flavia dos Santos"));
        assertTrue(t.contains("Link da arte: https://sharepoint/arte.png"));
        assertTrue(t.contains("DECLARAÇÃO"));
        assertFalse(t.contains("Resumo Executivo"));
    }

    @Test
    void htmlTrazResumoExecutivoETabelaDaSemana() throws Exception {
        String t = texto(service.gerar(relatorio(ModeloRelatorio.HTML)));

        assertTrue(t.contains("Resumo Executivo"));
        assertTrue(t.contains("TOTAL"));
        assertTrue(t.contains("Código BC"));
        assertTrue(t.contains("BC2610AC"));
        assertTrue(t.contains("Natureza"));
    }

    @Test
    void padraoSegueATrilhaDaNorma() throws Exception {
        String t = texto(service.gerar(relatorio(ModeloRelatorio.PADRAO)));

        assertTrue(t.contains("Descrição das Atividades Executadas:"));
        assertTrue(t.contains("Demandas do Banco / Ordens de Serviço:"));
        assertTrue(t.contains("Evidências (obrigatório):"));
        assertTrue(t.contains("Origem: Flavia dos Santos"));
    }
}
