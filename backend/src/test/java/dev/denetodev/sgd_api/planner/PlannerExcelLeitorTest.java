package dev.denetodev.sgd_api.planner;

import dev.denetodev.sgd_api.planner.PlannerFonte.PlanoPlanner;
import dev.denetodev.sgd_api.planner.PlannerFonte.TarefaPlanner;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** O arquivo do "Exportar plano para o Excel" (nomes fictícios): as colunas saem pelo título e os campos viram listas. */
class PlannerExcelLeitorTest {

    private final PlannerExcelLeitor leitor = new PlannerExcelLeitor();

    private byte[] planilha(String aba, String[] cabecalho, String[]... linhas) throws IOException {
        try (XSSFWorkbook pasta = new XSSFWorkbook(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            pasta.createSheet("Plano");
            Sheet s = pasta.createSheet(aba);
            escrever(s.createRow(0), cabecalho);
            for (int i = 0; i < linhas.length; i++) {
                escrever(s.createRow(i + 1), linhas[i]);
            }
            pasta.write(saida);
            return saida.toByteArray();
        }
    }

    private void escrever(Row linha, String[] valores) {
        for (int i = 0; i < valores.length; i++) {
            if (valores[i] != null) {
                linha.createCell(i).setCellValue(valores[i]);
            }
        }
    }

    private static final String[] CABECALHO = {"Identificação da tarefa", "Nome da tarefa", "Categoria", "Status", "Atribuído a", "Criado em", "Rótulos"};

    @Test
    void leTarefaComBucketResponsaveisRotulosEDataSemHora() throws IOException {
        byte[] arquivo = planilha("Dados Consolidados", CABECALHO,
                new String[]{"id-1", "BC2611AA - Email teste", "HTML para Aprovação", "Em andamento",
                        "Ana Souza Lima; Bruno Dias Torres", "2026-10-29", "CRIATIVO - Ana;HTML - Bruno; URGENTE"});

        PlanoPlanner plano = leitor.ler(new ByteArrayInputStream(arquivo));

        assertEquals(1, plano.tarefas().size());
        TarefaPlanner t = plano.tarefas().get(0);
        assertEquals("id-1", t.id());
        assertEquals("BC2611AA - Email teste", t.titulo());
        assertEquals("HTML para Aprovação", plano.buckets().get(t.bucketId()));
        assertEquals(List.of("Ana Souza Lima", "Bruno Dias Torres"), t.responsaveis());
        assertEquals(List.of("CRIATIVO - Ana", "HTML - Bruno", "URGENTE"), t.etiquetasAplicadas());
        assertEquals("HTML - Bruno", plano.etiquetas().get("HTML - Bruno"));
        assertEquals(OffsetDateTime.of(2026, 10, 29, 0, 0, 0, 0, ZoneOffset.UTC), t.criadaEm());
        assertEquals(0, t.percentualConcluido());
    }

    @Test
    void colunaForaDeOrdemOuSemAcentoNoTituloAindaFunciona() throws IOException {
        byte[] arquivo = planilha("Dados Consolidados",
                new String[]{"Rotulos", "Nome da tarefa", "Identificacao da tarefa"},
                new String[]{"HTML - Bruno", "BC2611AB - x", "id-2"});

        PlanoPlanner plano = leitor.ler(new ByteArrayInputStream(arquivo));

        assertEquals(List.of("HTML - Bruno"), plano.tarefas().get(0).etiquetasAplicadas());
        assertNull(plano.tarefas().get(0).criadaEm());
    }

    @Test
    void linhaSemIdOuSemNomeEPulada() throws IOException {
        byte[] arquivo = planilha("Dados Consolidados", CABECALHO,
                new String[]{null, "sem id", "HTML para Aprovação"},
                new String[]{"id-3", null, "HTML para Aprovação"},
                new String[]{"id-4", "BC2611AC - ok", "HTML para Aprovação"});

        assertEquals(List.of("id-4"), leitor.ler(new ByteArrayInputStream(arquivo)).tarefas().stream().map(TarefaPlanner::id).toList());
    }

    @Test
    void arquivoSemAbaDeDadosOuSemColunasDeIdEDito() throws IOException {
        byte[] semAba = planilha("Outra", CABECALHO);
        IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> leitor.ler(new ByteArrayInputStream(semAba)));
        assertTrue(e1.getMessage().contains("Dados Consolidados"));

        byte[] semColunas = planilha("Dados Consolidados", new String[]{"A", "B"}, new String[]{"1", "2"});
        IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> leitor.ler(new ByteArrayInputStream(semColunas)));
        assertTrue(e2.getMessage().contains("Identificação da tarefa"));
    }

    @Test
    void arquivoQueNaoEXlsxViraMensagemAmigavel() {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                () -> leitor.ler(new ByteArrayInputStream("isso não é uma planilha".getBytes())));
        assertTrue(e.getMessage().startsWith("Não consegui ler o arquivo"));
    }
}
